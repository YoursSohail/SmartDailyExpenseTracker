package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject // Assuming DI

/**
 * Use case for retrieving all expenses.
 */
class GetExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    /**
     * Retrieves all expenses from the repository, typically ordered by date.
     *
     * @return A Flow emitting a list of all [Expense] objects.
     */
    operator fun invoke(): Flow<List<Expense>> {
        return expenseRepository.getAllExpenses()
    }
}
