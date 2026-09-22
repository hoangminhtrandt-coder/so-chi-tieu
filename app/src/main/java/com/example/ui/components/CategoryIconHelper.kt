package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    fun getIcon(iconName: String): ImageVector {
        return when (iconName) {
            "Restaurant" -> Icons.Default.Restaurant
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "ShoppingBag" -> Icons.Default.ShoppingBag
            "ReceiptLong" -> Icons.AutoMirrored.Filled.ReceiptLong
            "SportsEsports" -> Icons.Default.SportsEsports
            "LocalHospital" -> Icons.Default.LocalHospital
            "School" -> Icons.Default.School
            "Home" -> Icons.Default.Home
            "Payments" -> Icons.Default.Payments
            "CardGiftcard" -> Icons.Default.CardGiftcard
            "TrendingUp" -> Icons.Default.TrendingUp
            "Work" -> Icons.Default.Work
            "Favorite" -> Icons.Default.Favorite
            "AttachMoney" -> Icons.Default.AttachMoney
            "MoreHoriz" -> Icons.Default.MoreHoriz
            else -> Icons.Default.Category
        }
    }
}
