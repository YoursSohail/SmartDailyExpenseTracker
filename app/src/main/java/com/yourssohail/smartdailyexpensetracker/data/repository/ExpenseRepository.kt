package com.yourssohail.smartdailyexpensetracker.data.repository

import com.yourssohail.smartdailyexpensetracker.domain.model.Expense 
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {

    /**
    /**
    /**
     * Deletes an expense from the database.
     *
     * @param expense the expense to delete
     */
     * Updates an existing expense record in the database.
     * @param expense The updated expense record, which must have a valid [Expense.id].
     */
     * Inserts a new expense into the database, returning the ID of the newly inserted expense.
     *
     * @param expense the expense to be inserted
     * @return the ID of the inserted expense
     */
    suspend fun insertExpense(expense: Expense): Long

    /**
     * Updates an existing expense record in the database.
     *
     * @param expense the updated expense record, which must have a valid [Expense.id].
     */
    suspend fun updateExpense(expense: Expense)

    /**
     * Deletes an expense from the database.
     *
     * @param expense the expense to delete
     */
    suspend fun deleteExpense(expense: Expense)

    /**
     * Retrieves an expense from the database by its unique identifier.
     *
     * @param id The unique ID of the expense to retrieve.
     * @return A Flow emitting the Expense object if found, or null if no expense with the given ID exists.
     */
    fun getExpenseById(id: Long): Flow<Expense?>

    /**
     * Retrieves all expenses from the database, ordered by date in descending order.
     *
     * @return A Flow that emits a list of all expenses.
     */
    fun getAllExpenses(): Flow<List<Expense>>

    /**
     * Returns a flow of a list of expenses within the specified date range (inclusive).
     *
     * @param startDate the start of the date range, in milliseconds since the epoch
     * @param endDate the end of the date range, in milliseconds since the epoch
     * @return a flow of a list of expenses that fall within the specified date range
     */
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>

    /**
     * Fetches all expenses within the given date range, assuming the date range
     * provided is for a single day (i.e., the start and end dates are the same).
     *
     * @param todayStart the start of the day (inclusive) in milliseconds
     * @param todayEnd the end of the day (exclusive) in milliseconds
     */
    fun getExpensesForToday(todayStart: Long, todayEnd: Long): Flow<List<Expense>>

    /**
     * Calculates the total amount of money spent within a given date range.
     *
     * @param dateStart the start of the date range (inclusive)
     * @param dateEnd the end of the date range (exclusive)
     * @return the total amount of money spent, wrapped in a [Flow] and [Double?].
     *  If there are no expenses in the given date range, a [Double?] with value `null` is returned.
     */
    fun getTotalSpentOnDate(dateStart: Long, dateEnd: Long): Flow<Double?>

    /**
     * Detects if there is a duplicate expense in the database with the specified title, amount,
     * and within the provided date range.
     *
     * @param title The title of the expense.
     * @param amount The amount of the expense.
     * @param dateStart The start timestamp of the date range.
     * @param dateEnd The end timestamp of the date range.
     * @return A Flow emitting the first duplicate Expense found, or null if none exists.
     */
    fun detectDuplicate(title: String, amount: Double, dateStart: Long, dateEnd: Long): Flow<Expense?> // Changed

}
