package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DetectDuplicateExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    // Parameters might need to be adjusted based on what defines a duplicate
    // For now, using title, amount, and date as per ExpenseDao
    operator fun invoke(title: String, amount: Double, date: Long): Flow<Expense?> {
        return expenseRepository.detectDuplicate(title, amount, date)
    }
}
