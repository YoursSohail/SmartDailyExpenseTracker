package com.yourssohail.smartdailyexpensetracker.domain.repository

import com.yourssohail.smartdailyexpensetracker.domain.model.Expense 
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {

    suspend fun insertExpense(expense: Expense): Long // Uses domain model

    suspend fun updateExpense(expense: Expense) // Uses domain model

    suspend fun deleteExpense(expense: Expense) // Uses domain model

    fun getExpenseById(id: Long): Flow<Expense?> // Uses domain model

    fun getAllExpenses(): Flow<List<Expense>> // Uses domain model

    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> // Uses domain model

    fun getExpensesForToday(todayStart: Long, todayEnd: Long): Flow<List<Expense>> // Uses domain model

    fun getTotalSpentOnDate(dateStart: Long, dateEnd: Long): Flow<Double?>

    fun detectDuplicate(title: String, amount: Double, dateStart: Long, dateEnd: Long): Flow<Expense?> // Uses domain model
}
