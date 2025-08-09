package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.repository.ExpenseRepository
import javax.inject.Inject // Assuming DI

class AddExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense): Long {
        // Basic validation (can be expanded or moved to a dedicated ValidateExpenseUseCase later)
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
