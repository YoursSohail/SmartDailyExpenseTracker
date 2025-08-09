package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpensesByDateRangeUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<Expense>> {
        return expenseRepository.getExpensesByDateRange(startDate, endDate)
    }
}
