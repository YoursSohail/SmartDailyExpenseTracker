package com.yourssohail.smartdailyexpensetracker.data.repository

import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {

    suspend fun insertExpense(expense: Expense): Long

    suspend fun updateExpense(expense: Expense)

    suspend fun deleteExpense(expense: Expense)

    fun getExpenseById(id: Long): Flow<Expense?>

    fun getAllExpenses(): Flow<List<Expense>>

    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>

    fun getExpensesForToday(todayStart: Long, todayEnd: Long): Flow<List<Expense>>

    fun getTotalSpentOnDate(dateStart: Long, dateEnd: Long): Flow<Double?>

    fun detectDuplicate(title: String, amount: Double, date: Long): Flow<Expense?>

    // TODO: Add methods for mock offline-first sync if needed later
}
