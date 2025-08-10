package com.yourssohail.smartdailyexpensetracker.ui.report

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.domain.usecase.GetSevenDayReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.random.Random

// PdfConstants moved here from the Screen
private object PdfConstants {
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

// Simplified ReportEvent - file operations are now handled within ViewModel
sealed class ReportEvent {
    data class ShowToast(val message: String) : ReportEvent()
    data class ShareReport(val summary: String) : ReportEvent()
}

@HiltViewModel
class ExpenseReportViewModel @Inject constructor(
    private val getSevenDayReportUseCase: GetSevenDayReportUseCase,
    @ApplicationContext private val applicationContext: Context // Injected Application Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseReportUiState())
    val uiState: StateFlow<ExpenseReportUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ReportEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val reportDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")) // For PDF

    var useDummyDataForReport = true

    init {
        loadReportData()
    }

    private fun generateDummyExpensesForLast7Days(): List<Expense> {
        val dummyExpenses = mutableListOf<Expense>()
        val calendar = Calendar.getInstance()
        val categories = CategoryType.entries

        for (i in 6 downTo 0) { 
            calendar.time = Date() 
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val currentDayMillis = calendar.timeInMillis

            val numberOfExpensesToday = Random.nextInt(2, 4)
            for (j in 0 until numberOfExpensesToday) {
                val randomCategory = categories.random()
                val randomAmount = Random.nextDouble(50.0, 500.0)
                val expense = Expense(
                    id = Random.nextLong(),
                    title = "Dummy Expense ${7-i}-${j+1} (${randomCategory.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }})",
                    amount = randomAmount,
                    category = randomCategory.name,
                    date = currentDayMillis - Random.nextInt(0, 1000 * 60 * 60 * 12),
                    notes = "This is a dummy expense for day ${7-i}."
                )
                dummyExpenses.add(expense)
            }
        }
         calendar.time = Date()
         dummyExpenses.add(Expense(Random.nextLong(), "Lunch Today", 150.0, CategoryType.FOOD.name, calendar.timeInMillis, "Office lunch"))
         dummyExpenses.add(Expense(Random.nextLong(), "Taxi Today", 300.0, CategoryType.TRAVEL.name, calendar.timeInMillis - TimeUnit.HOURS.toMillis(1), "Client visit"))
        return dummyExpenses.sortedByDescending { it.date }
    }

    fun loadReportData() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            if (useDummyDataForReport) {
                val dummyExpenses = generateDummyExpensesForLast7Days()
                processExpensesForReport(dummyExpenses)
            } else {
                getSevenDayReportUseCase()
                    .catch { e ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Error fetching report data: ${e.message}")
                        }
                    }
                    .collect { expenses ->
                        processExpensesForReport(expenses)
                    }
            }
        }
    }

    private fun processExpensesForReport(expenses: List<Expense>) {
        val dailyMap = mutableMapOf<Long, Double>()
        val calendar = Calendar.getInstance()

        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            dailyMap[calendar.timeInMillis] = 0.0
        }

        expenses.forEach { expense ->
            calendar.timeInMillis = expense.date
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val dayStartMillis = calendar.timeInMillis
            dailyMap[dayStartMillis] = (dailyMap[dayStartMillis] ?: 0.0) + expense.amount
        }

        val dailyTotalsList = dailyMap.entries
            .sortedBy { it.key }
            .map { DailyTotal(date = it.key, formattedDate = shortDateFormat.format(Date(it.key)), totalAmount = it.value) }

        val categoryMap = mutableMapOf<CategoryType, Double>()
        CategoryType.values().forEach { categoryMap[it] = 0.0 }

        expenses.forEach { expense ->
            try {
                val categoryName = expense.category.uppercase(Locale.ROOT)
                if (CategoryType.values().any { it.name == categoryName }) {
                    val categoryEnum = CategoryType.valueOf(categoryName)
                    categoryMap[categoryEnum] = (categoryMap[categoryEnum] ?: 0.0) + expense.amount
                } else {
                     println("Warning: Unknown category literal '${expense.category}' in report processing.")
                }
            } catch (e: IllegalArgumentException) {
                println("Warning: Could not parse category '${expense.category}' for expense '${expense.title}'. Error: ${e.message}")
            }
        }
        val totalForAllCategories = categoryMap.values.sum()

        val categoryTotalsList = categoryMap.entries
            .filter { it.value > 0 }
            .map {
                CategoryTotal(
                    category = it.key,
                    totalAmount = it.value,
                    percentage = if (totalForAllCategories > 0) ((it.value / totalForAllCategories) * 100).toFloat() else 0f
                )
            }
            .sortedByDescending { it.totalAmount }

        _uiState.update {
            it.copy(
                isLoading = false,
                expensesOverLast7Days = expenses,
                dailyTotals = dailyTotalsList,
                categoryTotals = categoryTotalsList,
                totalForAllCategories = totalForAllCategories,
                errorMessage = if (it.errorMessage != null && expenses.isNotEmpty()) null else it.errorMessage
            )
        }
    }

    fun onShareReportClicked() {
        viewModelScope.launch {
            val summary = generateReportSummaryText()
            _eventFlow.emit(ReportEvent.ShareReport(summary))
        }
    }

    private fun generateReportSummaryText(): String {
        val state = uiState.value
        val sb = StringBuilder()
        sb.append("Expense Report (Last 7 Days):\n\n") // TODO: Use string resources
        sb.append("Daily Totals:\n")
        state.dailyTotals.forEach {
            sb.append("- ${it.formattedDate}: ${currencyFormatter.format(it.totalAmount)}\n")
        }
        sb.append("\n")
        val totalAllCategoriesFormatted = currencyFormatter.format(state.totalForAllCategories)
        sb.append("Category Totals (Total: $totalAllCategoriesFormatted):\n")
        state.categoryTotals.forEach {
             val categoryName = it.category.name.replaceFirstChar { char -> 
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() 
            }
            sb.append("- $categoryName: ${currencyFormatter.format(it.totalAmount)} (${it.percentage.roundToInt()}%)\n")
        }
        sb.append("\nGenerated by Smart Daily Expense Tracker") // TODO: Use string resource
        return sb.toString()
    }

    private fun generateCsvContent(expenses: List<Expense>): String {
        val csvBuilder = StringBuilder()
        csvBuilder.appendLine("\"ID\",\"Date\",\"Title\",\"Amount\",\"Category\",\"Notes\"")
        expenses.forEach { expense ->
            val dateString = reportDateFormat.format(Date(expense.date))
            val title = expense.title.replace("\"", "\"\"") 
            val notes = expense.notes?.replace("\"", "\"\"") ?: ""
            csvBuilder.appendLine("\"${expense.id}\",\"$dateString\",\"$title\",\"${expense.amount}\",\"${expense.category}\",\"$notes\"")
        }
        return csvBuilder.toString()
    }

    fun onExportCsvClicked() {
        viewModelScope.launch {
            val expensesToExport = uiState.value.expensesOverLast7Days
            if (expensesToExport.isEmpty() && !useDummyDataForReport) {
                _eventFlow.emit(ReportEvent.ShowToast("No data to export for CSV.")) // TODO: Use stringResource
                return@launch
            }
            val csvContent = generateCsvContent(expensesToExport)
            val timestamp = fileTimestampFormat.format(Date())
            val fileName = "ExpenseReport_Last7Days_$timestamp.csv"

            saveCsvFile(fileName, csvContent).onSuccess {
                _eventFlow.emit(ReportEvent.ShowToast("CSV saved to Downloads")) // TODO: Use stringResource
            }.onFailure { exception ->
                _eventFlow.emit(ReportEvent.ShowToast("Failed to save CSV: ${exception.localizedMessage ?: "Unknown error"}")) // TODO: Use stringResource
            }
        }
    }

    fun onExportPdfClicked() {
        viewModelScope.launch {
            val reportData = uiState.value
            if (reportData.expensesOverLast7Days.isEmpty() && !useDummyDataForReport) {
                _eventFlow.emit(ReportEvent.ShowToast("No data to export for PDF.")) // TODO: Use stringResource
                return@launch
            }
            val timestamp = fileTimestampFormat.format(Date())
            val fileName = "ExpenseReport_Last7Days_$timestamp.pdf"
            
            generateAndSavePdfReport(fileName, reportData).onSuccess {
                _eventFlow.emit(ReportEvent.ShowToast("PDF saved to Downloads")) // TODO: Use stringResource
            }.onFailure { exception ->
                _eventFlow.emit(ReportEvent.ShowToast("Failed to save PDF: ${exception.localizedMessage ?: "Unknown error"}")) // TODO: Use stringResource
            }
        }
    }

    private suspend fun saveCsvFile(fileName: String, csvContent: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val resolver = applicationContext.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let { fileUri ->
                    resolver.openOutputStream(fileUri).use { outputStream: OutputStream? ->
                        outputStream?.bufferedWriter()?.use { writer ->
                            writer.write(csvContent)
                        } ?: return@withContext Result.failure(IOException("Failed to open output stream."))
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(fileUri, contentValues, null, null)
                    }
                    Result.success(Unit)
                } ?: Result.failure(IOException("Failed to create MediaStore entry for CSV."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun generateAndSavePdfReport(fileName: String, reportData: ExpenseReportUiState): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val pdfDocument = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(PdfConstants.A4_PAGE_WIDTH, PdfConstants.A4_PAGE_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val titlePaint = PdfConstants.titlePaint()
                val headerPaint = PdfConstants.headerPaint()
                val bodyPaint = PdfConstants.bodyPaint()
                // currencyFormatter is now a class member

                var yPosition = PdfConstants.DEFAULT_MARGIN_FLOAT
                val xMargin = PdfConstants.DEFAULT_MARGIN_FLOAT

                canvas.drawText("Expense Report - Last 7 Days", xMargin, yPosition, titlePaint) // TODO: Use stringResource
                yPosition += PdfConstants.SECTION_SPACING_FLOAT * 1.5f

                canvas.drawText("Daily Totals:", xMargin, yPosition, headerPaint) // TODO: Use stringResource
                yPosition += PdfConstants.SECTION_SPACING_FLOAT
                reportData.dailyTotals.forEach {
                    canvas.drawText("${it.formattedDate}: ${currencyFormatter.format(it.totalAmount)}", xMargin, yPosition, bodyPaint)
                    yPosition += PdfConstants.LINE_SPACING_FLOAT
                }
                yPosition += PdfConstants.SECTION_SPACING_FLOAT
                
                val totalAllCategoriesFormatted = currencyFormatter.format(reportData.totalForAllCategories)
                canvas.drawText("Category Totals (Total: $totalAllCategoriesFormatted):", xMargin, yPosition, headerPaint) // TODO: Use stringResource
                yPosition += PdfConstants.SECTION_SPACING_FLOAT
                reportData.categoryTotals.forEach {
                    val categoryName = it.category.name.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                    canvas.drawText("$categoryName (${it.percentage.roundToInt()}%): ${currencyFormatter.format(it.totalAmount)}", xMargin, yPosition, bodyPaint)
                    yPosition += PdfConstants.LINE_SPACING_FLOAT
                }
                pdfDocument.finishPage(page)

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val resolver = applicationContext.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it).use { outputStream ->
                        if (outputStream == null) return@withContext Result.failure(IOException("Failed to open output stream for PDF."))
                        pdfDocument.writeTo(outputStream)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(it, contentValues, null, null)
                    }
                    Result.success(Unit)
                } ?: Result.failure(IOException("Failed to create MediaStore entry for PDF."))
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                pdfDocument.close()
            }
        }
    }
}
