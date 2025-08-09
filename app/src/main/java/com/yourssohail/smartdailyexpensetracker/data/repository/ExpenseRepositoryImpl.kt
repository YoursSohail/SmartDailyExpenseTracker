package com.yourssohail.smartdailyexpensetracker.data.repository

import com.yourssohail.smartdailyexpensetracker.data.local.dao.ExpenseDao
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject // Assuming Hilt or another DI framework might be used later.
                      // If not, this can be removed or replaced with manual injection.

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    override suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    override fun getExpenseById(id: Long): Flow<Expense?> {
        return expenseDao.getExpenseById(id)
    }

    override fun getAllExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses()
    }

    override fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesByDateRange(startDate, endDate)
    }

    override fun getExpensesForToday(todayStart: Long, todayEnd: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesForToday(todayStart, todayEnd)
    }

    override fun getTotalSpentOnDate(dateStart: Long, dateEnd: Long): Flow<Double?> {
        return expenseDao.getTotalSpentOnDate(dateStart, dateEnd)
    }

    override fun detectDuplicate(title: String, amount: Double, dateStart: Long, dateEnd: Long): Flow<Expense?> { // Changed
        return expenseDao.detectDuplicate(title, amount, dateStart, dateEnd) // Changed
    }

    // Mock logic for offline-first sync (as per requirements.md and tasks.md)
    // This is a placeholder and would need a proper implementation strategy.
    fun performMockSync() {
        // Simulate network call or data processing
        println("ExpenseRepositoryImpl: Mock sync initiated...")
        // In a real scenario, this might involve:
        // 1. Fetching remote data
        // 2. Comparing with local data
        // 3. Merging/updating local database
        // 4. Posting updates or handling conflicts
        println("ExpenseRepositoryImpl: Mock sync completed.")
    }
}
