package com.yourssohail.smartdailyexpensetracker.ui.expenselist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssohail.smartdailyexpensetracker.domain.model.Expense 
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.domain.usecase.DeleteExpenseUseCase
import com.yourssohail.smartdailyexpensetracker.domain.usecase.GetDailyTotalUseCase
import com.yourssohail.smartdailyexpensetracker.domain.usecase.GetExpensesByDateRangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.EnumMap
import javax.inject.Inject

/**
 * ViewModel for the Expense List screen.
 * Manages the UI state, handles user interactions, and fetches expense data.
 *
 * @param getExpensesByDateRangeUseCase Use case to fetch expenses within a date range.
 * @param getDailyTotalUseCase Use case to calculate the total expenses for a day.
 * @param deleteExpenseUseCase Use case to delete an expense.
 */
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val getExpensesByDateRangeUseCase: GetExpensesByDateRangeUseCase,
    private val getDailyTotalUseCase: GetDailyTotalUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseListUiState())
    /**
     * The current UI state of the Expense List screen, observed by the UI.
     */
    val uiState: StateFlow<ExpenseListUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ExpenseListEvent>()
    /**
     * A flow of one-time events to be consumed by the UI (e.g., showing toasts).
     */
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        onDateSelected(System.currentTimeMillis())
    }

    /**
     * Handles the selection of a new date by the user.
     * Updates the UI state with the new date and triggers loading of expenses for that date.
     *
     * @param newDateMillis The newly selected date in milliseconds since epoch.
     */
    fun onDateSelected(newDateMillis: Long) {
        _uiState.update {
            it.copy(
                selectedDate = newDateMillis,
                isLoading = true,
                errorMessage = null
            )
        }
        loadExpensesAndTotalForDate(newDateMillis)
    }

    /**
     * Refreshes the expense data for the currently selected date.
     * Useful for pull-to-refresh or after an operation like deletion.
     */
    fun refreshData() {
        loadExpensesAndTotalForDate(uiState.value.selectedDate)
    }

    /**
     * Determines the [TimeOfDay] (Morning, Afternoon, Evening) based on the hour of the given timestamp.
     *
     * @param timestamp The timestamp in milliseconds.
     * @return The corresponding [TimeOfDay].
     */
    private fun getTimeOfDay(timestamp: Long): TimeOfDay {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> TimeOfDay.MORNING // 12 AM - 11:59 AM
            in 12..16 -> TimeOfDay.AFTERNOON // 12 PM - 4:59 PM
            else -> TimeOfDay.EVENING // 5 PM - 11:59 PM
        }
    }

    /**
     * Groups a list of expenses by [TimeOfDay].
     * Ensures all [TimeOfDay] keys exist in the resulting map, even if they have no expenses.
     *
     * @param expenses The list of [Expense] objects to group.
     * @return A map where keys are [TimeOfDay] and values are lists of corresponding expenses.
     */
    private fun groupExpensesByTimeOfDay(expenses: List<Expense>): Map<TimeOfDay, List<Expense>> {
        val groupedMap = EnumMap<TimeOfDay, MutableList<Expense>>(TimeOfDay::class.java)
        TimeOfDay.values().forEach { tod -> groupedMap[tod] = mutableListOf() }

        expenses.forEach { expense ->
            val timeOfDay = getTimeOfDay(expense.date)
            groupedMap[timeOfDay]?.add(expense)
        }
        return groupedMap.mapValues { it.value.toList() } 
    }


    /**
     * Loads expenses and the total spending for a specific date.
     * Updates the UI state with loading indicators, fetched data, or error messages.
     *
     * @param dateMillis The specific date in milliseconds for which to load data.
     */
    private fun loadExpensesAndTotalForDate(dateMillis: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val dayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        viewModelScope.launch {
            getExpensesByDateRangeUseCase(dayStart, dayEnd)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Error fetching expenses: ${e.message}")
                    }
                }
                .collect { expenses ->
                    _uiState.update { currentState ->
                        val newTimeOfDayGroupedExpenses = if (currentState.groupBy == GroupByOption.TIME) {
                            groupExpensesByTimeOfDay(expenses)
                        } else {
                            emptyMap()
                        }
                        val newCategoryGroupedExpenses = if (currentState.groupBy == GroupByOption.CATEGORY) {
                            groupExpensesByCategory(expenses)
                        } else {
                            emptyMap()
                        }
                        currentState.copy(
                            expenses = expenses,
                            totalExpenseCountForSelectedDate = expenses.size,
                            timeOfDayGroupedExpenses = newTimeOfDayGroupedExpenses,
                            groupedExpenses = newCategoryGroupedExpenses
                            // isLoading will be set to false by the other launch block
                        )
                    }
                }
        }

        viewModelScope.launch {
            getDailyTotalUseCase(dayStart) // Use dayStart for consistency as it represents the whole day
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false, // Ensure loading stops even if only this call fails
                            errorMessage = (uiState.value.errorMessage ?: "") + "\\nError fetching total: ${e.message}")
                    }
                }
                .collect { total ->
                    _uiState.update {
                        it.copy(
                            totalSpentForSelectedDate = total,
                            isLoading = false // Final loading state update
                        )
                    }
                }
        }
    }

    /**
     * Sets the grouping option for the expense list.
     * Updates the UI state and re-groups the existing expenses according to the new option.
     *
     * @param newGroupBy The [GroupByOption] to apply.
     */
    fun setGroupBy(newGroupBy: GroupByOption) {
        if (_uiState.value.groupBy != newGroupBy) {
            _uiState.update { currentState ->
                val newTimeOfDayGroupedExpenses = if (newGroupBy == GroupByOption.TIME) {
                    groupExpensesByTimeOfDay(currentState.expenses)
                } else {
                    emptyMap() // Clear if not grouping by time
                }
                val newCategoryGroupedExpenses = if (newGroupBy == GroupByOption.CATEGORY) {
                    groupExpensesByCategory(currentState.expenses)
                } else {
                    emptyMap() // Clear if not grouping by category
                }
                currentState.copy(
                    groupBy = newGroupBy,
                    timeOfDayGroupedExpenses = newTimeOfDayGroupedExpenses,
                    groupedExpenses = newCategoryGroupedExpenses
                )
            }
        }
    }

    /**
     * Groups a list of expenses by their [CategoryType].
     * Handles potential unknown categories by defaulting to a fallback (e.g., FOOD).
     * The resulting map is sorted by category name.
     *
     * @param expenses The list of [Expense] objects to group.
     * @return A map where keys are [CategoryType] and values are lists of corresponding expenses, sorted by category name.
     */
    private fun groupExpensesByCategory(expenses: List<Expense>): Map<CategoryType, List<Expense>> {
        return expenses.groupBy { expense ->
            try {
                CategoryType.valueOf(expense.category.uppercase())
            } catch (e: IllegalArgumentException) {
                println("Warning: Unknown category '${expense.category}' for expense ID ${expense.id}. Grouping under FOOD as fallback.")
                CategoryType.FOOD // Fallback or a specific 'UNKNOWN' category
            }
        }.toSortedMap(compareBy { it.name }) // Sorts the map by category name (enum name)
    }

    /**
     * Deletes a given expense.
     * Emits a toast message indicating success or failure and refreshes the expense list.
     *
     * @param expense The [Expense] object to be deleted.
     */
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                deleteExpenseUseCase(expense)
                _eventFlow.emit(ExpenseListEvent.ShowToast("Expense deleted successfully"))
                refreshData() // Refresh data after deletion
            } catch (e: Exception) {
                _eventFlow.emit(ExpenseListEvent.ShowToast("Failed to delete expense: ${e.message}"))
            }
        }
    }
}
