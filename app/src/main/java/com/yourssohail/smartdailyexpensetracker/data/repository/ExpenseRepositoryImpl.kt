package com.yourssohail.smartdailyexpensetracker.data.repository

import com.yourssohail.smartdailyexpensetracker.data.local.dao.ExpenseDao
import com.yourssohail.smartdailyexpensetracker.data.mapper.toDataEntity
import com.yourssohail.smartdailyexpensetracker.data.mapper.toDomain
import com.yourssohail.smartdailyexpensetracker.data.mapper.toDomainModels
import com.yourssohail.smartdailyexpensetracker.domain.model.Expense
import com.yourssohail.smartdailyexpensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao
) : ExpenseRepository { // Implements domain repository

    override suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense.toDataEntity())
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense.toDataEntity())
    }

    override suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense.toDataEntity())
    }

    override fun getExpenseById(id: Long): Flow<Expense?> {
        return expenseDao.getExpenseById(id).map { dataEntity ->
            dataEntity?.toDomain()
        }
    }

    override fun getAllExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses().map { dataEntities ->
            dataEntities.toDomainModels()
        }
    }

    override fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesByDateRange(startDate, endDate).map { dataEntities ->
            dataEntities.toDomainModels()
        }
    }

    override fun getExpensesForToday(todayStart: Long, todayEnd: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesForToday(todayStart, todayEnd).map { dataEntities ->
            dataEntities.toDomainModels()
        }
    }

    override fun getTotalSpentOnDate(dateStart: Long, dateEnd: Long): Flow<Double?> {
        return expenseDao.getTotalSpentOnDate(dateStart, dateEnd)
    }

    override fun detectDuplicate(title: String, amount: Double, dateStart: Long, dateEnd: Long): Flow<Expense?> {
        return expenseDao.detectDuplicate(title, amount, dateStart, dateEnd).map { dataEntity ->
            dataEntity?.toDomain()
        }
    }

    /**
     * Performs a mock synchronization process.
     *
     * This is a placeholder for demonstrating offline-first synchronization logic.
     * In a real application, this would involve fetching remote data, comparing it with
     * local data, merging updates, and handling conflicts.
     */
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
