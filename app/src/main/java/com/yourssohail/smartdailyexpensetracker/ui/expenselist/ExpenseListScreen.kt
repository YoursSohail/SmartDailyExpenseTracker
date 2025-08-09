package com.yourssohail.smartdailyexpensetracker.ui.expenselist

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
// Import for Expense model might still be needed if ExpenseListUiState is defined here or used directly
// import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
// No longer need EmptyStateView or ExpenseListItem directly if fully handled by ListContent
// Helper for currency formatting and date formatting used directly in this file
import java.text.NumberFormat
import java.text.SimpleDateFormat
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

    LaunchedEffect(Unit) { // For eventFlow collection - runs once
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ExpenseListEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) { // For ON_RESUME refresh
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Expenses") },
                actions = {
                    val datePickerDialog = rememberDatePickerDialog( // Now calls the internal fun
                        initialDateMillis = uiState.selectedDate,
                        onDateSelected = viewModel::onDateSelected
                    )
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }

                    var showGroupByMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showGroupByMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Group By")
                    }
                    DropdownMenu(
                        expanded = showGroupByMenu,
                        onDismissRequest = { showGroupByMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Group by Time") },
                            onClick = {
                                viewModel.setGroupBy(GroupByOption.TIME)
                                showGroupByMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Group by Category") },
                            onClick = {
                                viewModel.setGroupBy(GroupByOption.CATEGORY)
                                showGroupByMenu = false
                            }
                        )
                    }
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
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selectedDateFormatted = DATE_FORMAT_DISPLAY.format(Date(uiState.selectedDate))
                    Text(
                        text = "Total for: $selectedDateFormatted",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${currencyFormatter.format(uiState.totalSpentForSelectedDate ?: 0.0)} (${uiState.totalExpenseCountForSelectedDate})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            ListContent(
                uiState = uiState,
                onDeleteExpense = viewModel::deleteExpense,
                onNavigateToExpenseEdit = onNavigateToExpenseEdit
            )
        }
    }
}
