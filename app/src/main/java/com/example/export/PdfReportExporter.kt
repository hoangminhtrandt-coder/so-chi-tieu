package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportExporter {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
    private val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))

    fun exportMonthlyReport(
        context: Context,
        month: Int,
        year: Int,
        transactions: List<TransactionEntity>
    ): File? {
        val pageHeight = 842 // A4 standard pt at 72dpi
        val pageWidth = 595
        val document = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()

        // Background
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        // Top Banner
        paint.color = Color.rgb(15, 118, 110) // Emerald Primary
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BÁO CÁO THU CHI THÁNG $month/$year", 30f, 45f, paint)

        // Subtitle
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val exportedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Sổ Chi Tiêu Cá Nhân • Ngày xuất: $exportedDate", 30f, 70f, paint)

        // Calculate Totals
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense

        // KPI Cards Row (Y = 115)
        val cardY = 115f
        val cardHeight = 65f
        val cardWidth = 165f

        // Card 1: Income
        drawKpiCard(
            canvas,
            30f,
            cardY,
            cardWidth,
            cardHeight,
            "TỔNG THU NHẬP",
            currencyFormat.format(totalIncome),
            Color.rgb(16, 185, 129)
        )

        // Card 2: Expense
        drawKpiCard(
            canvas,
            215f,
            cardY,
            cardWidth,
            cardHeight,
            "TỔNG CHI TIÊU",
            currencyFormat.format(totalExpense),
            Color.rgb(239, 68, 68)
        )

        // Card 3: Net Balance
        val balanceColor = if (netSavings >= 0) Color.rgb(59, 130, 246) else Color.rgb(239, 68, 68)
        drawKpiCard(
            canvas,
            400f,
            cardY,
            cardWidth,
            cardHeight,
            "SỐ DƯ TIẾT KIỆM",
            currencyFormat.format(netSavings),
            balanceColor
        )

        // Section 1: Category Breakdown (Y = 200)
        var currentY = 210f
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("1. Phân bổ chi tiêu theo danh mục", 30f, currentY, paint)

        currentY += 20f
        val expenseByCategory = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryName }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        if (expenseByCategory.isEmpty()) {
            paint.textSize = 10f
            paint.color = Color.GRAY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Chưa có khoản chi tiêu nào trong tháng.", 40f, currentY + 15f, paint)
            currentY += 30f
        } else {
            // Category table header
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 22f, paint)
            paint.color = Color.rgb(71, 85, 105)
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Danh mục", 40f, currentY + 15f, paint)
            canvas.drawText("Số tiền", 340f, currentY + 15f, paint)
            canvas.drawText("Tỷ lệ %", 480f, currentY + 15f, paint)
            currentY += 25f

            // Top 4 categories
            for ((categoryName, amount) in expenseByCategory.take(5)) {
                val percent = if (totalExpense > 0) (amount / totalExpense) * 100 else 0.0
                paint.color = Color.rgb(30, 41, 59)
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(categoryName, 40f, currentY + 12f, paint)
                canvas.drawText(currencyFormat.format(amount), 340f, currentY + 12f, paint)
                canvas.drawText(String.format(Locale.US, "%.1f%%", percent), 480f, currentY + 12f, paint)

                // Divider line
                paint.color = Color.rgb(226, 232, 240)
                canvas.drawLine(30f, currentY + 18f, (pageWidth - 30).toFloat(), currentY + 18f, paint)
                currentY += 22f
            }
        }

        currentY += 20f

        // Section 2: Recent Transaction Ledger
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("2. Danh sách các giao dịch trong tháng", 30f, currentY, paint)

        currentY += 20f
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 22f, paint)
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Ngày", 40f, currentY + 15f, paint)
        canvas.drawText("Danh mục", 130f, currentY + 15f, paint)
        canvas.drawText("Ghi chú", 250f, currentY + 15f, paint)
        canvas.drawText("Số tiền", 440f, currentY + 15f, paint)
        currentY += 26f

        // Rows
        for (item in transactions.take(15)) {
            if (currentY > pageHeight - 50) break
            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            canvas.drawText(dateOnlyFormat.format(Date(item.date)), 40f, currentY + 10f, paint)
            canvas.drawText(item.categoryName, 130f, currentY + 10f, paint)

            val note = if (item.note.length > 22) item.note.take(20) + "..." else item.note
            canvas.drawText(note.ifEmpty { "-" }, 250f, currentY + 10f, paint)

            if (item.type == TransactionType.INCOME) {
                paint.color = Color.rgb(16, 185, 129)
                canvas.drawText("+" + currencyFormat.format(item.amount), 440f, currentY + 10f, paint)
            } else {
                paint.color = Color.rgb(239, 68, 68)
                canvas.drawText("-" + currencyFormat.format(item.amount), 440f, currentY + 10f, paint)
            }

            paint.color = Color.rgb(241, 245, 249)
            canvas.drawLine(30f, currentY + 16f, (pageWidth - 30).toFloat(), currentY + 16f, paint)
            currentY += 20f
        }

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText(
            "Báo cáo được tạo tự động bởi ứng dụng Sổ Chi Tiêu. Giữ bí mật thông tin tài chính cá nhân.",
            30f,
            (pageHeight - 25).toFloat(),
            paint
        )

        document.finishPage(page)

        // Save file
        val outputDir = context.cacheDir
        val outputFile = File(outputDir, "BaoCaoChiTieu_T${month}_${year}.pdf")
        try {
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            return null
        }
    }

    private fun drawKpiCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        amount: String,
        accentColor: Int
    ) {
        val paint = Paint()
        // Card background
        paint.color = Color.WHITE
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, paint)

        // Border
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, paint)

        // Left accent bar
        paint.style = Paint.Style.FILL
        paint.color = accentColor
        canvas.drawRoundRect(x, y, x + 4f, y + height, 2f, 2f, paint)

        // Title
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, x + 12f, y + 22f, paint)

        // Amount
        paint.color = accentColor
        paint.textSize = 12.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(amount, x + 12f, y + 46f, paint)
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Chia sẻ báo cáo PDF tháng")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
