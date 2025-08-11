package com.yourssohail.smartdailyexpensetracker.ui.report

import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.domain.model.Expense

internal object PdfConstants {
    const val A4_PAGE_HEIGHT = 842
    const val A4_PAGE_WIDTH = 595
    const val DEFAULT_MARGIN_FLOAT = 40f
    const val LINE_SPACING_FLOAT = 18f
    const val SECTION_SPACING_FLOAT = 28f
    const val TITLE_TEXT_SIZE_FLOAT = 18f
    const val HEADER_TEXT_SIZE_FLOAT = 14f
    const val BODY_TEXT_SIZE_FLOAT = 12f

    fun titlePaint(): Paint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = TITLE_TEXT_SIZE_FLOAT
        color = android.graphics.Color.BLACK
    }

    fun headerPaint(): Paint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = HEADER_TEXT_SIZE_FLOAT
        color = android.graphics.Color.BLACK
    }

    fun bodyPaint(): Paint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = BODY_TEXT_SIZE_FLOAT
        color = android.graphics.Color.BLACK
    }
}

data class DailyTotal(
    val date: Long,
    val formattedDate: String,
    val totalAmount: Double
)

data class CategoryTotal(
    val category: CategoryType,
    val totalAmount: Double,
    val percentage: Float
)

data class ExpenseReportUiState(
    val isLoading: Boolean = true,
    val expensesOverLast7Days: List<Expense> = emptyList(),
    val dailyTotals: List<DailyTotal> = emptyList(),
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val totalForAllCategories: Double = 0.0,
    val errorMessage: String? = null
)

sealed class ReportEvent {
    data class ShowToast(val message: String) : ReportEvent()
    data class ShareReport(val summary: String) : ReportEvent()
    data class ShareFile(val uri: Uri, val mimeType: String) : ReportEvent()
}
