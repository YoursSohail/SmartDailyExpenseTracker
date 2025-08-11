package com.yourssohail.smartdailyexpensetracker.ui.expenselist

import com.yourssohail.smartdailyexpensetracker.domain.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType

/**
 * Defines the available options for grouping expenses in the list.
 */
enum class GroupByOption {
    /** Groups expenses by the time of day they occurred (Morning, Afternoon, Evening). */
    TIME,
    /** Groups expenses by their assigned category. */
    CATEGORY
}

/**
 * Represents different parts of the day for grouping expenses.
 */
enum class TimeOfDay {
    /** Represents the morning period (e.g., 12 AM - 11:59 AM). */
    MORNING,
    /** Represents the afternoon period (e.g., 12 PM - 4:59 PM). */
    AFTERNOON,
    /** Represents the evening period (e.g., 5 PM - 11:59 PM). */
    EVENING
}

/**
 * Represents the UI state for the Expense List screen.
 *
 * @property expenses The list of expenses for the currently selected date.
 * @property selectedDate The currently selected date in milliseconds.
 * @property totalSpentForSelectedDate The total amount spent on the selected date. Null if no expenses.
 * @property totalExpenseCountForSelectedDate The total number of expenses recorded for the selected date.
 * @property isLoading True if data is currently being loaded, false otherwise.
 * @property groupBy The current option selected for grouping expenses.
 * @property errorMessage An optional error message to be displayed to the user.
 * @property groupedExpenses A map of expenses grouped by [CategoryType], used when [groupBy] is [GroupByOption.CATEGORY].
 * @property timeOfDayGroupedExpenses A map of expenses grouped by [TimeOfDay], used when [groupBy] is [GroupByOption.TIME].
 */
data class ExpenseListUiState(
    val expenses: List<Expense> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val totalSpentForSelectedDate: Double? = null,
    val totalExpenseCountForSelectedDate: Int = 0,
    val isLoading: Boolean = true,
    val groupBy: GroupByOption = GroupByOption.TIME,
    val errorMessage: String? = null,
    val groupedExpenses: Map<CategoryType, List<Expense>> = emptyMap(),
    val timeOfDayGroupedExpenses: Map<TimeOfDay, List<Expense>> = emptyMap()
)

/**
 * Represents one-time events that can be emitted from the [ExpenseListViewModel] to the UI.
 */
sealed class ExpenseListEvent {
    /**
     * Event to request showing a toast message.
     * @param message The message to be displayed in the toast.
     */
    data class ShowToast(val message: String) : ExpenseListEvent()
}
