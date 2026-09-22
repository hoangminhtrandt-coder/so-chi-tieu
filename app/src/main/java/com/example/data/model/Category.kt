package com.example.data.model

data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val color: Long,
    val type: TransactionType
)

object DefaultCategories {
    val expenseCategories = listOf(
        Category("food", "Ăn uống", "Restaurant", 0xFFEF4444, TransactionType.EXPENSE),
        Category("transport", "Di chuyển", "DirectionsCar", 0xFF3B82F6, TransactionType.EXPENSE),
        Category("shopping", "Mua sắm", "ShoppingBag", 0xFFEC4899, TransactionType.EXPENSE),
        Category("bills", "Hóa đơn & Tiện ích", "ReceiptLong", 0xFFF59E0B, TransactionType.EXPENSE),
        Category("entertainment", "Giải trí", "SportsEsports", 0xFF8B5CF6, TransactionType.EXPENSE),
        Category("health", "Y tế & Sức khỏe", "LocalHospital", 0xFF10B981, TransactionType.EXPENSE),
        Category("education", "Giáo dục", "School", 0xFF06B6D4, TransactionType.EXPENSE),
        Category("housing", "Nhà cửa", "Home", 0xFF6366F1, TransactionType.EXPENSE),
        Category("other_expense", "Chi phí khác", "MoreHoriz", 0xFF64748B, TransactionType.EXPENSE)
    )

    val incomeCategories = listOf(
        Category("salary", "Tiền lương", "Payments", 0xFF10B981, TransactionType.INCOME),
        Category("bonus", "Thưởng & Phụ cấp", "CardGiftcard", 0xFF3B82F6, TransactionType.INCOME),
        Category("investment", "Đầu tư & Lãi", "TrendingUp", 0xFF8B5CF6, TransactionType.INCOME),
        Category("part_time", "Nghề tay trái", "Work", 0xFFF59E0B, TransactionType.INCOME),
        Category("gift", "Quà tặng", "Favorite", 0xFFEC4899, TransactionType.INCOME),
        Category("other_income", "Thu nhập khác", "AttachMoney", 0xFF14B8A6, TransactionType.INCOME)
    )

    val allCategories = expenseCategories + incomeCategories

    fun getCategoryById(id: String): Category? = allCategories.find { it.id == id }

    val walletOptions = listOf("Tiền mặt", "Tài khoản ngân hàng", "Ví điện tử (MoMo/ZaloPay)", "Thẻ tín dụng")
}
