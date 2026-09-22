package com.example.sync

import android.content.Context
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String, val timestamp: Long) : SyncState()
    data class Error(val message: String) : SyncState()
}

class CloudSyncManager(
    private val context: Context,
    private val repository: FinanceRepository
) {
    private val prefs = context.getSharedPreferences("finance_cloud_sync_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SYNC_ID = "cloud_sync_id"
        private const val KEY_LAST_SYNC_TIME = "last_sync_timestamp"
    }

    fun getSyncId(): String {
        var id = prefs.getString(KEY_SYNC_ID, null)
        if (id.isNullOrBlank()) {
            // Generate a readable 8-character pairing sync ID, e.g., VN-7382-EX
            val randomDigits = (1000..9999).random()
            id = "VN-$randomDigits-${UUID.randomUUID().toString().take(4).uppercase()}"
            prefs.edit().putString(KEY_SYNC_ID, id).apply()
        }
        return id
    }

    fun setSyncId(newId: String) {
        prefs.edit().putString(KEY_SYNC_ID, newId.trim().uppercase()).apply()
    }

    fun getLastSyncTime(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    private fun updateLastSyncTime() {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis()).apply()
    }

    suspend fun syncWithCloud(): SyncState = withContext(Dispatchers.IO) {
        val syncId = getSyncId()
        try {
            val firestore = FirebaseFirestore.getInstance()
            val syncDoc = firestore.collection("device_sync").document(syncId)

            // 1. Upload local data to Firestore
            val localTransactions = repository.getAllTransactionsSnapshot()
            val transactionsCollection = syncDoc.collection("transactions")

            for (tx in localTransactions) {
                val data = hashMapOf(
                    "uuid" to tx.uuid,
                    "type" to tx.type.name,
                    "amount" to tx.amount,
                    "categoryId" to tx.categoryId,
                    "categoryName" to tx.categoryName,
                    "categoryIcon" to tx.categoryIcon,
                    "categoryColor" to tx.categoryColor,
                    "date" to tx.date,
                    "note" to tx.note,
                    "wallet" to tx.wallet,
                    "updatedAt" to tx.updatedAt,
                    "isDeleted" to tx.isDeleted
                )
                transactionsCollection.document(tx.uuid).set(data, SetOptions.merge()).await()
            }

            // 2. Fetch remote data from Firestore
            val remoteSnapshot = transactionsCollection.get().await()
            val remoteTransactions = mutableListOf<TransactionEntity>()

            for (doc in remoteSnapshot.documents) {
                val uuid = doc.getString("uuid") ?: doc.id
                val typeStr = doc.getString("type") ?: "EXPENSE"
                val amount = doc.getDouble("amount") ?: 0.0
                val categoryId = doc.getString("categoryId") ?: "other_expense"
                val categoryName = doc.getString("categoryName") ?: "Khác"
                val categoryIcon = doc.getString("categoryIcon") ?: "MoreHoriz"
                val categoryColor = doc.getLong("categoryColor") ?: 0xFF64748B
                val date = doc.getLong("date") ?: System.currentTimeMillis()
                val note = doc.getString("note") ?: ""
                val wallet = doc.getString("wallet") ?: "Tiền mặt"
                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                val isDeleted = doc.getBoolean("isDeleted") ?: false

                remoteTransactions.add(
                    TransactionEntity(
                        uuid = uuid,
                        type = TransactionType.valueOf(typeStr),
                        amount = amount,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        categoryIcon = categoryIcon,
                        categoryColor = categoryColor,
                        date = date,
                        note = note,
                        wallet = wallet,
                        updatedAt = updatedAt,
                        isDeleted = isDeleted
                    )
                )
            }

            // 3. Merge into local DB
            if (remoteTransactions.isNotEmpty()) {
                repository.mergeRemoteTransactions(remoteTransactions)
            }

            updateLastSyncTime()
            SyncState.Success("Đồng bộ thành công! Đã cập nhật ${remoteTransactions.size} mục từ máy chủ.", System.currentTimeMillis())
        } catch (e: Exception) {
            // If offline or Firebase is not reachable, record backup locally
            SyncState.Error(
                if (e.message?.contains("FirebaseApp") == true || e.message?.contains("default FirebaseApp") == true) {
                    "Chế độ ngoại tuyến: Vui lòng kết nối Internet hoặc dùng tính năng Sao lưu JSON."
                } else {
                    "Lỗi kết nối đồng bộ: ${e.localizedMessage ?: "Vui lòng kiểm tra mạng"}"
                }
            )
        }
    }

    // Offline JSON Backup & Restore for multi-device sync
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("syncId", getSyncId())
        root.put("exportedAt", System.currentTimeMillis())

        val txArray = JSONArray()
        val list = repository.getAllTransactionsSnapshot()
        for (tx in list) {
            val obj = JSONObject().apply {
                put("uuid", tx.uuid)
                put("type", tx.type.name)
                put("amount", tx.amount)
                put("categoryId", tx.categoryId)
                put("categoryName", tx.categoryName)
                put("categoryIcon", tx.categoryIcon)
                put("categoryColor", tx.categoryColor)
                put("date", tx.date)
                put("note", tx.note)
                put("wallet", tx.wallet)
                put("updatedAt", tx.updatedAt)
                put("isDeleted", tx.isDeleted)
            }
            txArray.put(obj)
        }
        root.put("transactions", txArray)
        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val txArray = root.optJSONArray("transactions") ?: JSONArray()
            val list = mutableListOf<TransactionEntity>()

            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                list.add(
                    TransactionEntity(
                        uuid = obj.getString("uuid"),
                        type = TransactionType.valueOf(obj.getString("type")),
                        amount = obj.getDouble("amount"),
                        categoryId = obj.getString("categoryId"),
                        categoryName = obj.getString("categoryName"),
                        categoryIcon = obj.getString("categoryIcon"),
                        categoryColor = obj.getLong("categoryColor"),
                        date = obj.getLong("date"),
                        note = obj.optString("note", ""),
                        wallet = obj.optString("wallet", "Tiền mặt"),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isDeleted = obj.optBoolean("isDeleted", false)
                    )
                )
            }

            if (list.isNotEmpty()) {
                repository.mergeRemoteTransactions(list)
            }
            updateLastSyncTime()
            Result.success(list.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
