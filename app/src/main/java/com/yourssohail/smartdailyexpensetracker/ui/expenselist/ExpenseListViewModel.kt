package com.yourssohail.smartdailyexpensetracker.ui.expenselist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
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

enum class GroupByOption {
    TIME, // Will now mean group by TimeOfDay (Morning, Afternoon, Evening)
    CATEGORY
}

enum class TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING
}

data class ExpenseListUiState(
    val expenses: List<Expense> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val totalSpentForSelectedDate: Double? = null,
    val totalExpenseCountForSelectedDate: Int = 0,
    val isLoading: Boolean = true,
    val groupBy: GroupByOption = GroupByOption.TIME,
    val errorMessage: String? = null,
    val groupedExpenses: Map<CategoryType, List<Expense>> = emptyMap(),
    // Changed from hourlyGroupedExpenses to timeOfDayGroupedExpenses
    val timeOfDayGroupedExpenses: Map<TimeOfDay, List<Expense>> = emptyMap()
)

sealed class ExpenseListEvent {
    data class ShowToast(val message: String) : ExpenseListEvent()
}

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val getExpensesByDateRangeUseCase: GetExpensesByDateRangeUseCase,
    private val getDailyTotalUseCase: GetDailyTotalUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseListUiState())
    val uiState: StateFlow<ExpenseListUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ExpenseListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        onDateSelected(System.currentTimeMillis())
    }

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

    fun refreshData() {
        loadExpensesAndTotalForDate(uiState.value.selectedDate)
    }

    private fun getTimeOfDay(timestamp: Long): TimeOfDay {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> TimeOfDay.MORNING // 12 AM - 11:59 AM
            in 12..16 -> TimeOfDay.AFTERNOON // 12 PM - 4:59 PM
            else -> TimeOfDay.EVENING // 5 PM - 11:59 PM
        }
    }

    private fun groupExpensesByTimeOfDay(expenses: List<Expense>): Map<TimeOfDay, List<Expense>> {
        val groupedMap = EnumMap<TimeOfDay, MutableList<Expense>>(TimeOfDay::class.java)
        // Ensure all TimeOfDay keys exist for ordered display, even if empty
        TimeOfDay.values().forEach { tod -> groupedMap[tod] = mutableListOf() }

        expenses.forEach { expense ->
            val timeOfDay = getTimeOfDay(expense.date)
            groupedMap[timeOfDay]?.add(expense)
        }
        // Filter out empty groups only if you don't want to show empty sections,
        // but for consistent UI, it's often better to show the section header.
        // For this implementation, we will keep all sections.
        return groupedMap.mapValues { it.value.toList() } // Make lists immutable
    }


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
                        )
                    }
                }
        }

        viewModelScope.launch {
            getDailyTotalUseCase(dayStart)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = (uiState.value.errorMessage ?: "") + "\nError fetching total: ${e.message}")
                    }
                }
                .collect { total ->
                    _uiState.update {
                        it.copy(
                            totalSpentForSelectedDate = total,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun setGroupBy(newGroupBy: GroupByOption) {
        if (_uiState.value.groupBy != newGroupBy) {
            _uiState.update { currentState ->
                val newTimeOfDayGroupedExpenses = if (newGroupBy == GroupByOption.TIME) {
                    groupExpensesByTimeOfDay(currentState.expenses)
                } else {
                    emptyMap()
                }
                val newCategoryGroupedExpenses = if (newGroupBy == GroupByOption.CATEGORY) {
                    groupExpensesByCategory(currentState.expenses)
                } else {
                    emptyMap()
                }
                currentState.copy(
                    groupBy = newGroupBy,
                    timeOfDayGroupedExpenses = newTimeOfDayGroupedExpenses,
                    groupedExpenses = newCategoryGroupedExpenses
                )
            }
        }
    }

    private fun groupExpensesByCategory(expenses: List<Expense>): Map<CategoryType, List<Expense>> {
        return expenses.groupBy { expense ->
            try {
                CategoryType.valueOf(expense.category.uppercase())
            } catch (e: IllegalArgumentException) {
                // Consider logging this or handling it more robustly if unknown categories are common
                println("Warning: Unknown category '${expense.category}' for expense ID ${expense.id}. Grouping under FOOD as fallback.")
                CategoryType.FOOD // Fallback or a specific 'UNKNOWN' category
            }
        }.toSortedMap(compareBy { it.name })
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                deleteExpenseUseCase(expense)
                _eventFlow.emit(ExpenseListEvent.ShowToast("Expense deleted successfully"))
                refreshData()
            } catch (e: Exception) {
                _eventFlow.emit(ExpenseListEvent.ShowToast("Failed to delete expense: ${e.message}"))
            }
        }
    }
}
