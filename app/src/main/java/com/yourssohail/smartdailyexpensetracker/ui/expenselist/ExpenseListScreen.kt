package com.yourssohail.smartdailyexpensetracker.ui.expenselist

// Imports to be added/adjusted:
// Removed: animateDpAsState, background, border, Box, offset, size, CircleShape, clip, Dp
// Retained: Category and Schedule icons for now, in case they are used elsewhere,
// but they are prime candidates for removal if CustomGroupToggleSwitch is self-contained.

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
// import androidx.compose.foundation.background // Removed
// import androidx.compose.foundation.border // Removed
import androidx.compose.foundation.layout.Arrangement
// import androidx.compose.foundation.layout.Box // Removed
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
// import androidx.compose.foundation.layout.height // Potentially removable if not used by other elements
import androidx.compose.foundation.layout.padding
// import androidx.compose.foundation.layout.size // Potentially removable
import androidx.compose.foundation.layout.width
// import androidx.compose.foundation.shape.CircleShape // Removed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
// import androidx.compose.material.icons.filled.Category // Kept for now, verify usage
// import androidx.compose.material.icons.filled.Schedule // Kept for now, verify usage
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
// import androidx.compose.ui.draw.clip // Removed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.unit.Dp // Removed
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
        viewModel.refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Expenses") },
                actions = {
                    val datePickerDialog = rememberDatePickerDialog(
                        initialDateMillis = uiState.selectedDate,
                        onDateSelected = viewModel::onDateSelected
                    )
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }

                    // Use the updated CustomGroupToggleSwitch from ExpenseListComponents.kt
                    CustomGroupToggleSwitch(
                        currentOption = uiState.groupBy,
                        onOptionSelected = viewModel::setGroupBy,
                        modifier = Modifier.padding(horizontal = 8.dp) 
                    )
                    Spacer(modifier = Modifier.width(4.dp)) // Maintain spacing if needed
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
