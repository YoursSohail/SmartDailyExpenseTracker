package com.yourssohail.smartdailyexpensetracker.ui.expenselist

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.ui.components.EmptyStateView
import com.yourssohail.smartdailyexpensetracker.ui.components.ExpenseListItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal val DATE_FORMAT_SHORT_COMPONENTS = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())


@Composable
internal fun rememberDatePickerDialog(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit
): DatePickerDialog {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    calendar.timeInMillis = initialDateMillis

    return remember(context, initialDateMillis) {
        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDayOfMonth)
                }
                onDateSelected(newCal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply { // Apply the maxDate modification here
            datePicker.maxDate = System.currentTimeMillis()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AnimatedExpenseListItem(
    expense: Expense,
    index: Int,
    onDeleteClick: (Expense) -> Unit,
    onEditClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    // Use a key that includes the index for LaunchedEffect if items can be reordered
    // and you want the animation to re-trigger based on position changes.
    // For simple appearance, expense.id is often sufficient.
    LaunchedEffect(key1 = expense.id, key2 = index) {
        kotlinx.coroutines.delay(index * 100L) // Stagger
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 300)
        ),
        modifier = modifier
    ) {
        ExpenseListItem(
            expense = expense,
            onDeleteClick = { onDeleteClick(expense) },
            onEditClick = { onEditClick(expense.id) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ListContent(
    uiState: ExpenseListUiState,
    onDeleteExpense: (Expense) -> Unit, // Changed from ViewModel to direct lambda
    onNavigateToExpenseEdit: (expenseId: Long) -> Unit
) {
    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        uiState.expenses.isEmpty() && uiState.groupBy == GroupByOption.TIME && !uiState.isLoading -> {
            EmptyStateView(
                message = "No expenses recorded for ${DATE_FORMAT_SHORT_COMPONENTS.format(Date(uiState.selectedDate))}.\nTap the '+' button to add one!"
            )
        }
        uiState.expenses.isEmpty() && uiState.groupBy == GroupByOption.CATEGORY && !uiState.isLoading -> {
            EmptyStateView(
                message = "No expenses to group by category for ${DATE_FORMAT_SHORT_COMPONENTS.format(Date(uiState.selectedDate))}.\nTap the '+' button to add one!"
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp) // Consider FAB space
            ) {
                if (uiState.groupBy == GroupByOption.TIME) {
                    itemsIndexed(uiState.expenses, key = { _, expense -> expense.id }) { index, expense ->
                        AnimatedExpenseListItem(
                            expense = expense,
                            index = index,
                            onDeleteClick = onDeleteExpense,
                            onEditClick = onNavigateToExpenseEdit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else { // GroupByOption.CATEGORY
                    if (uiState.groupedExpenses.isEmpty() && uiState.expenses.isNotEmpty()) {
                        item {
                            Text(
                                "Could not group expenses. Displaying as list:",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        itemsIndexed(uiState.expenses, key = { _, expense -> "fallback-${expense.id}" }) { index, expense ->
                            AnimatedExpenseListItem(
                                expense = expense,
                                index = index,
                                onDeleteClick = onDeleteExpense,
                                onEditClick = onNavigateToExpenseEdit,
                                modifier = Modifier.fillMaxWidth()
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
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp)
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }
                            }
                            itemsIndexed(expensesInCategory, key = { _, expense -> expense.id }) { index, expense ->
                                AnimatedExpenseListItem(
                                    expense = expense,
                                    index = index,
                                    onDeleteClick = onDeleteExpense,
                                    onEditClick = onNavigateToExpenseEdit,
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

// --- Previews ---

@Preview(showBackground = true, name = "Animated Expense Item")
@Composable
fun AnimatedExpenseListItemPreview() {
    // SmartDailyExpenseTrackerTheme { // Assuming a theme
        AnimatedExpenseListItem(
            expense = Expense(id = 1, title = "Lunch Preview", amount = 12.75, category = CategoryType.FOOD.name, date = System.currentTimeMillis(), notes = "With colleagues"),
            index = 0,
            onDeleteClick = { /* No-op for preview */ },
            onEditClick = { /* No-op for preview */ }
        )
    // }
}

@Preview(showBackground = true, name = "ListContent - Loading")
@Composable
fun ListContentLoadingPreview() {
    // SmartDailyExpenseTrackerTheme {
        ListContent(
            uiState = ExpenseListUiState(isLoading = true),
            onDeleteExpense = {},
            onNavigateToExpenseEdit = {}
        )
    // }
}

@Preview(showBackground = true, name = "ListContent - Empty Time Group")
@Composable
fun ListContentEmptyTimePreview() {
    // SmartDailyExpenseTrackerTheme {
        ListContent(
            uiState = ExpenseListUiState(
                expenses = emptyList(),
                groupedExpenses = emptyMap(),
                groupBy = GroupByOption.TIME,
                isLoading = false,
                selectedDate = System.currentTimeMillis()
            ),
            onDeleteExpense = {},
            onNavigateToExpenseEdit = {}
        )
    // }
}

@Preview(showBackground = true, name = "ListContent - Empty Category Group")
@Composable
fun ListContentEmptyCategoryPreview() {
    // SmartDailyExpenseTrackerTheme {
        ListContent(
            uiState = ExpenseListUiState(
                expenses = emptyList(),
                groupedExpenses = emptyMap(),
                groupBy = GroupByOption.CATEGORY,
                isLoading = false,
                selectedDate = System.currentTimeMillis()
            ),
            onDeleteExpense = {},
            onNavigateToExpenseEdit = {}
        )
    // }
}

@Preview(showBackground = true, name = "ListContent - With Data (Time)")
@Composable
fun ListContentWithDataTimePreview() {
    val sampleExpenses = listOf(
        Expense(id = 1, title = "Groceries", amount = 55.20, category = CategoryType.FOOD.name, date = System.currentTimeMillis() - 100000, notes = "Weekly shopping"),
        Expense(id = 2, title = "Coffee", amount = 4.50, category = CategoryType.FOOD.name, date = System.currentTimeMillis(), notes = "Morning booster"),
        Expense(id = 3, title = "Movie Ticket", amount = 15.00, category = CategoryType.UTILITY.name, date = System.currentTimeMillis() - 200000)
    )
    // SmartDailyExpenseTrackerTheme {
        ListContent(
            uiState = ExpenseListUiState(
                expenses = sampleExpenses,
                groupBy = GroupByOption.TIME,
                isLoading = false,
                totalExpenseCountForSelectedDate = sampleExpenses.size,
                totalSpentForSelectedDate = sampleExpenses.sumOf { it.amount }
            ),
            onDeleteExpense = {},
            onNavigateToExpenseEdit = {}
        )
    // }
}

@Preview(showBackground = true, name = "ListContent - With Data (Category)")
@Composable
fun ListContentWithDataCategoryPreview() {
    val sampleExpenses = listOf(
        Expense(id = 1, title = "Pizza", amount = 20.0, category = CategoryType.FOOD.name, date = System.currentTimeMillis()),
        Expense(id = 2, title = "Concert", amount = 75.0, category = CategoryType.TRAVEL.name, date = System.currentTimeMillis()),
        Expense(id = 3, title = "Burger", amount = 10.0, category = CategoryType.STAFF.name, date = System.currentTimeMillis()),
        Expense(id = 4, title = "Fuel", amount = 50.0, category = CategoryType.UTILITY.name, date = System.currentTimeMillis())
    )
    val grouped = sampleExpenses.groupBy { CategoryType.valueOf(it.category.uppercase()) }

    // SmartDailyExpenseTrackerTheme {
        ListContent(
            uiState = ExpenseListUiState(
                expenses = sampleExpenses, // Still provide original expenses for fallback/count
                groupedExpenses = grouped,
                groupBy = GroupByOption.CATEGORY,
                isLoading = false,
                totalExpenseCountForSelectedDate = sampleExpenses.size,
                totalSpentForSelectedDate = sampleExpenses.sumOf { it.amount }
            ),
            onDeleteExpense = {},
            onNavigateToExpenseEdit = {}
        )
    // }
}

@Preview(showBackground = true, name = "ListContent - Error")
@Composable
fun ListContentErrorPreview() {
    // SmartDailyExpenseTrackerTheme {
        ListContent(
            uiState = ExpenseListUiState(isLoading = false, errorMessage = "Failed to load expenses. Please try again."),
            onDeleteExpense = {},
            onNavigateToExpenseEdit = {}
        )
    // }
}

