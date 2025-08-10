package com.yourssohail.smartdailyexpensetracker.ui.report

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourssohail.smartdailyexpensetracker.ui.common.EmptyStateView
import com.yourssohail.smartdailyexpensetracker.ui.common.FullScreenLoadingIndicator
import com.yourssohail.smartdailyexpensetracker.ui.common.ScreenErrorMessage
import com.yourssohail.smartdailyexpensetracker.ui.common.SectionTitle
import ir.ehsannarmani.compose_charts.models.Bars
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseReportScreen(
    viewModel: ExpenseReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    }

    var showExportBottomSheet by remember { mutableStateOf(false) } 
    val exportBottomSheetState = rememberModalBottomSheetState() 

    var showShareOptionsBottomSheet by remember { mutableStateOf(false) }
    val shareOptionsSheetState = rememberModalBottomSheetState()

    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ReportEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
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
                is ReportEvent.ShareFile -> { 
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        type = event.mimeType
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share Report File")
                    context.startActivity(shareIntent)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Report") },
                actions = {
                    IconButton(onClick = { showExportBottomSheet = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Export Report")
                    }
                    IconButton(onClick = { showShareOptionsBottomSheet = true }) { 
                        Icon(Icons.Default.Share, contentDescription = "Share Report Options")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) { 
            when {
                uiState.isLoading -> {
                    FullScreenLoadingIndicator()
                }
                uiState.errorMessage != null -> {
                    ScreenErrorMessage(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.loadReportData() } 
                    )
                }
                uiState.expensesOverLast7Days.isEmpty() && !viewModel.useDummyDataForReport -> {
                    EmptyStateView(message = "No expenses recorded in the last 7 days to generate a report.")
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp) 
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SectionTitle(text = "Daily Spending Chart (Last 7 Days)")

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
                            DailyExpenseChartCard(columnChartData = columnChartData)

                        } else {
                            Text("Not enough data for chart.", style = MaterialTheme.typography.bodySmall)
                        }
                        val totalFormatted = currencyFormatter.format(uiState.totalForAllCategories)
                        ReportSectionCard(title = "Category Totals (Total: $totalFormatted)") {
                            if (uiState.categoryTotals.isEmpty()){
                                 Text("No expenses with categories found.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                uiState.categoryTotals.forEach { categoryTotal ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val categoryName = categoryTotal.category.name.replaceFirstChar { 
                                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                                        }
                                        Text("$categoryName (${categoryTotal.percentage.roundToInt()}%)", style = MaterialTheme.typography.bodyLarge)
                                        Text(currencyFormatter.format(categoryTotal.totalAmount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    }
                                    LinearProgressIndicator(
                                        progress = { categoryTotal.percentage / 100f },
                                        color = categoryTotal.category.color, 
                                        modifier = Modifier.fillMaxWidth().height(6.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }

            // Export Bottom Sheet
            if (showExportBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showExportBottomSheet = false },
                    sheetState = exportBottomSheetState,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        ListItem(
                            headlineContent = { Text("Export as CSV") },
                            leadingContent = { 
                                Icon(
                                    Icons.Outlined.TableView, 
                                    contentDescription = "Export as CSV"
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.onExportCsvClicked()
                                scope.launch {
                                    exportBottomSheetState.hide()
                                }.invokeOnCompletion { 
                                    if (!exportBottomSheetState.isVisible) {
                                        showExportBottomSheet = false
                                    }
                                }
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Export as PDF") },
                            leadingContent = { 
                                Icon(
                                    Icons.Outlined.PictureAsPdf, 
                                    contentDescription = "Export as PDF"
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.onExportPdfClicked()
                                scope.launch {
                                    exportBottomSheetState.hide()
                                }.invokeOnCompletion { 
                                    if (!exportBottomSheetState.isVisible) {
                                        showExportBottomSheet = false
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Share Options Bottom Sheet
            if (showShareOptionsBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showShareOptionsBottomSheet = false },
                    sheetState = shareOptionsSheetState,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        ListItem(
                            headlineContent = { Text("Share as PDF") },
                            leadingContent = {
                                Icon(
                                    Icons.Outlined.PictureAsPdf,
                                    contentDescription = "Share as PDF"
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.onSharePdfRequested()
                                scope.launch {
                                    shareOptionsSheetState.hide()
                                }.invokeOnCompletion {
                                    if (!shareOptionsSheetState.isVisible) {
                                        showShareOptionsBottomSheet = false
                                    }
                                }
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Share as CSV") },
                            leadingContent = {
                                Icon(
                                    Icons.Outlined.TableView,
                                    contentDescription = "Share as CSV"
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.onShareCsvRequested()
                                scope.launch {
                                    shareOptionsSheetState.hide()
                                }.invokeOnCompletion {
                                    if (!shareOptionsSheetState.isVisible) {
                                        showShareOptionsBottomSheet = false
                                    }
                                }
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Share as Text") },
                            leadingContent = {
                                Icon(
                                    Icons.Outlined.TextSnippet,
                                    contentDescription = "Share as Text"
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.onShareTextRequested()
                                scope.launch {
                                    shareOptionsSheetState.hide()
                                }.invokeOnCompletion {
                                    if (!shareOptionsSheetState.isVisible) {
                                        showShareOptionsBottomSheet = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
