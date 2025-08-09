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
import javax.inject.Inject

enum class GroupByOption {
    TIME,
    CATEGORY
}

data class ExpenseListUiState(
    val expenses: List<Expense> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(), // Default to today
    val totalSpentForSelectedDate: Double? = null,
    val totalExpenseCountForSelectedDate: Int = 0,
    val isLoading: Boolean = true,
    val groupBy: GroupByOption = GroupByOption.TIME,
    val errorMessage: String? = null,
    val groupedExpenses: Map<CategoryType, List<Expense>> = emptyMap()
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
        // Load expenses for the current day initially
        onDateSelected(System.currentTimeMillis())
    }

    fun onDateSelected(newDateMillis: Long) {
        _uiState.update { it.copy(selectedDate = newDateMillis, isLoading = true, errorMessage = null) }
        loadExpensesAndTotalForDate(newDateMillis)
    }

    fun refreshData() {
        // Re-fetches data for the currently selected date
        loadExpensesAndTotalForDate(uiState.value.selectedDate)
    }

    private fun loadExpensesAndTotalForDate(dateMillis: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val dayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val dayEnd = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
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
                    _uiState.update { currentState -> // Use currentState
                        currentState.copy(
                            expenses = expenses,
                            totalExpenseCountForSelectedDate = expenses.size,
                            groupedExpenses = if (currentState.groupBy == GroupByOption.CATEGORY) { // Check current groupBy
                                groupExpensesByCategory(expenses) // Group the newly fetched expenses
                            } else {
                                emptyMap()
                            }
                            // isLoading is handled by the total fetching coroutine
                        )
                    }
                }
        }

        viewModelScope.launch {
            getDailyTotalUseCase(dayStart)
                 .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, // Set loading false even if total fails
                            errorMessage = (uiState.value.errorMessage ?: "") + "\nError fetching total: ${e.message}")
                    }
                }
                .collect { total ->
                    _uiState.update {
                        it.copy(
                            totalSpentForSelectedDate = total,
                            isLoading = false // Set loading false after both calls attempt/complete
                        )
                    }
                }
        }
    }

    // Replaced onToggleGroupBy with setGroupBy
    fun setGroupBy(newGroupBy: GroupByOption) {
        // Only update if the group by option has actually changed
        if (_uiState.value.groupBy != newGroupBy) {
            _uiState.update { currentState ->
                currentState.copy(
                    groupBy = newGroupBy,
                    groupedExpenses = if (newGroupBy == GroupByOption.CATEGORY) {
                        // Re-group the currently loaded expenses
                        groupExpensesByCategory(currentState.expenses)
                    } else {
                        // Clear grouped expenses if switching to TIME view
                        emptyMap()
                    }
                )
            }
        }
    }

    private fun groupExpensesByCategory(expenses: List<Expense>): Map<CategoryType, List<Expense>> {
        return expenses.groupBy { expense ->
            try {
                CategoryType.valueOf(expense.category.uppercase())
            } catch (e: IllegalArgumentException) {
                println("Warning: Unknown category '${expense.category}' for expense ID ${expense.id}. Grouping under FOOD as fallback.")
                CategoryType.FOOD // Fallback for unknown categories
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                deleteExpenseUseCase(expense)
                _eventFlow.emit(ExpenseListEvent.ShowToast("Expense deleted successfully"))
                refreshData() // Refresh the list after deletion
            } catch (e: Exception) {
                _eventFlow.emit(ExpenseListEvent.ShowToast("Failed to delete expense: ${e.message}"))
            }
        }
    }
}
