package com.yourssohail.smartdailyexpensetracker.ui.report

import android.content.ContentValues
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.domain.model.Expense
import com.yourssohail.smartdailyexpensetracker.domain.usecase.GetSevenDayReportUseCase
import com.yourssohail.smartdailyexpensetracker.utils.CURRENCY_FORMATTER_INR
import com.yourssohail.smartdailyexpensetracker.utils.DatePatterns
import com.yourssohail.smartdailyexpensetracker.utils.formatDate
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.Date
import java.util.EnumMap
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.random.Random

@HiltViewModel
class ExpenseReportViewModel @Inject constructor(
    private val getSevenDayReportUseCase: GetSevenDayReportUseCase,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseReportUiState())
    val uiState: StateFlow<ExpenseReportUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ReportEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    var useDummyDataForReport = true

    init {
        loadReportData()
    }

    private fun getShareCacheDir(context: Context): File {
        val cacheDir = File(context.cacheDir, "share_temp")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }

    private fun generateDummyExpensesForLast7Days(): List<Expense> {
        val dummyExpenses = mutableListOf<Expense>()
        val calendar = Calendar.getInstance()
        val categories = CategoryType.entries
        for (i in 6 downTo 0) {
            calendar.time = Date() // Current date for each day iteration start
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val currentDayMillis = calendar.timeInMillis
            val numberOfExpensesToday = Random.nextInt(2, 4)
            for (j in 0 until numberOfExpensesToday) {
                val randomCategory = categories.random()
                val randomAmount = Random.nextDouble(50.0, 500.0)
                dummyExpenses.add(Expense(
                    id = Random.nextLong(0, Long.MAX_VALUE),
                    title = "Dummy Expense ${7 - i}-${j + 1} (${randomCategory.name.lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) }})",
                    amount = randomAmount,
                    category = randomCategory.name,
                    date = currentDayMillis - Random.nextInt(0, 1000 * 60 * 60 * 12), // Random time within the day
                    notes = "This is a dummy expense for day ${7-i}."
                ))
            }
        }
        calendar.time = Date() // Reset to current time for today's specific dummies
        dummyExpenses.add(Expense(Random.nextLong(0,Long.MAX_VALUE), "Lunch Today", 150.0, CategoryType.FOOD.name, calendar.timeInMillis, "Office lunch"))
        dummyExpenses.add(Expense(Random.nextLong(0,Long.MAX_VALUE), "Taxi Today", 300.0, CategoryType.TRAVEL.name, calendar.timeInMillis - TimeUnit.HOURS.toMillis(1) , "Client visit"))
        return dummyExpenses.sortedByDescending { it.date }
    }

    fun loadReportData() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            if (useDummyDataForReport) {
                processExpensesForReport(generateDummyExpensesForLast7Days())
            } else {
                getSevenDayReportUseCase()
                    .catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = "Error fetching report data: ${e.message}") } }
                    .collect { expenses -> processExpensesForReport(expenses) }
            }
        }
    }

    private fun formatDailyTotals(dailyMap: Map<Long, Double>): List<DailyTotal> {
        return dailyMap.entries.sortedBy { it.key }.map {
            DailyTotal(date = it.key, formattedDate = formatDate(it.key, DatePatterns.SHORT_DATE_REPORTS), totalAmount = it.value)
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
        val dailyTotalsList = formatDailyTotals(dailyMap)

        val categoryMap = EnumMap<CategoryType, Double>(CategoryType::class.java)
        CategoryType.entries.forEach { categoryMap[it] = 0.0 }
        expenses.forEach { expense ->
            try {
                val categoryEnum = CategoryType.valueOf(expense.category.uppercase(Locale.ROOT))
                categoryMap[categoryEnum] = (categoryMap[categoryEnum] ?: 0.0) + expense.amount
            } catch (e: IllegalArgumentException) {
                Log.w("ReportVM", "Unknown category '${expense.category}' for expense '${expense.title}'")
            }
        }
        val totalForAllCategories = categoryMap.values.sum()
        val categoryTotalsList = categoryMap.entries.filter { it.value > 0 }.map {
            CategoryTotal(category = it.key, totalAmount = it.value, percentage = if (totalForAllCategories > 0) ((it.value / totalForAllCategories) * 100).toFloat() else 0f)
        }.sortedByDescending { it.totalAmount }

        _uiState.update {
            it.copy(isLoading = false, expensesOverLast7Days = expenses, dailyTotals = dailyTotalsList, categoryTotals = categoryTotalsList, totalForAllCategories = totalForAllCategories, errorMessage = if (it.errorMessage != null && expenses.isNotEmpty()) null else it.errorMessage)
        }
    }

    fun onShareTextRequested() {
        viewModelScope.launch {
            _eventFlow.emit(ReportEvent.ShareReport(generateReportSummaryText()))
        }
    }

    private fun generateReportSummaryText(): String {
        val state = uiState.value
        return buildString {
            append("Expense Report (Last 7 Days):\\n\\n")
            append("Daily Totals:\\n")
            state.dailyTotals.forEach { append("- ${it.formattedDate}: ${CURRENCY_FORMATTER_INR.format(it.totalAmount)}\\n") }
            append("\\n")
            val totalAllCategoriesFormatted = CURRENCY_FORMATTER_INR.format(state.totalForAllCategories)
            append("Category Totals (Total: $totalAllCategoriesFormatted):\\n")
            state.categoryTotals.forEach {categoryTotal ->
                val categoryName = categoryTotal.category.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                append("- $categoryName: ${CURRENCY_FORMATTER_INR.format(categoryTotal.totalAmount)} (${categoryTotal.percentage.roundToInt()}%)\\n")
            }
            append("\\nGenerated by Smart Daily Expense Tracker")
        }
    }

    private fun generateCsvContent(expenses: List<Expense>): String {
        return buildString {
            appendLine("\"Date\",\"Title\",\"Amount\",\"Category\",\"Notes\"")
            expenses.forEach { expense ->
                val dateString = formatDate(expense.date, DatePatterns.CSV_DATE)
                val title = expense.title.replace("\"", "\"\"")
                val notes = expense.notes?.replace("\"", "\"\"") ?: ""
                appendLine("\"$dateString\",\"$title\",\"${expense.amount}\",\"${expense.category}\",\"$notes\"")
            }
        }
    }

    fun onExportCsvClicked() {
        viewModelScope.launch {
            val expensesToExport = uiState.value.expensesOverLast7Days
            if (expensesToExport.isEmpty() && !useDummyDataForReport) {
                _eventFlow.emit(ReportEvent.ShowToast("No data to export for CSV."))
                return@launch
            }
            val csvContent = generateCsvContent(expensesToExport)
            val fileName = "ExpenseReport_${formatDate(System.currentTimeMillis(), DatePatterns.FILE_TIMESTAMP)}.csv"
            try {
                saveFileToDownloads(csvContent, fileName, "text/csv")
                _eventFlow.emit(ReportEvent.ShowToast("CSV report saved to Downloads: $fileName"))
            } catch (e: IOException) {
                _eventFlow.emit(ReportEvent.ShowToast("Error saving CSV: ${e.message}"))
            }
        }
    }

    @Throws(IOException::class)
    private suspend fun saveFileToDownloads(content: String, fileName: String, mimeType: String) {
        withContext(Dispatchers.IO) {
            val contentResolver = applicationContext.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                var uri: Uri? = null
                try {
                    uri = contentResolver.insert(collection, contentValues)
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            outputStream.write(content.toByteArray())
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        contentResolver.update(it, contentValues, null, null)
                    } ?: throw IOException("Failed to create MediaStore entry for $fileName")
                } catch (e: Exception) {
                    uri?.let { contentResolver.delete(it, null, null) }
                    throw IOException("Failed to save file to Downloads: ${e.message}", e)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs() // Ensure directory exists
                val file = File(downloadsDir, fileName)
                try {
                    FileOutputStream(file).use { it.write(content.toByteArray()) }
                    MediaScannerConnection.scanFile(applicationContext, arrayOf(file.absolutePath), arrayOf(mimeType), null)
                } catch (e: Exception) {
                    if (file.exists()) file.delete() // Attempt to clean up partially written file
                    throw IOException("Failed to save file to Downloads (legacy): ${e.message}", e)
                }
            }
        }
    }

    private fun generatePdfDocument(): PdfDocument {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply { textSize = 10f }
        var yPosition = 25f
        val xMargin = 20f
        val lineHeight = 12f

        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("Smart Daily Expense Tracker - Report", xMargin, yPosition, paint)
        yPosition += lineHeight * 2
        paint.isFakeBoldText = false
        paint.textSize = 10f
        canvas.drawText("Report Generated: ${formatDate(System.currentTimeMillis(), DatePatterns.REPORT_TIMESTAMP)}", xMargin, yPosition, paint)
        yPosition += lineHeight * 2

        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("Daily Totals (Last 7 Days):", xMargin, yPosition, paint)
        yPosition += lineHeight
        paint.isFakeBoldText = false
        paint.textSize = 10f
        uiState.value.dailyTotals.forEach {
            yPosition += lineHeight
            canvas.drawText("- ${it.formattedDate}: ${CURRENCY_FORMATTER_INR.format(it.totalAmount)}", xMargin + 10f, yPosition, paint)
        }
        yPosition += lineHeight * 2

        paint.textSize = 12f
        paint.isFakeBoldText = true
        val totalFormatted = CURRENCY_FORMATTER_INR.format(uiState.value.totalForAllCategories)
        canvas.drawText("Category Totals (Total: $totalFormatted):", xMargin, yPosition, paint)
        yPosition += lineHeight
        paint.isFakeBoldText = false
        paint.textSize = 10f
        uiState.value.categoryTotals.forEach {
            yPosition += lineHeight
            val categoryName = it.category.name.lowercase().replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }
            canvas.drawText("- $categoryName: ${CURRENCY_FORMATTER_INR.format(it.totalAmount)} (${it.percentage.roundToInt()}%)", xMargin + 10f, yPosition, paint)
        }
        yPosition += lineHeight * 2

        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("All Expenses (Last 7 Days):", xMargin, yPosition, paint)
        yPosition += lineHeight
        paint.isFakeBoldText = false
        paint.textSize = 8f
        val itemLineHeight = 10f
        uiState.value.expensesOverLast7Days.take(50).forEach {
            yPosition += itemLineHeight
            if (yPosition > pageInfo.pageHeight - 20) {
                pdfDocument.finishPage(page) // Finish current page
                // Note: For a real multi-page PDF, you'd start a new page here and reset yPosition.
                // This implementation will truncate if content exceeds one page after this point.
                return@forEach
            }
            val expenseText = "${formatDate(it.date, DatePatterns.CSV_DATE)} - ${it.title} (${it.category}): ${CURRENCY_FORMATTER_INR.format(it.amount)}"
            canvas.drawText(expenseText, xMargin + 10f, yPosition, paint)
        }
        pdfDocument.finishPage(page)
        return pdfDocument
    }

    @Throws(IOException::class)
    private suspend fun savePdfToDownloads(document: PdfDocument, fileName: String) {
        withContext(Dispatchers.IO) {
            val contentResolver = applicationContext.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                var uri: Uri? = null
                try {
                    uri = contentResolver.insert(collection, contentValues)
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            document.writeTo(outputStream)
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        contentResolver.update(it, contentValues, null, null)
                    } ?: throw IOException("Failed to create MediaStore entry for $fileName")
                } catch (e: Exception) {
                    uri?.let { contentResolver.delete(it, null, null) }
                    throw IOException("Failed to save PDF to Downloads: ${e.message}", e)
                } finally {
                    document.close() // Close document regardless of success or failure in Q+
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                try {
                    FileOutputStream(file).use { document.writeTo(it) }
                    MediaScannerConnection.scanFile(applicationContext, arrayOf(file.absolutePath), arrayOf("application/pdf"), null)
                } catch (e: Exception) {
                    if (file.exists()) file.delete()
                    throw IOException("Failed to save PDF to Downloads (legacy): ${e.message}", e)
                } finally {
                    document.close() // Close document regardless of success or failure pre-Q
                }
            }
        }
    }

    fun onExportPdfClicked() {
        viewModelScope.launch {
            if (uiState.value.expensesOverLast7Days.isEmpty() && !useDummyDataForReport) {
                _eventFlow.emit(ReportEvent.ShowToast("No data to export for PDF."))
                return@launch
            }
            val pdfDocument = generatePdfDocument() 
            val fileName = "ExpenseReport_${formatDate(System.currentTimeMillis(), DatePatterns.FILE_TIMESTAMP)}.pdf"
            try {
                savePdfToDownloads(pdfDocument, fileName)
                _eventFlow.emit(ReportEvent.ShowToast("PDF report saved to Downloads: $fileName"))
            } catch (e: IOException) {
                _eventFlow.emit(ReportEvent.ShowToast("Error saving PDF: ${e.message}"))
            } 
        }
    }

    private suspend fun saveFileToCacheForSharing(content: String, fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val cachePath = getShareCacheDir(applicationContext)
                val file = File(cachePath, fileName)
                FileOutputStream(file).use { it.write(content.toByteArray()) }
                FileProvider.getUriForFile(applicationContext, "${applicationContext.packageName}.provider", file)
            } catch (e: Exception) {
                Log.e("ShareError", "Failed to save $fileName to cache for sharing", e)
                _eventFlow.emit(ReportEvent.ShowToast("Error preparing $fileName for sharing."))
                null
            }
        }
    }

    private suspend fun savePdfToCacheForSharing(document: PdfDocument, fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val cachePath = getShareCacheDir(applicationContext)
                val file = File(cachePath, fileName)
                FileOutputStream(file).use { document.writeTo(it) }
                FileProvider.getUriForFile(applicationContext, "${applicationContext.packageName}.provider", file)
            } catch (e: Exception) {
                Log.e("ShareError", "Failed to save PDF $fileName to cache for sharing", e)
                _eventFlow.emit(ReportEvent.ShowToast("Error preparing PDF for sharing."))
                null
            } finally {
                document.close()
            }
        }
    }

    fun onShareCsvRequested() {
        viewModelScope.launch {
            val expensesToExport = uiState.value.expensesOverLast7Days
            if (expensesToExport.isEmpty() && !useDummyDataForReport) {
                _eventFlow.emit(ReportEvent.ShowToast("No data to share for CSV."))
                return@launch
            }
            val csvContent = generateCsvContent(expensesToExport)
            val fileName = "ExpenseReport_Share_${formatDate(System.currentTimeMillis(), DatePatterns.FILE_TIMESTAMP)}.csv"
            saveFileToCacheForSharing(csvContent, fileName)?.let {
                _eventFlow.emit(ReportEvent.ShareFile(it, "text/csv"))
            }
        }
    }

    fun onSharePdfRequested() {
        viewModelScope.launch {
            if (uiState.value.expensesOverLast7Days.isEmpty() && !useDummyDataForReport) {
                _eventFlow.emit(ReportEvent.ShowToast("No data to share for PDF."))
                return@launch
            }
            val pdfDocument = generatePdfDocument()
            val fileName = "ExpenseReport_Share_${formatDate(System.currentTimeMillis(), DatePatterns.FILE_TIMESTAMP)}.pdf"
            savePdfToCacheForSharing(pdfDocument, fileName)?.let {
                _eventFlow.emit(ReportEvent.ShareFile(it, "application/pdf"))
            }
        }
    }
}
