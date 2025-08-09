package com.yourssohail.smartdailyexpensetracker.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.domain.usecase.GetSevenDayReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.random.Random

data class DailyTotal(
    val date: Long, // Timestamp for the start of the day
    val formattedDate: String,
    val totalAmount: Double
)

data class CategoryTotal(
    val category: CategoryType,
    val totalAmount: Double,
    val percentage: Float // Percentage of total expenses for the period
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
    data class RequestCsvExport(val fileName: String, val csvContent: String) : ReportEvent()
    // New event for PDF export, passing the current UI state for report data
    data class RequestPdfExport(val fileName: String, val reportData: ExpenseReportUiState) : ReportEvent()
}


@HiltViewModel
class ExpenseReportViewModel @Inject constructor(
    private val getSevenDayReportUseCase: GetSevenDayReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseReportUiState())
    val uiState: StateFlow<ExpenseReportUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ReportEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val reportDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())


    // Flag to control dummy data. Set to true to always show dummy data for reports.
    private val useDummyDataForReport = true // TODO: Set to false for production

    init {
        loadReportData()
    }

    private fun generateDummyExpensesForLast7Days(): List<Expense> {
        val dummyExpenses = mutableListOf<Expense>()
        val calendar = Calendar.getInstance()
        val categories = CategoryType.values()

        for (i in 6 downTo 0) { // Last 7 days, including today
            calendar.time = Date() // Reset to today
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val currentDayMillis = calendar.timeInMillis

            val numberOfExpensesToday = Random.nextInt(2, 4)
            for (j in 0 until numberOfExpensesToday) {
                val randomCategory = categories.random()
                val randomAmount = Random.nextDouble(50.0, 500.0)
                val expense = Expense(
                    id = Random.nextLong(),
                    title = "Dummy Expense ${7-i}-${j+1} (${randomCategory.name.lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) }})",
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
        sb.append("Expense Report (Last 7 Days):\n\n")
        sb.append("Daily Totals:\n")
        state.dailyTotals.forEach {
            sb.append("- ${it.formattedDate}: ₹${String.format("%.2f", it.totalAmount)}\n")
        }
        sb.append("\n")
        sb.append("Category Totals (Total: ₹${String.format("%.2f", state.totalForAllCategories)}):\n")
        state.categoryTotals.forEach {
            sb.append("- ${it.category.name.lowercase().replaceFirstChar { cat -> cat.titlecase(Locale.ROOT) }}: ₹${String.format("%.2f", it.totalAmount)} (${it.percentage.roundToInt()}%)\n")
        }
        sb.append("\nGenerated by Smart Daily Expense Tracker")
        return sb.toString()
    }

    private fun generateCsvContent(expenses: List<Expense>): String {
        val csvBuilder = StringBuilder()
        // Header Row
        csvBuilder.appendLine("\"ID\",\"Date\",\"Title\",\"Amount\",\"Category\",\"Notes\"")

        // Data Rows
        expenses.forEach { expense ->
            val dateString = reportDateFormat.format(Date(expense.date))
            val title = expense.title.replace("\"", "\"\"") // Escape double quotes
            val notes = expense.notes?.replace("\"", "\"\"") ?: "" // Escape double quotes, handle null
            csvBuilder.appendLine("\"${expense.id}\",\"$dateString\",\"$title\",\"${expense.amount}\",\"${expense.category}\",\"$notes\"")
        }
        return csvBuilder.toString()
    }

    fun onExportPdfClicked() {
        viewModelScope.launch {
            val reportData = uiState.value
            if (reportData.expensesOverLast7Days.isEmpty() && !useDummyDataForReport) { // Check if there's actual data if not using dummies
                _eventFlow.emit(ReportEvent.ShowToast("No data to export for PDF."))
                return@launch
            }
            val timestamp = fileTimestampFormat.format(Date())
            val fileName = "ExpenseReport_Last7Days_$timestamp.pdf"
            _eventFlow.emit(ReportEvent.RequestPdfExport(fileName, reportData))
        }
    }

    fun onExportCsvClicked() {
        viewModelScope.launch {
            val expensesToExport = uiState.value.expensesOverLast7Days
            if (expensesToExport.isEmpty()) {
                _eventFlow.emit(ReportEvent.ShowToast("No data to export for CSV."))
                return@launch
            }

            val csvContent = generateCsvContent(expensesToExport)
            val timestamp = fileTimestampFormat.format(Date())
            val fileName = "ExpenseReport_Last7Days_$timestamp.csv"

            _eventFlow.emit(ReportEvent.RequestCsvExport(fileName, csvContent))
        }
    }
}
