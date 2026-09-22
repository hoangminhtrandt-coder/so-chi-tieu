package com.example.data.model

enum class TimeFilter(val title: String) {
    TODAY("Hôm nay"),
    WEEK("Tuần này"),
    MONTH("Tháng này"),
    YEAR("Năm nay"),
    ALL("Tất cả")
}

data class CategoryStat(
    val categoryId: String,
    val categoryName: String,
    val iconName: String,
    val color: Long,
    val totalAmount: Double,
    val percentage: Float, // 0..100
    val count: Int
)

data class BudgetProgress(
    val budgetId: Long,
    val categoryId: String,
    val categoryName: String,
    val iconName: String,
    val color: Long,
    val limit: Double,
    val spent: Double,
    val remaining: Double,
    val progressPercent: Float, // 0..1+
    val isExceeded: Boolean,
    val isNearLimit: Boolean
)
