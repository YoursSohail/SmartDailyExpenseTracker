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
import java.io.File
import java.io.FileOutputStream
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

/**
 * ViewModel for the Expense Report screen.
 * Manages fetching, processing, and exposing report data for the last 7 days.
 * Handles report generation (CSV, PDF) and sharing functionalities.
 *
 * @param getSevenDayReportUseCase Use case to fetch expense data for the last seven days.
 * @param applicationContext The application context, required for file operations (saving, caching, sharing).
 */
@HiltViewModel
class ExpenseReportViewModel @Inject constructor(
    private val getSevenDayReportUseCase: GetSevenDayReportUseCase,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseReportUiState())
    /**
     * The current UI state of the Expense Report screen, observed by the UI.
     */
    val uiState: StateFlow<ExpenseReportUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ReportEvent>()
    /**
     * A flow of one-time events to be consumed by the UI (e.g., showing toasts, triggering share actions).
     */
    val eventFlow = _eventFlow.asSharedFlow()

    private val reportDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    /**
     * A flag to toggle between using real data or dummy data for the report.
     * Defaults to true (use dummy data). Change and call [loadReportData] to reflect.
     */
    var useDummyDataForReport = true // Set to true to use dummy data by default

    init {
        loadReportData()
    }

    /**
     * Retrieves or creates the directory for temporarily caching files to be shared.
     *
     * @param context The application context.
     * @return The [File] object representing the cache directory for shared files.
     */
    private fun getShareCacheDir(context: Context): File {
        val cacheDir = File(context.cacheDir, "share_temp")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }

    /**
     * Generates a list of dummy [Expense] objects for the last 7 days.
     * Used for UI development and previewing report features without real data.
     * @return A list of randomly generated [Expense] objects.
     */
    private fun generateDummyExpensesForLast7Days(): List<Expense> {
        val dummyExpenses = mutableListOf<Expense>()
        val calendar = Calendar.getInstance()
        val categories = CategoryType.entries

        for (i in 6 downTo 0) { // Iterate from 6 days ago up to today
            calendar.time = Date() // Reset to current date/time
            calendar.add(Calendar.DAY_OF_YEAR, -i) // Go back 'i' days
            val currentDayMillis = calendar.timeInMillis

            // Generate a random number of expenses for this day
            val numberOfExpensesToday = Random.nextInt(2, 4) // Between 2 and 3 expenses
            for (j in 0 until numberOfExpensesToday) {
                val randomCategory = categories.random()
                val randomAmount = Random.nextDouble(50.0, 500.0) // Random amount
                val expense = Expense(
                    id = Random.nextLong(0, Long.MAX_VALUE), // Positive ID
                    title = "Dummy Expense ${7 - i}-${j + 1} (${randomCategory.name.lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) }})",
                    amount = randomAmount,
                    category = randomCategory.name,
                    date = currentDayMillis - Random.nextInt(0, 1000 * 60 * 60 * 12), // Random time within the day
                    notes = "This is a dummy expense for day ${7-i}."
                )
                dummyExpenses.add(expense)
            }
        }
        // Add a couple of specific expenses for today for better demo
         calendar.time = Date() // Reset to current date/time
        dummyExpenses.add(Expense(Random.nextLong(0,Long.MAX_VALUE), "Lunch Today", 150.0, CategoryType.FOOD.name, calendar.timeInMillis, "Office lunch"))
        dummyExpenses.add(Expense(Random.nextLong(0,Long.MAX_VALUE), "Taxi Today", 300.0, CategoryType.TRAVEL.name, calendar.timeInMillis - TimeUnit.HOURS.toMillis(1) , "Client visit"))
        return dummyExpenses.sortedByDescending { it.date } // Sort by date, newest first
    }

    /**
     * Loads or reloads the expense report data.
     * Fetches expenses for the last 7 days (either real or dummy based on [useDummyDataForReport]),
     * processes them into daily and category totals, and updates the [uiState].
     * Sets loading state and handles potential errors.
     */
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
                            it.copy(
                                isLoading = false,
                                errorMessage = "Error fetching report data: ${e.message}"
                            )
                        }
                    }
                    .collect { expenses ->
                        processExpensesForReport(expenses)
                    }
            }
        }
    }

    /**
     * Formats a map of daily totals into a list of [DailyTotal] objects.
     *
     * @param dailyMap A map where keys are timestamps (start of day) and values are total amounts.
     * @param dateFormat The [SimpleDateFormat] to use for formatting the date string in [DailyTotal].
     * @return A list of [DailyTotal] objects, sorted by date.
     */
    private fun formatDailyTotals(
        dailyMap: Map<Long, Double>,
        dateFormat: SimpleDateFormat
    ): List<DailyTotal> {
        return dailyMap.entries
            .sortedBy { it.key } // Sort by date
            .map {
                DailyTotal(
                    date = it.key,
                    formattedDate = dateFormat.format(Date(it.key)),
                    totalAmount = it.value
                )
            }
    }

    /**
     * Processes a list of expenses to calculate daily totals, category totals,
     * and the overall total for the report period. Updates the UI state with these results.
     *
     * @param expenses The list of [Expense] objects to process.
     */
    private fun processExpensesForReport(expenses: List<Expense>) {
        // Calculate daily totals for the last 7 days (including days with no expenses)
        val dailyMap = mutableMapOf<Long, Double>()
        val calendar = Calendar.getInstance()

        // Initialize map with the last 7 days to ensure all days are present in the chart
        for (i in 6 downTo 0) {
            calendar.time = Date() // Reset to current
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            // Normalize to start of the day
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            dailyMap[calendar.timeInMillis] = 0.0 // Initialize with 0.0
        }

        expenses.forEach { expense ->
            calendar.timeInMillis = expense.date
            // Normalize to start of the day for grouping
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val dayStartMillis = calendar.timeInMillis
            dailyMap[dayStartMillis] = (dailyMap[dayStartMillis] ?: 0.0) + expense.amount
        }

        val dailyTotalsList = formatDailyTotals(dailyMap, shortDateFormat)

        // Calculate category totals
        val categoryMap = mutableMapOf<CategoryType, Double>()
        // Initialize all categories to ensure they appear if needed, or for consistent ordering
        CategoryType.entries.forEach { categoryMap[it] = 0.0 }

        expenses.forEach { expense ->
            try {
                // Ensure category name from DB matches enum name (case-insensitive check then valueOf)
                val categoryName = expense.category.uppercase(Locale.ROOT)
                if (CategoryType.entries.any { it.name == categoryName }) {
                    val categoryEnum = CategoryType.valueOf(categoryName)
                    categoryMap[categoryEnum] = (categoryMap[categoryEnum] ?: 0.0) + expense.amount
                } else {
                    // Log or handle unknown category string if necessary
                    println("Warning: Unknown category literal '${expense.category}' in report processing.")
                }
            } catch (e: IllegalArgumentException) {
                // This catch block handles cases where valueOf might fail even if the string exists,
                // though the pre-check with .any should prevent this.
                println("Warning: Could not parse category '${expense.category}' for expense '${expense.title}'. Error: ${e.message}")
            }
        }
        val totalForAllCategories = categoryMap.values.sum()

        val categoryTotalsList = categoryMap.entries
            .filter { it.value > 0 } // Only include categories with spending
            .map {
                CategoryTotal(
                    category = it.key,
                    totalAmount = it.value,
                    percentage = if (totalForAllCategories > 0) ((it.value / totalForAllCategories) * 100).toFloat() else 0f
                )
            }
            .sortedByDescending { it.totalAmount } // Sort by amount, highest first

        _uiState.update {
            it.copy(
                isLoading = false,
                expensesOverLast7Days = expenses,
                dailyTotals = dailyTotalsList,
                categoryTotals = categoryTotalsList,
                totalForAllCategories = totalForAllCategories,
                errorMessage = if (it.errorMessage != null && expenses.isNotEmpty()) null else it.errorMessage // Clear error if data loaded
            )
        }
    }

    /**
     * Handles the request to share the report summary as text.
     * Generates the summary and emits a [ReportEvent.ShareReport].
     */
    fun onShareTextRequested() {
        viewModelScope.launch {
            val summary = generateReportSummaryText()
            _eventFlow.emit(ReportEvent.ShareReport(summary))
        }
    }

    /**
     * Generates a plain text summary of the current report data.
     * @return A string containing the formatted report summary.
     */
    private fun generateReportSummaryText(): String {
        val state = uiState.value
        val sb = StringBuilder()
        sb.append("Expense Report (Last 7 Days):\n\n")
        sb.append("Daily Totals:\n")
        state.dailyTotals.forEach {
            sb.append("- ${it.formattedDate}: ${currencyFormatter.format(it.totalAmount)}\n")
        }
        sb.append("\n")
        val totalAllCategoriesFormatted = currencyFormatter.format(state.totalForAllCategories)
        sb.append("Category Totals (Total: $totalAllCategoriesFormatted):\n")
        state.categoryTotals.forEach {
            val categoryName = it.category.name.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() }
            sb.append("- $categoryName: ${currencyFormatter.format(it.totalAmount)} (${it.percentage.roundToInt()}%)\n")
        }
        sb.append("\nGenerated by Smart Daily Expense Tracker")
        return sb.toString()
    }

    /**
     * Generates the content for a CSV file from a list of expenses.
     * Includes a header row and formats each expense as a row.
     *
     * @param expenses The list of [Expense] objects to include in the CSV.
     * @return A string containing the full CSV content.
     */
    private fun generateCsvContent(expenses: List<Expense>): String {
        val csvBuilder = StringBuilder()
        // CSV Header
        csvBuilder.appendLine("\"ID\",\"Date\",\"Title\",\"Amount\",\"Category\",\"Notes\"")
        expenses.forEach { expense ->
            val dateString = reportDateFormat.format(Date(expense.date))
            val title = expense.title.replace("\"", "\"\"") // Escape double quotes
            val notes = expense.notes?.replace("\"", "\"\"") ?: "" // Escape double quotes, handle null
            csvBuilder.appendLine("\"${expense.id}\",\"$dateString\",\"$title\",\"${expense.amount}\",\"${expense.category}\",\"$notes\"")
        }
        return csvBuilder.toString()
    }

    /**
     * Handles the request to export the report data as a CSV file.
     * Generates CSV content and saves it to the device's Downloads directory.
     * Emits toast messages for success or failure.
     */
    fun onExportCsvClicked() {
        viewModelScope.launch {
            val expensesToExport = uiState.value.expensesOverLast7Days
            if (expensesToExport.isEmpty() && !useDummyDataForReport) { // Only show toast if real data is empty
                _eventFlow.emit(ReportEvent.ShowToast("No data to export for CSV."))
                return@launch
            }
            val csvContent = generateCsvContent(expensesToExport)
            val timestamp = fileTimestampFormat.format(Date())
            val fileName = "ExpenseReport_Last7Days_$timestamp.csv"

            saveCsvFileToDownloads(fileName, csvContent).onSuccess {
                _eventFlow.emit(ReportEvent.ShowToast("CSV saved to Downloads"))
            }.onFailure { exception ->
                _eventFlow.emit(ReportEvent.ShowToast("Failed to save CSV: ${exception.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    /**
     * Handles the request to export the report data as a PDF file.
     * Generates PDF content and saves it to the device's Downloads directory.
     * Emits toast messages for success or failure.
     */
    fun onExportPdfClicked() {
        viewModelScope.launch {
            val reportData = uiState.value
            if (reportData.expensesOverLast7Days.isEmpty() && !useDummyDataForReport) { // Only show toast if real data is empty
                _eventFlow.emit(ReportEvent.ShowToast("No data to export for PDF."))
                return@launch
            }
            val timestamp = fileTimestampFormat.format(Date())
            val fileName = "ExpenseReport_Last7Days_$timestamp.pdf"

            generateAndSavePdfReportToDownloads(fileName, reportData).onSuccess {
                _eventFlow.emit(ReportEvent.ShowToast("PDF saved to Downloads"))
            }.onFailure { exception ->
                _eventFlow.emit(ReportEvent.ShowToast("Failed to save PDF: ${exception.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    /**
     * Saves CSV content to a file in the public Downloads directory.
     * Uses [MediaStore] for API 29+ and direct file access for older versions.
     *
     * @param fileName The desired name for the CSV file.
     * @param csvContent The string content of the CSV.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun saveCsvFileToDownloads(fileName: String, csvContent: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1) // Mark as pending until write is complete
                    }
                    val resolver = applicationContext.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                    uri?.let {
                        resolver.openOutputStream(it).use { outputStream: OutputStream? ->
                            outputStream?.bufferedWriter()?.use { writer ->
                                writer.write(csvContent)
                            } ?: return@withContext Result.failure(IOException("Failed to open output stream."))
                        }
                        // Now that content is written, clear IS_PENDING
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(it, contentValues, null, null)
                        Result.success(Unit)
                    } ?: Result.failure(IOException("Failed to create MediaStore entry for CSV."))
                } else {
                    @Suppress("DEPRECATION") // For Environment.getExternalStoragePublicDirectory
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { fos ->
                        fos.bufferedWriter().use { writer ->
                            writer.write(csvContent)
                        }
                    }
                    MediaScannerConnection.scanFile(applicationContext, arrayOf(file.absolutePath), null, null)
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.e("ExpenseReportVM", "Error saving CSV to Downloads", e)
                Result.failure(e)
            }
        }
    }


    /**
     * Generates a PDF document from the report data and saves it to the public Downloads directory.
     * Uses [MediaStore] for API 29+ and direct file access for older versions.
     *
     * @param fileName The desired name for the PDF file.
     * @param reportData The [ExpenseReportUiState] containing the data to include in the PDF.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun generateAndSavePdfReportToDownloads(
        fileName: String,
        reportData: ExpenseReportUiState
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val pdfDocument = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(PdfConstants.A4_PAGE_WIDTH, PdfConstants.A4_PAGE_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // PDF Content Drawing Logic
                val titlePaint = PdfConstants.titlePaint()
                val headerPaint = PdfConstants.headerPaint()
                val bodyPaint = PdfConstants.bodyPaint()
                var yPosition = PdfConstants.DEFAULT_MARGIN_FLOAT
                val xMargin = PdfConstants.DEFAULT_MARGIN_FLOAT

                canvas.drawText("Expense Report - Last 7 Days", xMargin, yPosition, titlePaint)
                yPosition += PdfConstants.SECTION_SPACING_FLOAT * 1.5f // More space after main title

                // Daily Totals Section
                canvas.drawText("Daily Totals:", xMargin, yPosition, headerPaint)
                yPosition += PdfConstants.SECTION_SPACING_FLOAT
                reportData.dailyTotals.forEach {
                    canvas.drawText("${it.formattedDate}: ${currencyFormatter.format(it.totalAmount)}", xMargin, yPosition, bodyPaint)
                    yPosition += PdfConstants.LINE_SPACING_FLOAT
                }

                yPosition += PdfConstants.SECTION_SPACING_FLOAT // Space before next section

                // Category Totals Section
                val totalAllCategoriesFormatted = currencyFormatter.format(reportData.totalForAllCategories)
                canvas.drawText("Category Totals (Total: $totalAllCategoriesFormatted):", xMargin, yPosition, headerPaint)
                yPosition += PdfConstants.SECTION_SPACING_FLOAT
                reportData.categoryTotals.forEach {
                    val categoryName = it.category.name.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() }
                    canvas.drawText("$categoryName (${it.percentage.roundToInt()}%): ${currencyFormatter.format(it.totalAmount)}", xMargin, yPosition, bodyPaint)
                    yPosition += PdfConstants.LINE_SPACING_FLOAT
                }

                pdfDocument.finishPage(page)

                // Save PDF to Downloads
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val resolver = applicationContext.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                    uri?.let {
                        resolver.openOutputStream(it).use { outputStream ->
                            if (outputStream == null) return@withContext Result.failure(IOException("Failed to open output stream for PDF."))
                            pdfDocument.writeTo(outputStream)
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(it, contentValues, null, null)
                        Result.success(Unit)
                    } ?: Result.failure(IOException("Failed to create MediaStore entry for PDF."))
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { fos ->
                        pdfDocument.writeTo(fos)
                    }
                    MediaScannerConnection.scanFile(applicationContext, arrayOf(file.absolutePath), null, null)
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.e("ExpenseReportVM", "Error saving PDF to Downloads", e)
                Result.failure(e)
            } finally {
                pdfDocument.close()
            }
        }
    }

    /**
     * Saves CSV content to a temporary file in the app's cache directory for sharing.
     * Uses [FileProvider] to generate a content URI for the cached file.
     *
     * @param context The application context.
     * @param fileName The desired name for the temporary CSV file.
     * @param csvContent The string content of the CSV.
     * @return A [Result] containing the content URI for the cached file on success, or an error on failure.
     */
    private suspend fun saveCsvToCacheForSharing(context: Context, fileName: String, csvContent: String): Result<Uri> {
        return withContext(Dispatchers.IO) {
            try {
                val shareDir = getShareCacheDir(context)
                val file = File(shareDir, fileName)
                file.writeText(csvContent)
                val authority = "${context.packageName}.provider" // Ensure this matches AndroidManifest
                val uri = FileProvider.getUriForFile(context, authority, file)
                Result.success(uri)
            } catch (e: Exception) {
                Log.e("ExpenseReportViewModel", "Error saving CSV to cache for sharing", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Generates a PDF document from report data and saves it to a temporary file
     * in the app's cache directory for sharing.
     * Uses [FileProvider] to generate a content URI for the cached file.
     *
     * @param context The application context.
     * @param fileName The desired name for the temporary PDF file.
     * @param reportData The [ExpenseReportUiState] containing data for the PDF.
     * @return A [Result] containing the content URI for the cached PDF on success, or an error on failure.
     */
    private suspend fun generateAndCachePdfForSharing(context: Context, fileName: String, reportData: ExpenseReportUiState): Result<Uri> {
        return withContext(Dispatchers.IO) {
            val pdfDocument = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(PdfConstants.A4_PAGE_WIDTH, PdfConstants.A4_PAGE_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                // PDF Content Drawing Logic (Same as for saving to downloads)
                val titlePaint = PdfConstants.titlePaint()
                val headerPaint = PdfConstants.headerPaint()
                val bodyPaint = PdfConstants.bodyPaint()
                var yPosition = PdfConstants.DEFAULT_MARGIN_FLOAT
                val xMargin = PdfConstants.DEFAULT_MARGIN_FLOAT

                canvas.drawText("Expense Report - Last 7 Days", xMargin, yPosition, titlePaint)
                yPosition += PdfConstants.SECTION_SPACING_FLOAT * 1.5f
                canvas.drawText("Daily Totals:", xMargin, yPosition, headerPaint)
                yPosition += PdfConstants.SECTION_SPACING_FLOAT
                reportData.dailyTotals.forEach {
                    canvas.drawText("${it.formattedDate}: ${currencyFormatter.format(it.totalAmount)}", xMargin, yPosition, bodyPaint)
                    yPosition += PdfConstants.LINE_SPACING_FLOAT
                }
                yPosition += PdfConstants.SECTION_SPACING_FLOAT
                val totalAllCategoriesFormatted = currencyFormatter.format(reportData.totalForAllCategories)
                canvas.drawText("Category Totals (Total: $totalAllCategoriesFormatted):", xMargin, yPosition, headerPaint)
                yPosition += PdfConstants.SECTION_SPACING_FLOAT
                reportData.categoryTotals.forEach {
                    val categoryName = it.category.name.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() }
                    canvas.drawText("$categoryName (${it.percentage.roundToInt()}%): ${currencyFormatter.format(it.totalAmount)}", xMargin, yPosition, bodyPaint)
                    yPosition += PdfConstants.LINE_SPACING_FLOAT
                }
                pdfDocument.finishPage(page)

                val shareDir = getShareCacheDir(context)
                val file = File(shareDir, fileName)
                FileOutputStream(file).use { fos -> pdfDocument.writeTo(fos) }
                // pdfDocument is closed in the finally block

                val authority = "${context.packageName}.provider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                Result.success(uri)
            } catch (e: Exception) {
                Log.e("ExpenseReportViewModel", "Error generating/caching PDF for sharing", e)
                Result.failure(e)
            } finally {
                 pdfDocument.close() // Ensure document is closed
            }
        }
    }

    /**
     * Handles the request to share the report as a PDF file.
     * Generates the PDF, caches it, and emits a [ReportEvent.ShareFile] with the PDF's content URI.
     */
    fun onSharePdfRequested() {
        viewModelScope.launch {
            val reportData = uiState.value
            if (reportData.expensesOverLast7Days.isEmpty() && !useDummyDataForReport) {
                _eventFlow.emit(ReportEvent.ShowToast("No data to share for PDF."))
                return@launch
            }
            val timestamp = fileTimestampFormat.format(Date())
            val fileName = "ExpenseReport_Share_$timestamp.pdf"

            generateAndCachePdfForSharing(applicationContext, fileName, reportData).onSuccess { uri ->
                _eventFlow.emit(ReportEvent.ShareFile(uri, "application/pdf"))
            }.onFailure { exception ->
                _eventFlow.emit(ReportEvent.ShowToast("Failed to prepare PDF for sharing: ${exception.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    /**
     * Handles the request to share the report as a CSV file.
     * Generates CSV content, caches it, and emits a [ReportEvent.ShareFile] with the CSV's content URI.
     */
    fun onShareCsvRequested() {
        viewModelScope.launch {
            val expensesToShare = uiState.value.expensesOverLast7Days
            if (expensesToShare.isEmpty() && !useDummyDataForReport) {
                _eventFlow.emit(ReportEvent.ShowToast("No data to share for CSV."))
                return@launch
            }
            val csvContent = generateCsvContent(expensesToShare)
            val timestamp = fileTimestampFormat.format(Date())
            val fileName = "ExpenseReport_Share_$timestamp.csv"

            saveCsvToCacheForSharing(applicationContext, fileName, csvContent).onSuccess { uri ->
                _eventFlow.emit(ReportEvent.ShareFile(uri, "text/csv"))
            }.onFailure { exception ->
                _eventFlow.emit(ReportEvent.ShowToast("Failed to prepare CSV for sharing: ${exception.localizedMessage ?: "Unknown error"}"))
            }
        }
    }
}
