package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.TimeUtils

data class BarItem(
    val label: String,
    val income: Double,
    val expense: Double
)

@Composable
fun ComparisonBarChart(
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(income, expense, 1.0)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(income, expense) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(700))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bar_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "So sánh Thu - Chi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Bars
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("bar_canvas")
            ) {
                val availableWidth = size.width
                val barWidth = 46.dp.toPx()
                val spacing = 36.dp.toPx()
                val totalBarsWidth = (barWidth * 2) + spacing
                val startX = (availableWidth - totalBarsWidth) / 2f
                val chartHeight = size.height - 24.dp.toPx()

                // Baseline
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 2.dp.toPx()
                )

                // Income Bar
                val incomeBarHeight = ((income / maxVal).toFloat() * chartHeight * progress.value).coerceAtLeast(6f)
                val incomeX = startX
                val incomeY = chartHeight - incomeBarHeight

                drawRoundRect(
                    color = IncomeGreen,
                    topLeft = Offset(incomeX, incomeY),
                    size = Size(barWidth, incomeBarHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                // Expense Bar
                val expenseBarHeight = ((expense / maxVal).toFloat() * chartHeight * progress.value).coerceAtLeast(6f)
                val expenseX = startX + barWidth + spacing
                val expenseY = chartHeight - expenseBarHeight

                drawRoundRect(
                    color = ExpenseRed,
                    topLeft = Offset(expenseX, expenseY),
                    size = Size(barWidth, expenseBarHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend & Amounts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Income Legend
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = IncomeGreen) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Tổng Thu", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = TimeUtils.formatVnd(income),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }

                // Expense Legend
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = ExpenseRed) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Tổng Chi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = TimeUtils.formatVnd(expense),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }
        }
    }
}
