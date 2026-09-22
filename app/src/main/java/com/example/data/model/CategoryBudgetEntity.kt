package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: String,
    val categoryName: String,
    val monthlyLimit: Double,
    val month: Int, // 1 - 12
    val year: Int,  // e.g. 2026
    val updatedAt: Long = System.currentTimeMillis()
)
