package com.yourssohail.smartdailyexpensetracker.ui.expenselist

// import androidx.compose.animation.fadeIn // Removed
// import androidx.compose.animation.slideInHorizontally // Removed
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
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
import com.yourssohail.smartdailyexpensetracker.ui.components.EmptyStateView
import com.yourssohail.smartdailyexpensetracker.ui.components.ExpenseListItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExpenseListScreen(
    viewModel: ExpenseListViewModel = hiltViewModel(),
    onNavigateToExpenseEntry: () -> Unit,
    onNavigateToExpenseEdit: (expenseId: Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
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
                    val calendar =
                        Calendar.getInstance().apply { timeInMillis = uiState.selectedDate }
                    val datePickerDialog = DatePickerDialog(
                        context,
                        { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                            val newCal = Calendar.getInstance().apply {
                                set(selectedYear, selectedMonth, selectedDayOfMonth)
                            }
                            viewModel.onDateSelected(newCal.timeInMillis)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
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
                                if (uiState.groupBy != GroupByOption.TIME) viewModel.onToggleGroupBy()
                                showGroupByMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Group by Category") },
                            onClick = {
                                if (uiState.groupBy != GroupByOption.CATEGORY) viewModel.onToggleGroupBy()
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
                    val selectedDateFormatted =
                        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(
                            Date(uiState.selectedDate)
                        )
                    Text(
                        text = "Total for: $selectedDateFormatted",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "₹${
                            String.format(
                                "%.2f",
                                uiState.totalSpentForSelectedDate ?: 0.0
                            )
                        } (${uiState.totalExpenseCountForSelectedDate})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (uiState.expenses.isEmpty() && uiState.groupBy == GroupByOption.TIME) {
                EmptyStateView(
                    message = "No expenses recorded for ${
                        SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.getDefault()
                        ).format(Date(uiState.selectedDate))
                    }.\nTap the '+' button to add one!"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    if (uiState.groupBy == GroupByOption.TIME) {
                        items(uiState.expenses, key = { it.id }) { expense ->
                            var isVisible by remember { mutableStateOf(false) }
                            // Trigger item animation on screen start with a small stagger
                            LaunchedEffect(Unit) {
                                // Optional stagger effect so each item comes in one after another
                                val index = uiState.expenses.indexOf(expense)
                                kotlinx.coroutines.delay(index * 100L) // 100ms delay between items
                                isVisible = true
                            }
                            AnimatedVisibility(
                                visible = isVisible, // you can control this based on state
                                enter = slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth }, // start off-screen right
                                    animationSpec = tween(durationMillis = 300)
                                ),
                                exit = slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth }, // slide off to right
                                    animationSpec = tween(durationMillis = 300)
                                )
                            ) {
                                ExpenseListItem(
                                    expense = expense,
                                    onDeleteClick = {
                                        isVisible = true
                                        viewModel.deleteExpense(it)
                                    },
                                    onEditClick = { onNavigateToExpenseEdit(expense.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else { // GroupByOption.CATEGORY
                        if (uiState.groupedExpenses.isEmpty() && uiState.expenses.isNotEmpty()) {
                            item {
                                Text(
                                    "Could not group expenses by category. Displaying as list:",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            items(uiState.expenses, key = { "fallback-${it.id}" }) { expense ->
                                var isVisible by remember { mutableStateOf(false) }
                                // Trigger item animation on screen start with a small stagger
                                LaunchedEffect(Unit) {
                                    // Optional stagger effect so each item comes in one after another
                                    val index = uiState.expenses.indexOf(expense)
                                    kotlinx.coroutines.delay(index * 100L) // 100ms delay between items
                                    isVisible = true
                                }
                                AnimatedVisibility(
                                    visible = isVisible, // you can control this based on state
                                    enter = slideInHorizontally(
                                        initialOffsetX = { fullWidth -> fullWidth }, // start off-screen right
                                        animationSpec = tween(durationMillis = 300)
                                    ),
                                    exit = slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> fullWidth }, // slide off to right
                                        animationSpec = tween(durationMillis = 300)
                                    )
                                ) {
                                    ExpenseListItem(
                                        expense = expense,
                                        onDeleteClick = {
                                            isVisible = true
                                            viewModel.deleteExpense(it)
                                        },
                                        onEditClick = { onNavigateToExpenseEdit(expense.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                            }
                        } else if (uiState.groupedExpenses.isEmpty() && uiState.expenses.isEmpty()) {
                            item {
                                EmptyStateView(
                                    message = "No expenses to group by category for ${
                                        SimpleDateFormat(
                                            "dd MMM yyyy",
                                            Locale.getDefault()
                                        ).format(Date(uiState.selectedDate))
                                    }."
                                )
                            }
                        } else {
                            uiState.groupedExpenses.forEach { (category, expensesInCategory) ->
                                stickyHeader {
                                    Surface(
                                        modifier = Modifier.fillParentMaxWidth(),
                                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                                        shadowElevation = 1.dp
                                    ) {
                                        Column {
                                            Text(
                                                text = category.name.replaceFirstChar { char ->
                                                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp, vertical = 10.dp)
                                            )
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                    alpha = 0.4f
                                                )
                                            )
                                        }
                                    }
                                }
                                items(expensesInCategory, key = { it.id }) { expense ->
                                    var isVisible by remember { mutableStateOf(false) }
                                    // Trigger item animation on screen start with a small stagger
                                    LaunchedEffect(Unit) {
                                        // Optional stagger effect so each item comes in one after another
                                        val index = uiState.expenses.indexOf(expense)
                                        kotlinx.coroutines.delay(index * 100L) // 100ms delay between items
                                        isVisible = true
                                    }
                                    AnimatedVisibility(
                                        visible = isVisible, // you can control this based on state
                                        enter = slideInHorizontally(
                                            initialOffsetX = { fullWidth -> fullWidth }, // start off-screen right
                                            animationSpec = tween(durationMillis = 300)
                                        ),
                                        exit = slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> fullWidth }, // slide off to right
                                            animationSpec = tween(durationMillis = 300)
                                        )
                                    ) {
                                        ExpenseListItem(
                                            expense = expense,
                                            onDeleteClick = {
                                                isVisible = true
                                                viewModel.deleteExpense(it)
                                            },
                                            onEditClick = { onNavigateToExpenseEdit(expense.id) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
