package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.domain.model.Expense // Updated import
import com.yourssohail.smartdailyexpensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject // Assuming DI

/**
 * Use case for retrieving expenses from the last 7 days, including today.
 */
class GetSevenDayReportUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    /**
     * Retrieves a list of expenses from the repository for the last 7 days.
     * This includes expenses from today and the 6 preceding days.
     *
     * @return A Flow emitting a list of [Expense] objects for the 7-day period.
     */
    operator fun invoke(): Flow<List<Expense>> {
        val calendar = Calendar.getInstance()

        // Set to the end of today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        // Go back 6 days to get the start of the 7-day period
        // (e.g., if today is 7th, 7-6=1st, so it includes 1st to 7th)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        return expenseRepository.getExpensesByDateRange(startDate, endDate)
    }
}
