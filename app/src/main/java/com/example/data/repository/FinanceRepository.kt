package com.example.data.repository

import com.example.data.db.BudgetDao
import com.example.data.db.TransactionDao
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(startTime, endTime)
    }

    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<CategoryBudgetEntity>> {
        return budgetDao.getBudgetsForMonth(month, year)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insert(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.update(transaction.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.softDeleteById(id)
    }

    suspend fun setBudget(categoryId: String, categoryName: String, limit: Double, month: Int, year: Int) {
        val existing = budgetDao.getBudgetForCategory(categoryId, month, year)
        if (existing != null) {
            budgetDao.update(
                existing.copy(
                    monthlyLimit = limit,
                    categoryName = categoryName,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            budgetDao.insert(
                CategoryBudgetEntity(
                    categoryId = categoryId,
                    categoryName = categoryName,
                    monthlyLimit = limit,
                    month = month,
                    year = year
                )
            )
        }
    }

    suspend fun deleteBudget(id: Long) {
        budgetDao.deleteById(id)
    }

    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity> {
        return transactionDao.getAllTransactionsSnapshot()
    }

    suspend fun getBudgetsSnapshot(month: Int, year: Int): List<CategoryBudgetEntity> {
        return budgetDao.getBudgetsForMonthSync(month, year)
    }

    suspend fun mergeRemoteTransactions(remoteList: List<TransactionEntity>) {
        for (remote in remoteList) {
            val local = transactionDao.getByUuid(remote.uuid)
            if (local == null) {
                transactionDao.insert(remote)
            } else if (remote.updatedAt > local.updatedAt) {
                transactionDao.update(remote.copy(id = local.id))
            }
        }
    }

    suspend fun mergeRemoteBudgets(remoteBudgets: List<CategoryBudgetEntity>) {
        for (remote in remoteBudgets) {
            val local = budgetDao.getBudgetForCategory(remote.categoryId, remote.month, remote.year)
            if (local == null) {
                budgetDao.insert(remote)
            } else if (remote.updatedAt > local.updatedAt) {
                budgetDao.update(remote.copy(id = local.id))
            }
        }
    }
}
