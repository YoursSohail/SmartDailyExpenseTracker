package com.yourssohail.smartdailyexpensetracker.ui.expenselist

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.ui.components.EmptyStateView
import com.yourssohail.smartdailyexpensetracker.ui.components.ExpenseListItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.EnumMap
import java.util.Locale

internal val DATE_FORMAT_SHORT_COMPONENTS = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

@Composable
private fun getTimeOfDayHeaderPresentation(timeOfDay: TimeOfDay): Pair<String, String> {
    return when (timeOfDay) {
        TimeOfDay.MORNING -> "🌅" to "Morning"
        TimeOfDay.AFTERNOON -> "☀️" to "Afternoon"
        TimeOfDay.EVENING -> "🌙" to "Evening"
    }
}

@Composable
private fun getCategoryHeaderPresentation(category: CategoryType): Pair<ImageVector, Color> {
    val icon = Icons.AutoMirrored.Filled.Label
    val color = when (category) {
        CategoryType.FOOD -> Color(0xFF4CAF50)
        CategoryType.TRAVEL -> Color(0xFF2196F3)
        CategoryType.UTILITY -> Color(0xFF9C27B0)
        CategoryType.STAFF -> Color(0xFFFF9800)
    }
    return icon to color
}


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
        ).apply {
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
    LaunchedEffect(key1 = expense.id, key2 = index) {
        kotlinx.coroutines.delay(index * 70L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth / 2 },
            animationSpec = tween(durationMillis = 350)
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
    onDeleteExpense: (Expense) -> Unit,
    onNavigateToExpenseEdit: (expenseId: Long) -> Unit
) {
    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.errorMessage != null -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        uiState.expenses.isEmpty() && !uiState.isLoading -> {
            EmptyStateView(
                message = "No expenses recorded for ${DATE_FORMAT_SHORT_COMPONENTS.format(Date(uiState.selectedDate))}.\nTap the '+' button to add one!"
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                if (uiState.groupBy == GroupByOption.TIME) {
                    if (uiState.timeOfDayGroupedExpenses.values.all { it.isEmpty() } && uiState.expenses.isNotEmpty() && !uiState.isLoading) {
                         item {
                            Text(
                                "Could not group expenses by time of day. Displaying as list:",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                        itemsIndexed(uiState.expenses, key = { _, expense -> "fallback-time-${expense.id}" }) { index, expense ->
                            AnimatedExpenseListItem(expense, index, onDeleteExpense, onNavigateToExpenseEdit)
                        }
                    } else {
                        uiState.timeOfDayGroupedExpenses.forEach { (timeOfDay, expensesInTimeOfDay) ->
                             if (expensesInTimeOfDay.isNotEmpty()) {
                                item { 
                                    Surface(
                                        modifier = Modifier.fillParentMaxWidth(),
                                        color = Color.Transparent,
                                        shadowElevation = 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val (emoji, name) = getTimeOfDayHeaderPresentation(timeOfDay)
                                            Text(
                                                text = emoji,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                itemsIndexed(expensesInTimeOfDay, key = { _, expense -> expense.id }) { index, expense ->
                                    AnimatedExpenseListItem(expense, index, onDeleteExpense, onNavigateToExpenseEdit, Modifier.padding(top = 2.dp, bottom = 2.dp))
                                }
                            }
                        }
                    }
                } else { // GroupByOption.CATEGORY
                    if (uiState.groupedExpenses.isEmpty() && uiState.expenses.isNotEmpty() && !uiState.isLoading) {
                         item {
                            Text(
                                "Could not group expenses by category. Displaying as list:",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                        itemsIndexed(uiState.expenses, key = { _, expense -> "fallback-category-${expense.id}" }) { index, expense ->
                            AnimatedExpenseListItem(expense, index, onDeleteExpense, onNavigateToExpenseEdit)
                        }
                    } else {
                        uiState.groupedExpenses.forEach { (category, expensesInCategory) ->
                             if (expensesInCategory.isNotEmpty()) {
                                item { 
                                    Surface(
                                        modifier = Modifier.fillParentMaxWidth(),
                                        color = Color.Transparent,
                                        shadowElevation = 0.dp
                                    ) {
                                        val (iconVector, iconTint) = getCategoryHeaderPresentation(category)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = iconVector,
                                                contentDescription = category.name,
                                                modifier = Modifier.size(20.dp),
                                                tint = iconTint
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = category.name.replaceFirstChar { nameChar ->
                                                    if (nameChar.isLowerCase()) nameChar.titlecase(Locale.getDefault()) else nameChar.toString()
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = iconTint
                                            )
                                        }
                                    }
                                }
                                itemsIndexed(expensesInCategory, key = { _, expense -> expense.id }) { index, expense ->
                                    AnimatedExpenseListItem(expense, index, onDeleteExpense, onNavigateToExpenseEdit, Modifier.padding(top = 2.dp, bottom = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomGroupToggleSwitch(
    currentOption: GroupByOption,
    onOptionSelected: (GroupByOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackHeight = 32.dp
    val trackWidth = 64.dp
    val thumbDiameter = trackHeight
    val padding = 6.dp // Padding for thumb edge spacing and icon internal padding

    val isGroupedByTime = currentOption == GroupByOption.TIME

    val thumbOffset: Dp by animateDpAsState(
        targetValue = if (isGroupedByTime) padding else trackWidth - thumbDiameter + padding,
        label = "thumbOffset"
    )

    Box( // Track
        modifier = modifier // Apply the passed modifier here
            .width(trackWidth + padding * 2)
            .height(trackHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable {
                onOptionSelected(
                    if (isGroupedByTime) GroupByOption.CATEGORY else GroupByOption.TIME
                )
            }
            .padding(horizontal = padding), // Apply padding for icons within the track
        contentAlignment = Alignment.CenterStart // Align thumb to start initially
    ) {
        // Inactive Icons on Track
        Row(
            modifier = Modifier.matchParentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // Distributes icons
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = "Group by Time",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isGroupedByTime) 0f else 0.7f), // Hide if active
                modifier = Modifier.size(thumbDiameter - padding * 2)
            )
            Icon(
                imageVector = Icons.Filled.Category,
                contentDescription = "Group by Category",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (!isGroupedByTime) 0f else 0.7f), // Hide if active
                modifier = Modifier.size(thumbDiameter - padding * 2)
            )
        }

        // Thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset - padding) // Adjust offset for padding
                .size(thumbDiameter)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGroupedByTime) Icons.Filled.Schedule else Icons.Filled.Category,
                contentDescription = if (isGroupedByTime) "Currently grouping by Time" else "Currently grouping by Category",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(thumbDiameter - padding * 2) // Icon smaller than thumb
            )
        }
    }
}

// --- Previews ---

@Preview(showBackground = true, name = "CustomGroupToggleSwitch - Time Selected")
@Composable
fun CustomGroupToggleSwitchTimeSelectedPreview() {
    MaterialTheme { // Wrap in MaterialTheme for previews to get proper styling
        var selectedOption by remember { mutableStateOf(GroupByOption.TIME) }
        CustomGroupToggleSwitch(
            currentOption = selectedOption,
            onOptionSelected = { selectedOption = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "CustomGroupToggleSwitch - Category Selected")
@Composable
fun CustomGroupToggleSwitchCategorySelectedPreview() {
    MaterialTheme {
        var selectedOption by remember { mutableStateOf(GroupByOption.CATEGORY) }
        CustomGroupToggleSwitch(
            currentOption = selectedOption,
            onOptionSelected = { selectedOption = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}


@Preview(showBackground = true, name = "Animated Expense Item")
@Composable
fun AnimatedExpenseListItemPreview() {
    AnimatedExpenseListItem(
        expense = Expense(id = 1, title = "Lunch Preview", amount = 12.75, category = CategoryType.FOOD.name, date = System.currentTimeMillis(), notes = "With colleagues"),
        index = 0, onDeleteClick = {}, onEditClick = {}
    )
}

@Preview(showBackground = true, name = "ListContent - Loading")
@Composable
fun ListContentLoadingPreview() {
    ListContent(uiState = ExpenseListUiState(isLoading = true), onDeleteExpense = {}, onNavigateToExpenseEdit = {})
}

@Preview(showBackground = true, name = "ListContent - Empty (Time Group)")
@Composable
fun ListContentEmptyTimePreview() {
    ListContent(
        uiState = ExpenseListUiState(
            expenses = emptyList(),
            timeOfDayGroupedExpenses = TimeOfDay.values().associateWith { emptyList<Expense>() },
            groupBy = GroupByOption.TIME,
            isLoading = false,
            selectedDate = System.currentTimeMillis()
        ),
        onDeleteExpense = {}, onNavigateToExpenseEdit = {}
    )
}

@Preview(showBackground = true, name = "ListContent - Empty (Category Group)")
@Composable
fun ListContentEmptyCategoryPreview() {
    ListContent(
        uiState = ExpenseListUiState(
            expenses = emptyList(),
            groupedExpenses = emptyMap(),
            groupBy = GroupByOption.CATEGORY,
            isLoading = false,
            selectedDate = System.currentTimeMillis()
        ),
        onDeleteExpense = {}, onNavigateToExpenseEdit = {}
    )
}

@Preview(showBackground = true, name = "ListContent - Data (Time of Day)")
@Composable
fun ListContentWithDataTimePreview() {
    val cal = Calendar.getInstance()
    val now = System.currentTimeMillis()

    val morningExpense = Expense(id = 1, title = "Coffee", amount = 4.50, category = CategoryType.FOOD.name, date = cal.apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 9) }.timeInMillis)
    val afternoonExpense = Expense(id = 2, title = "Lunch", amount = 12.50, category = CategoryType.FOOD.name, date = cal.apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 13) }.timeInMillis)
    val eveningExpense = Expense(id = 3, title = "Dinner", amount = 22.00, category = CategoryType.FOOD.name, date = cal.apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 19) }.timeInMillis)
    val anotherMorning = Expense(id = 4, title = "Breakfast Burrito", amount = 7.00, category = CategoryType.FOOD.name, date = cal.apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 8) }.timeInMillis)


    val allExpenses = listOf(morningExpense, afternoonExpense, eveningExpense, anotherMorning)
    val timeGrouped = EnumMap<TimeOfDay, List<Expense>>(TimeOfDay::class.java).apply {
        put(TimeOfDay.MORNING, listOf(morningExpense, anotherMorning))
        put(TimeOfDay.AFTERNOON, listOf(afternoonExpense))
        put(TimeOfDay.EVENING, listOf(eveningExpense))
    }

    ListContent(
        uiState = ExpenseListUiState(
            expenses = allExpenses,
            timeOfDayGroupedExpenses = timeGrouped,
            groupBy = GroupByOption.TIME,
            isLoading = false,
            totalExpenseCountForSelectedDate = allExpenses.size,
            totalSpentForSelectedDate = allExpenses.sumOf { it.amount }
        ),
        onDeleteExpense = {}, onNavigateToExpenseEdit = {}
    )
}

@Preview(showBackground = true, name = "ListContent - Data (Category)")
@Composable
fun ListContentWithDataCategoryPreview() {
    val cal = Calendar.getInstance()
    val now = System.currentTimeMillis()
    val expensesList = listOf(
        Expense(id = 1, title = "Pizza", amount = 20.0, category = CategoryType.FOOD.name, date = now),
        Expense(id = 2, title = "Bus Ticket", amount = 2.50, category = CategoryType.TRAVEL.name, date = now - 10000),
        Expense(id = 3, title = "Pens", amount = 5.0, category = CategoryType.STAFF.name, date = now - 20000),
        Expense(id = 4, title = "Electricity Bill", amount = 50.0, category = CategoryType.UTILITY.name, date = now - 30000),
        Expense(id = 5, title = "Groceries", amount = 30.0, category = CategoryType.FOOD.name, date = now - 40000)
    )
    val categoryGrouped = expensesList.groupBy { exp -> CategoryType.valueOf(exp.category.uppercase()) }
        .mapValues { entry -> entry.value.sortedByDescending { it.date } }

    ListContent(
        uiState = ExpenseListUiState(
            expenses = expensesList,
            groupedExpenses = categoryGrouped,
            groupBy = GroupByOption.CATEGORY,
            isLoading = false,
            totalExpenseCountForSelectedDate = expensesList.size,
            totalSpentForSelectedDate = expensesList.sumOf { it.amount }
        ),
        onDeleteExpense = {}, onNavigateToExpenseEdit = {}
    )
}

@Preview(showBackground = true, name = "ListContent - Error")
@Composable
fun ListContentErrorPreview() {
    ListContent(
        uiState = ExpenseListUiState(isLoading = false, errorMessage = "Failed to load expenses. Please try again."),
        onDeleteExpense = {}, onNavigateToExpenseEdit = {}
    )
}

