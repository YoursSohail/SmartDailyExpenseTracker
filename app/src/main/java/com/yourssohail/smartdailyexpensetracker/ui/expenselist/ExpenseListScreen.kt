package com.yourssohail.smartdailyexpensetracker.ui.expenselist

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense // Added import
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import java.text.NumberFormat
import java.text.SimpleDateFormat
// import java.util.Calendar // No longer directly needed here after rememberDatePickerDialog is self-contained
import java.util.Date
import java.util.Locale

private val DATE_FORMAT_DISPLAY = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExpenseListScreen(
    viewModel: ExpenseListViewModel = hiltViewModel(),
    onNavigateToExpenseEntry: () -> Unit,
    onNavigateToExpenseEdit: (expenseId: Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ExpenseListEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.refreshData() // Initial data load
    }

    val datePickerDialog = rememberDatePickerDialog(
        initialDateMillis = uiState.selectedDate,
        onDateSelected = viewModel::onDateSelected
    )

    val selectedDateFormatted = remember(uiState.selectedDate) {
        DATE_FORMAT_DISPLAY.format(Date(uiState.selectedDate))
    }
    val totalSpentFormatted = remember(uiState.totalSpentForSelectedDate) {
        currencyFormatter.format(uiState.totalSpentForSelectedDate ?: 0.0)
    }

    ExpenseListScreenContent(
        uiState = uiState,
        selectedDateFormatted = selectedDateFormatted,
        totalSpentFormatted = totalSpentFormatted,
        totalExpenseCount = uiState.totalExpenseCountForSelectedDate,
        onNavigateToExpenseEntry = onNavigateToExpenseEntry,
        onNavigateToExpenseEdit = onNavigateToExpenseEdit,
        onShowDatePicker = { datePickerDialog.show() },
        onGroupByChanged = viewModel::setGroupBy,
        onDeleteExpense = viewModel::deleteExpense,
        onRefreshData = viewModel::refreshData
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ExpenseListScreenContent(
    uiState: ExpenseListUiState,
    selectedDateFormatted: String,
    totalSpentFormatted: String,
    totalExpenseCount: Int,
    onNavigateToExpenseEntry: () -> Unit,
    onNavigateToExpenseEdit: (expenseId: Long) -> Unit,
    onShowDatePicker: () -> Unit,
    onGroupByChanged: (GroupByOption) -> Unit,
    onDeleteExpense: (Expense) -> Unit, // Changed from Long to Expense
    onRefreshData: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Daily Expenses") },
                actions = {
                    IconButton(onClick = onShowDatePicker) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                    CustomGroupToggleSwitch( // Assuming this is accessible from ExpenseListComponents.kt
                        currentOption = uiState.groupBy,
                        onOptionSelected = onGroupByChanged,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToExpenseEntry) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp // Or use Card for elevation
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDateFormatted,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$totalSpentFormatted ($totalExpenseCount)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            ListContent( // Assuming this is accessible from ExpenseListComponents.kt
                uiState = uiState,
                onDeleteExpense = onDeleteExpense, // Now expects (Expense) -> Unit
                onNavigateToExpenseEdit = onNavigateToExpenseEdit,
                onRefresh = onRefreshData
            )
        }
    }
}

@Preview(showBackground = true, name = "Expense List - Grouped by Category")
@Composable
fun ExpenseListScreenPreview_Category() {
    val currentTime = System.currentTimeMillis()
    val sampleExpenses = listOf(
        Expense(id = 1L, title = "Lunch", amount = 150.0, category = CategoryType.FOOD.name, date = currentTime, notes = "notes 1"),
        Expense(id = 2L, title = "Coffee", amount = 50.0, category = CategoryType.FOOD.name, date = currentTime - 10000, notes = "notes 2"),
        Expense(id = 3L, title = "Movie Ticket", amount = 300.0, category = CategoryType.TRAVEL.name, date = currentTime - 20000, notes = "notes 3")
    )
    val sampleGroupedExpenses: Map<CategoryType, List<Expense>> = mapOf(
        CategoryType.FOOD to sampleExpenses.filter { it.category == CategoryType.FOOD.name },
        CategoryType.TRAVEL to sampleExpenses.filter { it.category == CategoryType.TRAVEL.name }
    )
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    MaterialTheme { // Replace with your app's theme
        ExpenseListScreenContent(
            uiState = ExpenseListUiState(
                expenses = sampleExpenses,
                groupedExpenses = sampleGroupedExpenses,
                timeOfDayGroupedExpenses = emptyMap(), // Provide for completeness
                selectedDate = currentTime,
                totalSpentForSelectedDate = 500.0,
                totalExpenseCountForSelectedDate = 3,
                isLoading = false,
                errorMessage = null,
                groupBy = GroupByOption.CATEGORY
            ),
            selectedDateFormatted = DATE_FORMAT_DISPLAY.format(Date(currentTime)),
            totalSpentFormatted = currencyFormatter.format(500.0),
            totalExpenseCount = 3,
            onNavigateToExpenseEntry = {},
            onNavigateToExpenseEdit = {},
            onShowDatePicker = {},
            onGroupByChanged = {},
            onDeleteExpense = {},
            onRefreshData = {}
        )
    }
}

@Preview(showBackground = true, name = "Expense List - Grouped by Time")
@Composable
fun ExpenseListScreenPreview_Time() {
    val currentTime = System.currentTimeMillis()
    val sampleExpenses = listOf(
        Expense(id = 1L, title = "Morning Coffee", amount = 60.0, category = CategoryType.FOOD.name, date = currentTime - 1000*60*60*3, notes = "notes"),
        Expense(id = 2L, title = "Lunch", amount = 250.0, category = CategoryType.FOOD.name, date = currentTime - 1000*60*30, notes = "notes"),
        Expense(id = 3L, title = "Snacks", amount = 100.0, category = CategoryType.FOOD.name, date = currentTime, notes = "notes")
    )
    // Simplified time grouping for preview - actual ViewModel logic is more robust
    val sampleTimeOfDayGroupedExpenses: Map<TimeOfDay, List<Expense>> = mapOf(
        TimeOfDay.MORNING to sampleExpenses.filter { val hour = java.util.Calendar.getInstance().apply{timeInMillis = it.date}.get(java.util.Calendar.HOUR_OF_DAY); hour < 12 },
        TimeOfDay.AFTERNOON to sampleExpenses.filter { val hour = java.util.Calendar.getInstance().apply{timeInMillis = it.date}.get(java.util.Calendar.HOUR_OF_DAY); hour in 12..16 },
        TimeOfDay.EVENING to sampleExpenses.filter { val hour = java.util.Calendar.getInstance().apply{timeInMillis = it.date}.get(java.util.Calendar.HOUR_OF_DAY); hour > 16 }
    ).filterValues { it.isNotEmpty() }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    MaterialTheme {
         ExpenseListScreenContent(
            uiState = ExpenseListUiState(
                expenses = sampleExpenses,
                groupedExpenses = emptyMap(), // Provide for completeness
                timeOfDayGroupedExpenses = sampleTimeOfDayGroupedExpenses,
                selectedDate = currentTime,
                totalSpentForSelectedDate = 410.0,
                totalExpenseCountForSelectedDate = 3,
                isLoading = false,
                errorMessage = null,
                groupBy = GroupByOption.TIME
            ),
            selectedDateFormatted = DATE_FORMAT_DISPLAY.format(Date(currentTime)),
            totalSpentFormatted = currencyFormatter.format(410.0),
            totalExpenseCount = 3,
            onNavigateToExpenseEntry = {},
            onNavigateToExpenseEdit = {},
            onShowDatePicker = {},
            onGroupByChanged = {},
            onDeleteExpense = {},
            onRefreshData = {}
        )
    }
}

@Preview(showBackground = true, name = "Expense List - Empty State")
@Composable
fun ExpenseListScreenPreview_Empty() {
    val currentTime = System.currentTimeMillis()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    MaterialTheme {
         ExpenseListScreenContent(
            uiState = ExpenseListUiState(
                expenses = emptyList(),
                groupedExpenses = emptyMap(),
                timeOfDayGroupedExpenses = emptyMap(),
                selectedDate = currentTime,
                totalSpentForSelectedDate = 0.0,
                totalExpenseCountForSelectedDate = 0,
                isLoading = false,
                errorMessage = null,
                groupBy = GroupByOption.CATEGORY
            ),
            selectedDateFormatted = DATE_FORMAT_DISPLAY.format(Date(currentTime)),
            totalSpentFormatted = currencyFormatter.format(0.0),
            totalExpenseCount = 0,
            onNavigateToExpenseEntry = {},
            onNavigateToExpenseEdit = {},
            onShowDatePicker = {},
            onGroupByChanged = {},
            onDeleteExpense = {},
            onRefreshData = {}
        )
    }
}

// NOTE: For these previews to work fully, the actual 'ListContent' and 'CustomGroupToggleSwitch'
// composables (likely from ExpenseListComponents.kt) need to be accessible 
// and correctly interpret the updated ExpenseListUiState (using List<Expense>).
// You may need to import them if they are not in this package.
// Also ensure Expense, GroupByOption, TimeOfDay are imported or accessible.
