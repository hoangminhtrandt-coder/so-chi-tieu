package com.example.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelReportExporter {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    fun exportToExcel(
        context: Context,
        periodTitle: String,
        transactions: List<TransactionEntity>
    ): File? {
        val fileName = "SoChiTieu_${System.currentTimeMillis()}.csv"
        val outputFile = File(context.cacheDir, fileName)

        try {
            FileOutputStream(outputFile).use { fos ->
                // Write UTF-8 BOM so Excel opens Vietnamese diacritics correctly
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Title Block
                    writer.write("\"BÁO CÁO THU CHI CÁ NHÂN\"\n")
                    writer.write("\"Kỳ báo cáo: \",\"$periodTitle\"\n")
                    val exportedAt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                    writer.write("\"Ngày xuất: \",\"$exportedAt\"\n\n")

                    // Summary Block
                    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    val net = totalIncome - totalExpense

                    writer.write("\"TỔNG HỢP\"\n")
                    writer.write("\"Tổng Thu Nhập: \",\"${totalIncome.toLong()}\",\"${currencyFormat.format(totalIncome)}\"\n")
                    writer.write("\"Tổng Chi Tiêu: \",\"${totalExpense.toLong()}\",\"${currencyFormat.format(totalExpense)}\"\n")
                    writer.write("\"Số Dư Ròng: \",\"${net.toLong()}\",\"${currencyFormat.format(net)}\"\n\n")

                    // Table Header
                    writer.write("\"STT\",\"Ngày\",\"Giờ\",\"Loại\",\"Danh mục\",\"Số tiền (VNĐ)\",\"Tài khoản / Ví\",\"Ghi chú\"\n")

                    // Rows
                    transactions.forEachIndexed { index, item ->
                        val dateStr = dateFormat.format(Date(item.date))
                        val timeStr = timeFormat.format(Date(item.date))
                        val typeStr = if (item.type == TransactionType.INCOME) "Thu nhập" else "Chi tiêu"
                        val noteEscaped = item.note.replace("\"", "\"\"")
                        val walletEscaped = item.wallet.replace("\"", "\"\"")

                        writer.write(
                            "\"${index + 1}\",\"$dateStr\",\"$timeStr\",\"$typeStr\",\"${item.categoryName}\",\"${item.amount.toLong()}\",\"$walletEscaped\",\"$noteEscaped\"\n"
                        )
                    }
                    writer.flush()
                }
            }
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareExcel(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Chia sẻ file Excel / CSV")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
