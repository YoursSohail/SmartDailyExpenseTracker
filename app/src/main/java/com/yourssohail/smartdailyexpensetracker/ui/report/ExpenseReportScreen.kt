package com.yourssohail.smartdailyexpensetracker.ui.report

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.IndicatorPosition
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseReportScreen(
    viewModel: ExpenseReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ReportEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is ReportEvent.ShareReport -> {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, event.summary)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
                is ReportEvent.RequestCsvExport -> {
                    coroutineScope.launch { 
                        val success = saveCsvFile(
                            context = context,
                            fileName = event.fileName,
                            csvContent = event.csvContent
                        )
                        if (success) {
                            Toast.makeText(context, "CSV saved to Downloads", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Failed to save CSV", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                is ReportEvent.RequestPdfExport -> { // Handle PDF export event
                    coroutineScope.launch {
                        val success = generateAndSavePdfReport(
                            context = context,
                            fileName = event.fileName,
                            reportData = event.reportData
                        )
                        if (success) {
                            Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Report") },
                actions = {
                    IconButton(onClick = viewModel::onShareReportClicked) {
                        Icon(Icons.Default.Share, contentDescription = "Share Report")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (uiState.expensesOverLast7Days.isEmpty() && !viewModel.useDummyDataForReport) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No expenses recorded in the last 7 days to generate a report.")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Daily Spending Chart (Last 7 Days)", style = MaterialTheme.typography.titleMedium)

                if (uiState.dailyTotals.isNotEmpty()) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val primaryColorWithAlpha = primaryColor.copy(alpha = 0.7f) 

                    val columnChartData = remember(uiState.dailyTotals, primaryColor, primaryColorWithAlpha) {
                        uiState.dailyTotals.map { dailyTotal ->
                            Bars(
                                label = dailyTotal.formattedDate, 
                                values = listOf(
                                    Bars.Data(
                                        label = "Expense", 
                                        value = dailyTotal.totalAmount,
                                        color = Brush.verticalGradient(
                                            colors = listOf(primaryColor, primaryColorWithAlpha)
                                        )
                                    )
                                )
                            )
                        }
                    }

                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(16.dp)
                        ) {
                            Text(
                                "Daily Expenses",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            ColumnChart(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                data = columnChartData,
                                labelProperties = LabelProperties(
                                    enabled = true,
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.sp 
                                    )
                                ),
                                indicatorProperties = HorizontalIndicatorProperties(
                                    textStyle = TextStyle(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    count = IndicatorCount.CountBased(count = 4),
                                    position = IndicatorPosition.Horizontal.Start,
                                ),
                                barProperties = BarProperties(
                                    cornerRadius = Bars.Data.Radius.Circular(4.dp),
                                ),
                                labelHelperProperties = LabelHelperProperties(
                                    textStyle = TextStyle(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                ),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                            )
                        }
                    }
                } else {
                    Text("Not enough data for chart.", style = MaterialTheme.typography.bodySmall)
                }

                ReportSectionCard(title = "Daily Totals") {
                    if (uiState.dailyTotals.isEmpty()){
                        Text("No daily totals to display.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        uiState.dailyTotals.forEach { daily ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(daily.formattedDate, style = MaterialTheme.typography.bodyLarge) 
                                Text("₹${String.format("%.2f", daily.totalAmount)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                            HorizontalDivider()
                        }
                    }
                }

                ReportSectionCard(title = "Category Totals (Total: ₹${String.format("%.2f", uiState.totalForAllCategories)})") {
                    if (uiState.categoryTotals.isEmpty()){
                         Text("No expenses with categories found.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        uiState.categoryTotals.forEach { categoryTotal ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${categoryTotal.category.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }} (${categoryTotal.percentage.roundToInt()}%)", style = MaterialTheme.typography.bodyLarge)
                                Text("₹${String.format("%.2f", categoryTotal.totalAmount)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                            LinearProgressIndicator(
                                progress = { categoryTotal.percentage / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider()
                        }
                    }
                }

                 ReportSectionCard(title = "Actions") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = viewModel::onExportPdfClicked,
                            modifier = Modifier.weight(1f),
                            enabled = uiState.expensesOverLast7Days.isNotEmpty() || viewModel.useDummyDataForReport
                        ) {
                            Text("Export PDF")
                        }
                        Button(
                            onClick = viewModel::onExportCsvClicked,
                            modifier = Modifier.weight(1f),
                            enabled = uiState.expensesOverLast7Days.isNotEmpty() || viewModel.useDummyDataForReport
                        ) {
                            Text("Export CSV")
                        }
                    }
                 }
            }
        }
    }
}

// Helper function to save CSV file using MediaStore
private suspend fun saveCsvFile(context: Context, fileName: String, csvContent: String): Boolean {
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

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let { fileUri ->
                resolver.openOutputStream(fileUri).use { outputStream: OutputStream? ->
                    outputStream?.bufferedWriter()?.use { writer ->
                        writer.write(csvContent)
                    } ?: return@withContext false 
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear() 
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0) 
                    resolver.update(fileUri, contentValues, null, null)
                }
                true 
            } ?: false 
        } catch (e: Exception) {
            e.printStackTrace() 
            false 
        }
    }
}

// Helper function to generate and save PDF report
private suspend fun generateAndSavePdfReport(context: Context, fileName: String, reportData: ExpenseReportUiState): Boolean {
    return withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageHeight = 842 // A4 height in points
        val pageWidth = 595 // A4 width in points
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = android.graphics.Color.BLACK // Standard black for PDF
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = android.graphics.Color.BLACK
        }
        val bodyPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 12f
            color = android.graphics.Color.BLACK
        }

        var yPosition = 40f
        val xMargin = 40f
        val lineSpacing = 18f
        val sectionSpacing = 28f

        // Report Title
        canvas.drawText("Expense Report - Last 7 Days", xMargin, yPosition, titlePaint)
        yPosition += sectionSpacing * 1.5f

        // Daily Totals
        canvas.drawText("Daily Totals:", xMargin, yPosition, headerPaint)
        yPosition += sectionSpacing
        reportData.dailyTotals.forEach {
            canvas.drawText("${it.formattedDate}: ₹${String.format("%.2f", it.totalAmount)}", xMargin, yPosition, bodyPaint)
            yPosition += lineSpacing
        }
        yPosition += sectionSpacing

        // Category Totals
        canvas.drawText("Category Totals (Total: ₹${String.format("%.2f", reportData.totalForAllCategories)}):", xMargin, yPosition, headerPaint)
        yPosition += sectionSpacing
        reportData.categoryTotals.forEach {
            canvas.drawText("${it.category.name.lowercase().replaceFirstChar { cat -> cat.titlecase(Locale.getDefault()) }} (${it.percentage.roundToInt()}%): ₹${String.format("%.2f", it.totalAmount)}", xMargin, yPosition, bodyPaint)
            yPosition += lineSpacing
        }

        pdfDocument.finishPage(page)

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it).use { outputStream ->
                    if (outputStream == null) return@withContext false
                    pdfDocument.writeTo(outputStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
                pdfDocument.close()
                true
            } ?: run {
                pdfDocument.close()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            false
        }
    }
}
