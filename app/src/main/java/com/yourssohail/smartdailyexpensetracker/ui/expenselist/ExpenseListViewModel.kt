package com.yourssohail.smartdailyexpensetracker.ui.expenselist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.domain.usecase.DeleteExpenseUseCase
import com.yourssohail.smartdailyexpensetracker.domain.usecase.GetDailyTotalUseCase
import com.yourssohail.smartdailyexpensetracker.domain.usecase.GetExpensesByDateRangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    private val deleteExpenseUseCase: DeleteExpenseUseCase // Added for potential delete operations
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
            // Fetch expenses for the date range
            getExpensesByDateRangeUseCase(dayStart, dayEnd)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Error fetching expenses: ${e.message}")
                    }
                }
                .collect { expenses ->
                    _uiState.update {
                        it.copy(
                            expenses = expenses,
                            totalExpenseCountForSelectedDate = expenses.size,
                            groupedExpenses = if (it.groupBy == GroupByOption.CATEGORY) {
                                groupExpensesByCategory(expenses)
                            } else {
                                emptyMap()
                            },
                            // isLoading = false // isLoading will be set to false after total is also fetched
                        )
                    }
                }
        }

        viewModelScope.launch {
            // Fetch total for the day
            // Using dayStart for getDailyTotalUseCase as it expects a timestamp within the desired day
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

    fun onToggleGroupBy() {
        val currentGroupBy = uiState.value.groupBy
        val newGroupBy = if (currentGroupBy == GroupByOption.TIME) GroupByOption.CATEGORY else GroupByOption.TIME
        _uiState.update {
            it.copy(
                groupBy = newGroupBy,
                groupedExpenses = if (newGroupBy == GroupByOption.CATEGORY) {
                    groupExpensesByCategory(it.expenses)
                } else {
                    emptyMap()
                }
            )
        }
    }

    private fun groupExpensesByCategory(expenses: List<Expense>): Map<CategoryType, List<Expense>> {
        return expenses.groupBy { expense ->
            try {
                CategoryType.valueOf(expense.category.uppercase())
            } catch (e: IllegalArgumentException) {
                // Handle cases where category string might not match an enum constant
                // This could happen if new categories are added as strings without updating the enum,
                // or due to data inconsistencies.
                // For a robust app, you might have an "UNKNOWN" category in your enum
                // or log this more formally.
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
