package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.repository.ExpenseRepository
import javax.inject.Inject // Assuming DI

class DeleteExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
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
