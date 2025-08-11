package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.domain.model.Expense 
import com.yourssohail.smartdailyexpensetracker.domain.repository.ExpenseRepository
import javax.inject.Inject 

/**
 * Use case for deleting an expense.
 */
class DeleteExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    /**
     * Deletes a given expense from the repository.
     *
     * @param expense The [Expense] object to be deleted.
     */
    suspend operator fun invoke(expense: Expense) {
        expenseRepository.deleteExpense(expense)
    }

    // Optional: Overload to delete by ID if needed directly by a use case
    // suspend operator fun invoke(expenseId: Long) {
    //     // This would require the repository to have a deleteById method,
    //     // or fetching the expense first, then deleting.
    //     // For simplicity, sticking to deleting by Expense object for now.
    // }
}
