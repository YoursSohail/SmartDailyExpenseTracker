package com.yourssohail.smartdailyexpensetracker.ui.report

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.ehsannarmani.compose_charts.models.Bars
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) { 
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                uiState.expensesOverLast7Days.isEmpty() && !viewModel.useDummyDataForReport -> {
                    Text(
                        text = "No expenses recorded in the last 7 days to generate a report.",
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
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
                                        color = categoryTotal.category.color, // Added category color here
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
    }
}
