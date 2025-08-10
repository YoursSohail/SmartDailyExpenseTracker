package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.domain.model.Expense // Updated import
import com.yourssohail.smartdailyexpensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving expenses within a specified date range.
 */
class GetExpensesByDateRangeUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    /**
     * Retrieves a list of expenses from the repository that fall within the given date range.
     * The timestamps should represent the start and end of the desired range, inclusive.
     *
     * @param startDate The start timestamp of the date range (in milliseconds).
     * @param endDate The end timestamp of the date range (in milliseconds).
     * @return A Flow emitting a list of [Expense] objects within the specified range.
     */
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<Expense>> {
        return expenseRepository.getExpensesByDateRange(startDate, endDate)
    }
}
