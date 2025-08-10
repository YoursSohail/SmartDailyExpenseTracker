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
import androidx.compose.material.icons.automirrored.outlined.TextSnippet // Updated Icon
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableView
// For stubs, Card and CardDefaults are used, so they should remain if stubs are used.
// If DailyExpenseChartCard and ReportSectionCard are actual composables from another file, these might not be needed here.
// For the provided code, the stubs DO use Card and CardDefaults.
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
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
// import androidx.compose.ui.graphics.Color // No longer needed directly if stubs are basic
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense // For Preview
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.ui.common.EmptyStateView
import com.yourssohail.smartdailyexpensetracker.ui.common.FullScreenLoadingIndicator
import com.yourssohail.smartdailyexpensetracker.ui.common.ScreenErrorMessage
import com.yourssohail.smartdailyexpensetracker.ui.common.SectionTitle
import ir.ehsannarmani.compose_charts.models.Bars
import kotlinx.coroutines.CoroutineScope
// import kotlinx.coroutines.launch // No longer needed directly here
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
    var showShareOptionsBottomSheet by remember { mutableStateOf(false) }

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

    val formattedTotalForAllCategories = remember(uiState.totalForAllCategories) {
        currencyFormatter.format(uiState.totalForAllCategories)
    }

    ExpenseReportScreenContent(
        uiState = uiState,
        formattedTotalForAllCategories = formattedTotalForAllCategories,
        currencyFormatter = currencyFormatter,
        showExportBottomSheet = showExportBottomSheet,
        onDismissExportBottomSheet = { showExportBottomSheet = false },
        onExportCsvClicked = {
            viewModel.onExportCsvClicked()
            showExportBottomSheet = false
        },
        onExportPdfClicked = {
            viewModel.onExportPdfClicked()
            showExportBottomSheet = false
        },
        showShareOptionsBottomSheet = showShareOptionsBottomSheet,
        onDismissShareOptionsBottomSheet = { showShareOptionsBottomSheet = false },
        onSharePdfRequested = {
            viewModel.onSharePdfRequested()
            showShareOptionsBottomSheet = false
        },
        onShareCsvRequested = {
            viewModel.onShareCsvRequested()
            showShareOptionsBottomSheet = false
        },
        onShareTextRequested = {
            viewModel.onShareTextRequested()
            showShareOptionsBottomSheet = false
        },
        onRetry = viewModel::loadReportData,
        onShowExportBottomSheet = { showExportBottomSheet = true },
        onShowShareOptionsBottomSheet = { showShareOptionsBottomSheet = true },
        useDummyDataForReport = viewModel.useDummyDataForReport
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseReportScreenContent(
    uiState: ExpenseReportUiState,
    formattedTotalForAllCategories: String,
    currencyFormatter: NumberFormat,
    showExportBottomSheet: Boolean,
    onDismissExportBottomSheet: () -> Unit,
    onExportCsvClicked: () -> Unit,
    onExportPdfClicked: () -> Unit,
    showShareOptionsBottomSheet: Boolean,
    onDismissShareOptionsBottomSheet: () -> Unit,
    onSharePdfRequested: () -> Unit,
    onShareCsvRequested: () -> Unit,
    onShareTextRequested: () -> Unit,
    onRetry: () -> Unit,
    onShowExportBottomSheet: () -> Unit,
    onShowShareOptionsBottomSheet: () -> Unit,
    useDummyDataForReport: Boolean,
    modifier: Modifier = Modifier
) {
    val exportSheetState = rememberModalBottomSheetState()
    val shareSheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Expense Report") },
                actions = {
                    IconButton(onClick = onShowExportBottomSheet) {
                        Icon(Icons.Default.Download, contentDescription = "Export Report")
                    }
                    IconButton(onClick = onShowShareOptionsBottomSheet) {
                        Icon(Icons.Default.Share, contentDescription = "Share Report Options")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> FullScreenLoadingIndicator()
                uiState.errorMessage != null -> ScreenErrorMessage(
                    message = uiState.errorMessage,
                    onRetry = onRetry
                ) // Removed !!
                uiState.expensesOverLast7Days.isEmpty() && !useDummyDataForReport -> EmptyStateView(
                    message = "No expenses recorded in the last 7 days to generate a report."
                )

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
                            val columnChartData = remember(uiState.dailyTotals) {
                                uiState.dailyTotals.map { dailyTotal ->
                                    Bars(
                                        label = dailyTotal.formattedDate,
                                        values = listOf(
                                            Bars.Data(
                                                label = "Expense",
                                                value = dailyTotal.totalAmount,
                                                color = Brush.verticalGradient(
                                                    listOf(
                                                        primaryColor,
                                                        primaryColorWithAlpha
                                                    )
                                                )
                                            )
                                        )
                                    )
                                }
                            }
                            DailyExpenseChartCard(columnChartData = columnChartData)
                        } else {
                            Text(
                                "Not enough data for chart.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        ReportSectionCard(title = "Category Totals (Total: $formattedTotalForAllCategories)") {
                            if (uiState.categoryTotals.isEmpty()) {
                                Text(
                                    "No expenses with categories found.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                uiState.categoryTotals.forEach { categoryTotal ->
                                    val categoryName =
                                        categoryTotal.category.name.replaceFirstChar {
                                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                        }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "$categoryName (${categoryTotal.percentage.roundToInt()}%)",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            currencyFormatter.format(categoryTotal.totalAmount),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { categoryTotal.percentage / 100f },
                                        color = categoryTotal.category.color,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }

            if (showExportBottomSheet) {
                ExportOptionsBottomSheet(
                    sheetState = exportSheetState,
                    // scope = scope, // Removed as unused
                    onDismiss = onDismissExportBottomSheet,
                    onExportCsvClicked = onExportCsvClicked,
                    onExportPdfClicked = onExportPdfClicked
                )
            }

            if (showShareOptionsBottomSheet) {
                ShareReportOptionsBottomSheet(
                    sheetState = shareSheetState,
                    // scope = scope, // Removed as unused
                    onDismiss = onDismissShareOptionsBottomSheet,
                    onSharePdfRequested = onSharePdfRequested,
                    onShareCsvRequested = onShareCsvRequested,
                    onShareTextRequested = onShareTextRequested
                )
            }
        }
    }
}


@Preview(showBackground = true, name = "Expense Report - Populated")
@Composable
fun ExpenseReportScreenPreview_Populated() {
    val sampleDailyTotals = listOf(
        DailyTotal(formattedDate = "Mon", totalAmount = 150.0, date = 10L),
        DailyTotal(formattedDate = "Tue", totalAmount = 200.0, date = 20L),
        DailyTotal(formattedDate = "Wed", totalAmount = 120.0, date = 30L)
    )
    val sampleCategoryTotals = listOf(
        CategoryTotal(CategoryType.FOOD, 300.0, 60f),
        CategoryTotal(CategoryType.TRAVEL, 170.0, 34f),
        CategoryTotal(CategoryType.STAFF, 30.0, 6f) // Assuming OTHER is valid
    )
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val sampleExpenses = emptyList<Expense>() // Corrected type

    MaterialTheme {
        ExpenseReportScreenContent(
            uiState = ExpenseReportUiState(
                isLoading = false,
                errorMessage = null,
                expensesOverLast7Days = sampleExpenses, // Use corrected list
                dailyTotals = sampleDailyTotals,
                categoryTotals = sampleCategoryTotals,
                totalForAllCategories = 500.0
            ),
            formattedTotalForAllCategories = currencyFormatter.format(500.0),
            currencyFormatter = currencyFormatter,
            showExportBottomSheet = false,
            onDismissExportBottomSheet = {},
            onExportCsvClicked = {},
            onExportPdfClicked = {},
            showShareOptionsBottomSheet = false,
            onDismissShareOptionsBottomSheet = {},
            onSharePdfRequested = {},
            onShareCsvRequested = {},
            onShareTextRequested = {},
            onRetry = {},
            onShowExportBottomSheet = {},
            onShowShareOptionsBottomSheet = {},
            useDummyDataForReport = false
        )
    }
}

@Preview(showBackground = true, name = "Expense Report - Empty")
@Composable
fun ExpenseReportScreenPreview_Empty() {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    MaterialTheme {
        ExpenseReportScreenContent(
            uiState = ExpenseReportUiState(
                isLoading = false,
                errorMessage = null,
                expensesOverLast7Days = emptyList(),
                dailyTotals = emptyList(),
                categoryTotals = emptyList(),
                totalForAllCategories = 0.0
            ),
            formattedTotalForAllCategories = currencyFormatter.format(0.0),
            currencyFormatter = currencyFormatter,
            showExportBottomSheet = false,
            onDismissExportBottomSheet = {},
            onExportCsvClicked = {},
            onExportPdfClicked = {},
            showShareOptionsBottomSheet = false,
            onDismissShareOptionsBottomSheet = {},
            onSharePdfRequested = {},
            onShareCsvRequested = {},
            onShareTextRequested = {},
            onRetry = {},
            onShowExportBottomSheet = {},
            onShowShareOptionsBottomSheet = {},
            useDummyDataForReport = false
        )
    }
}

@Preview(showBackground = true, name = "Expense Report - Loading")
@Composable
fun ExpenseReportScreenPreview_Loading() {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    MaterialTheme {
        ExpenseReportScreenContent(
            uiState = ExpenseReportUiState(isLoading = true),
            formattedTotalForAllCategories = currencyFormatter.format(0.0),
            currencyFormatter = currencyFormatter,
            showExportBottomSheet = false,
            onDismissExportBottomSheet = {},
            onExportCsvClicked = {},
            onExportPdfClicked = {},
            showShareOptionsBottomSheet = false,
            onDismissShareOptionsBottomSheet = {},
            onSharePdfRequested = {},
            onShareCsvRequested = {},
            onShareTextRequested = {},
            onRetry = {},
            onShowExportBottomSheet = {},
            onShowShareOptionsBottomSheet = {},
            useDummyDataForReport = false
        )
    }
}

@Preview(showBackground = true, name = "Expense Report - Error")
@Composable
fun ExpenseReportScreenPreview_Error() {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    MaterialTheme {
        ExpenseReportScreenContent(
            uiState = ExpenseReportUiState(errorMessage = "Failed to load report data. Please try again."),
            formattedTotalForAllCategories = currencyFormatter.format(0.0),
            currencyFormatter = currencyFormatter,
            showExportBottomSheet = false,
            onDismissExportBottomSheet = {},
            onExportCsvClicked = {},
            onExportPdfClicked = { },
            showShareOptionsBottomSheet = false,
            onDismissShareOptionsBottomSheet = {},
            onSharePdfRequested = {},
            onShareCsvRequested = {},
            onShareTextRequested = {},
            onRetry = {},
            onShowExportBottomSheet = {},
            onShowShareOptionsBottomSheet = {},
            useDummyDataForReport = false
        )
    }
}

@Preview(showBackground = true, name = "Expense Report - With Export Sheet")
@Composable
fun ExpenseReportScreenPreview_WithExportSheet() {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    MaterialTheme {
        ExpenseReportScreenContent(
            uiState = ExpenseReportUiState(
                dailyTotals = listOf(
                    DailyTotal(
                        formattedDate = "Mon",
                        totalAmount = 10.0,
                        date = 50L
                    )
                )
            ),
            formattedTotalForAllCategories = currencyFormatter.format(10.0),
            currencyFormatter = currencyFormatter,
            showExportBottomSheet = true,
            onDismissExportBottomSheet = {},
            onExportCsvClicked = {},
            onExportPdfClicked = {},
            showShareOptionsBottomSheet = false,
            onDismissShareOptionsBottomSheet = {},
            onSharePdfRequested = {},
            onShareCsvRequested = {},
            onShareTextRequested = {},
            onRetry = {},
            onShowExportBottomSheet = {},
            onShowShareOptionsBottomSheet = {},
            useDummyDataForReport = false
        )
    }
}
