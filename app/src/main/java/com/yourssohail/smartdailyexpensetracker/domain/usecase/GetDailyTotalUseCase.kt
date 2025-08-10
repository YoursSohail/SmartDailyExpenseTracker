package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject // Assuming DI

/**
 * Use case for calculating the total expenses for a specific day.
 */
class GetDailyTotalUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    /**
     * Calculates the total expenses for the given date.
     * The time component of [dateInMillis] is ignored; the calculation is for the entire day.
     *
     * @param dateInMillis A timestamp (in milliseconds) representing any time on the desired day.
     * @return A Flow emitting the total sum of expenses for that day as a [Double?].
     *         Emits `null` if there are no expenses.
     */
    operator fun invoke(dateInMillis: Long): Flow<Double?> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val dayEnd = calendar.timeInMillis

        return expenseRepository.getTotalSpentOnDate(dayStart, dayEnd)
    }
}
