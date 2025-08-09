package com.yourssohail.smartdailyexpensetracker.ui.report

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
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
                    coroutineScope.launch { // Launch in a coroutine scope
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
                else -> {}
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
        } else if (uiState.expensesOverLast7Days.isEmpty()) {
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
                    
                    val chartData = remember(uiState.dailyTotals, primaryColor, primaryColorWithAlpha) {
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

                    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
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
                                data = chartData,
                                barProperties = BarProperties(
                                    cornerRadius = Bars.Data.Radius.Circular(4.dp),
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

                ReportSectionCard(title = "Category Totals (Total: ₹${String.format("%.2f", uiState.totalForAllCategories)})") {
                    if (uiState.categoryTotals.isEmpty()){
                         Text("No expenses with categories found.", style = MaterialTheme.typography.bodySmall)
                    }
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

                 ReportSectionCard(title = "Actions") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = viewModel::onExportPdfClicked, modifier = Modifier.weight(1f)) {
                            Text("Export PDF")
                        }
                        Button(onClick = viewModel::onExportCsvClicked, modifier = Modifier.weight(1f)) {
                            Text("Export CSV")
                        }
                    }
                 }
            }
        }
    }
}

// Helper function to save CSV file using MediaStore
private suspend fun saveCsvFile(context: android.content.Context, fileName: String, csvContent: String): Boolean {
    return withContext(Dispatchers.IO) { // Perform file operations on IO dispatcher
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1) // Set as pending until write is complete
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let { fileUri ->
                resolver.openOutputStream(fileUri).use { outputStream: OutputStream? ->
                    outputStream?.bufferedWriter()?.use { writer ->
                        writer.write(csvContent)
                    } ?: return@withContext false // Failed to open output stream
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0) // Mark as not pending
                    resolver.update(fileUri, contentValues, null, null)
                }
                true // Success
            } ?: false // Failed to create MediaStore entry
        } catch (e: Exception) {
            e.printStackTrace()
            false // Error during save
        }
    }
}


@Composable
fun ReportSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
