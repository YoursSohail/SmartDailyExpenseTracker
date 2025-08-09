package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar // Added
import javax.inject.Inject

class DetectDuplicateExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    /**
     * Detects duplicate expenses based on title, amount, and if they fall on the same calendar day.
     * @param title The title of the expense.
     * @param amount The amount of the expense.
     * @param dateForCheck A timestamp representing the start of the day (00:00:00.000) for which to check for duplicates.
     * @return A Flow emitting the first duplicate Expense found, or null if none.
     */
    operator fun invoke(title: String, amount: Double, dateForCheck: Long): Flow<Expense?> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateForCheck // This is already the start of the day
        }
        // dateForCheck is startOfDayTimestamp
        val startOfDayTimestamp = calendar.timeInMillis

        // Calculate end of day
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDayTimestamp = calendar.timeInMillis

        return expenseRepository.detectDuplicate(title, amount, startOfDayTimestamp, endOfDayTimestamp)
    }
}
