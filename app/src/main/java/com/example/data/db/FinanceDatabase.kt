package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.DefaultCategories
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [TransactionEntity::class, CategoryBudgetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.transactionDao(), database.budgetDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(transactionDao: TransactionDao, budgetDao: BudgetDao) {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH) + 1 // 1-12

            // Initial Category Budgets
            val initialBudgets = listOf(
                CategoryBudgetEntity(
                    categoryId = "food",
                    categoryName = "Ăn uống",
                    monthlyLimit = 4500000.0,
                    month = currentMonth,
                    year = currentYear
                ),
                CategoryBudgetEntity(
                    categoryId = "shopping",
                    categoryName = "Mua sắm",
                    monthlyLimit = 2000000.0,
                    month = currentMonth,
                    year = currentYear
                ),
                CategoryBudgetEntity(
                    categoryId = "bills",
                    categoryName = "Hóa đơn & Tiện ích",
                    monthlyLimit = 1800000.0,
                    month = currentMonth,
                    year = currentYear
                ),
                CategoryBudgetEntity(
                    categoryId = "transport",
                    categoryName = "Di chuyển",
                    monthlyLimit = 1200000.0,
                    month = currentMonth,
                    year = currentYear
                ),
                CategoryBudgetEntity(
                    categoryId = "entertainment",
                    categoryName = "Giải trí",
                    monthlyLimit = 1000000.0,
                    month = currentMonth,
                    year = currentYear
                )
            )
            budgetDao.insertAll(initialBudgets)

            // Initial Transactions
            val cal = Calendar.getInstance()
            val now = cal.timeInMillis

            // Sample Income
            cal.set(Calendar.DAY_OF_MONTH, 5)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            val salaryDate = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, 15)
            val bonusDate = cal.timeInMillis

            // Today's transactions
            val todayCal = Calendar.getInstance()
            val todayMorning = todayCal.apply { set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 30) }.timeInMillis
            val todayLunch = todayCal.apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 15) }.timeInMillis

            // Yesterday's transactions
            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1); set(Calendar.HOUR_OF_DAY, 19) }
            val yesterdayDinner = yesterdayCal.timeInMillis

            // Earlier this month
            val billCal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 10); set(Calendar.HOUR_OF_DAY, 14) }
            val billDate = billCal.timeInMillis

            val transList = listOf(
                TransactionEntity(
                    type = TransactionType.INCOME,
                    amount = 25000000.0,
                    categoryId = "salary",
                    categoryName = "Tiền lương",
                    categoryIcon = "Payments",
                    categoryColor = 0xFF10B981,
                    date = salaryDate,
                    note = "Lương hàng tháng công ty",
                    wallet = "Tài khoản ngân hàng"
                ),
                TransactionEntity(
                    type = TransactionType.INCOME,
                    amount = 3000000.0,
                    categoryId = "bonus",
                    categoryName = "Thưởng & Phụ cấp",
                    categoryIcon = "CardGiftcard",
                    categoryColor = 0xFF3B82F6,
                    date = bonusDate,
                    note = "Thưởng dự án hoàn thành tốt",
                    wallet = "Tài khoản ngân hàng"
                ),
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 45000.0,
                    categoryId = "food",
                    categoryName = "Ăn uống",
                    categoryIcon = "Restaurant",
                    categoryColor = 0xFFEF4444,
                    date = todayMorning,
                    note = "Cà phê & Bánh mì sáng",
                    wallet = "Tiền mặt"
                ),
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 75000.0,
                    categoryId = "food",
                    categoryName = "Ăn uống",
                    categoryIcon = "Restaurant",
                    categoryColor = 0xFFEF4444,
                    date = todayLunch,
                    note = "Cơm trưa văn phòng",
                    wallet = "Ví điện tử (MoMo/ZaloPay)"
                ),
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 250000.0,
                    categoryId = "shopping",
                    categoryName = "Mua sắm",
                    categoryIcon = "ShoppingBag",
                    categoryColor = 0xFFEC4899,
                    date = yesterdayDinner,
                    note = "Đồ dùng gia đình siêu thị",
                    wallet = "Thẻ tín dụng"
                ),
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 650000.0,
                    categoryId = "bills",
                    categoryName = "Hóa đơn & Tiện ích",
                    categoryIcon = "ReceiptLong",
                    categoryColor = 0xFFF59E0B,
                    date = billDate,
                    note = "Tiền điện nước tháng này",
                    wallet = "Tài khoản ngân hàng"
                ),
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 180000.0,
                    categoryId = "transport",
                    categoryName = "Di chuyển",
                    categoryIcon = "DirectionsCar",
                    categoryColor = 0xFF3B82F6,
                    date = billDate + 86400000L,
                    note = "Đổ xăng xe máy",
                    wallet = "Tiền mặt"
                )
            )

            transactionDao.insertAll(transList)
        }
    }
}
