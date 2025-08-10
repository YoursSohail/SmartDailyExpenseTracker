package com.yourssohail.smartdailyexpensetracker.ui.report

import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType

/**
 * Defines constants used for generating PDF reports, such as page dimensions,
 * margins, line spacing, and text sizes. Also provides pre-configured [Paint]
 * objects for different text styles (title, header, body).
 */
internal object PdfConstants {
    const val A4_PAGE_HEIGHT = 842
    const val A4_PAGE_WIDTH = 595
    const val DEFAULT_MARGIN_FLOAT = 40f
    const val LINE_SPACING_FLOAT = 18f
    const val SECTION_SPACING_FLOAT = 28f
    const val TITLE_TEXT_SIZE_FLOAT = 18f
    const val HEADER_TEXT_SIZE_FLOAT = 14f
    const val BODY_TEXT_SIZE_FLOAT = 12f

    /**
     * Creates and returns a [Paint] object configured for PDF title text.
     * Uses bold typeface, [TITLE_TEXT_SIZE_FLOAT], and black color.
     * @return A [Paint] object for title text.
     */
    fun titlePaint(): Paint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = TITLE_TEXT_SIZE_FLOAT
        color = android.graphics.Color.BLACK
    }

    /**
     * Creates and returns a [Paint] object configured for PDF header text.
     * Uses bold typeface, [HEADER_TEXT_SIZE_FLOAT], and black color.
     * @return A [Paint] object for header text.
     */
    fun headerPaint(): Paint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = HEADER_TEXT_SIZE_FLOAT
        color = android.graphics.Color.BLACK
    }

    /**
     * Creates and returns a [Paint] object configured for PDF body text.
     * Uses normal typeface, [BODY_TEXT_SIZE_FLOAT], and black color.
     * @return A [Paint] object for body text.
     */
    fun bodyPaint(): Paint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = BODY_TEXT_SIZE_FLOAT
        color = android.graphics.Color.BLACK
    }
}

/**
 * Represents the total expense amount for a specific day.
 *
 * @property date The timestamp (in milliseconds since epoch) for the start of the day.
 * @property formattedDate The date formatted as a string (e.g., "dd/MM").
 * @property totalAmount The total sum of expenses for this day.
 */
data class DailyTotal(
    val date: Long,
    val formattedDate: String,
    val totalAmount: Double
)

/**
 * Represents the total expense amount for a specific category over a period.
 *
 * @property category The [CategoryType] for which the total is calculated.
 * @property totalAmount The total sum of expenses for this category.
 * @property percentage The percentage of this category's total amount relative to the overall total expenses.
 */
data class CategoryTotal(
    val category: CategoryType,
    val totalAmount: Double,
    val percentage: Float
)

/**
 * Represents the UI state for the Expense Report screen.
 *
 * @property isLoading True if report data is currently being loaded, false otherwise.
 * @property expensesOverLast7Days List of all [Expense] items from the last 7 days.
 * @property dailyTotals List of [DailyTotal] objects, showing total spending per day for the last 7 days.
 * @property categoryTotals List of [CategoryTotal] objects, showing total spending per category for the last 7 days.
 * @property totalForAllCategories The grand total of expenses across all categories for the last 7 days.
 * @property errorMessage An optional error message to be displayed if data loading fails.
 */
data class ExpenseReportUiState(
    val isLoading: Boolean = true,
    val expensesOverLast7Days: List<Expense> = emptyList(),
    val dailyTotals: List<DailyTotal> = emptyList(),
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val totalForAllCategories: Double = 0.0,
    val errorMessage: String? = null
)

/**
 * Represents one-time events that can be emitted from the [ExpenseReportViewModel] to the UI,
 * typically for actions like showing toasts or triggering share intents.
 */
sealed class ReportEvent {
    /**
     * Event to request showing a toast message.
     * @param message The message to be displayed in the toast.
     */
    data class ShowToast(val message: String) : ReportEvent()
    /**
     * Event to trigger sharing of the report summary as plain text.
     * @param summary The generated report summary string.
     */
    data class ShareReport(val summary: String) : ReportEvent()
    /**
     * Event to trigger sharing of a report file (CSV or PDF).
     * @param uri The [Uri] of the file to be shared.
     * @param mimeType The MIME type of the file (e.g., "text/csv", "application/pdf").
     */
    data class ShareFile(val uri: Uri, val mimeType: String) : ReportEvent()
}
