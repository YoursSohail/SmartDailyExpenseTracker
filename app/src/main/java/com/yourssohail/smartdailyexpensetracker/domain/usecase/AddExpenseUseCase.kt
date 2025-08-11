package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.domain.model.Expense 
import com.yourssohail.smartdailyexpensetracker.domain.repository.ExpenseRepository
import javax.inject.Inject 

/**
 * Use case for adding a new expense.
 * It performs basic validation before inserting the expense into the repository.
 */
class AddExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    /**
     * Adds a new expense to the repository after performing basic validation.
     *
     * @param expense The [Expense] object to be added.
     * @return The ID of the newly inserted expense.
     * @throws IllegalArgumentException if the expense title is blank or the amount is not positive.
     */
    suspend operator fun invoke(expense: Expense): Long {
        if (expense.title.isBlank()) {
            throw IllegalArgumentException("Expense title cannot be blank.")
        }
        if (expense.amount <= 0) {
            throw IllegalArgumentException("Expense amount must be positive.")
        }
        // Add more validation as needed (e.g., category, date constraints)
        return expenseRepository.insertExpense(expense)
    }
}
