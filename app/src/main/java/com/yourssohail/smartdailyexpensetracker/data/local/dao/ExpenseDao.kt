package com.yourssohail.smartdailyexpensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    /**
     * Inserts a new expense or updates an existing one if an expense with the same ID is found.
     *
     * @param expense The expense to be inserted or updated.
     * @return The ID of the inserted or updated expense.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    /**
     * Updates an existing expense in the database.
     *
     * @param expense The expense with updated values to be saved.
     * If the expense does not exist, the update will have no effect.
     */
    @Update
    suspend fun updateExpense(expense: Expense)

    /*¯*
     * Deletes an expense from the database.
     *
     * @param expense The expense to be deleted. If the expense does not exist in the database, the deletion will have no effect.
     */
    @Delete
    suspend fun deleteExpense(expense: Expense)

    /**
     * Retrieves an expense from the database by its ID.
     *
     * @param id The ID of the expense to be retrieved.
     * @return A Flow emitting the retrieved expense, or null if no expense with the given ID exists.
     */
    @Query("SELECT * FROM expenses WHERE id = :id")
    fun getExpenseById(id: Long): Flow<Expense?>

    /**
     * Retrieves all expenses from the database, ordered by date in descending order.
     *
     * @return A Flow emitting a list of all expenses.
     */
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    /**
     * Retrieves all expenses from the database that fall within the given date range, ordered by date in descending order.
     *
     * @param startDate The start of the date range (inclusive) in milliseconds since the Unix epoch.
     * @param endDate The end of the date range (inclusive) in milliseconds since the Unix epoch.
     * @return A Flow emitting a list of all expenses that fall within the given date range.
     */
    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>

    /**
     * Retrieves all expenses from the database that fall within the current day, ordered by date in descending order.
    /**
     * Calculates the total amount spent on expenses within a specified date range.
     *
     * @param dateStart The start of the date range (inclusive) in milliseconds since the Unix epoch.
     * @param dateEnd The end of the date range (inclusive) in milliseconds since the Unix epoch.
     * @return A Flow emitting the total amount spent, or null if no expenses exist within the date range.
     */
     *
     * @param todayStart The start of the current day (inclusive) in milliseconds since the Unix epoch.
     * @param todayEnd The end of the current day (inclusive) in milliseconds since the Unix epoch.
     * @return A Flow emitting a list of all expenses that fall within the current day.
     */
    @Query("SELECT * FROM expenses WHERE date BETWEEN :todayStart AND :todayEnd ORDER BY date DESC")
    fun getExpensesForToday(todayStart: Long, todayEnd: Long): Flow<List<Expense>>

/* <<<<<<<<<<<<<<  ✨ Windsurf Command ⭐ >>>>>>>>>>>>>>>> */
    /**
     * Calculates the total amount spent on expenses within a specified date range.
     *
     * @param dateStart The start of the date range (inclusive) in milliseconds since the Unix epoch.
     * @param dateEnd The end of the date range (inclusive) in milliseconds since the Unix epoch.
     * @return A Flow emitting the total amount spent, or null if no expenses exist within the date range.
     */
/* <<<<<<<<<<  acb6fa2e-530b-4e7d-a127-16c81b6646f1  >>>>>>>>>>> */
    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :dateStart AND :dateEnd")
    fun getTotalSpentOnDate(dateStart: Long, dateEnd: Long): Flow<Double?>

/* <<<<<<<<<<<<<<  ✨ Windsurf Command ⭐ >>>>>>>>>>>>>>>> */
    /**
     * Retrieves the first expense from the database that matches the given title, amount, and falls within the given date range.
     * This is used to detect duplicate expenses.
     *
     * @param title The title of the expense to search for.
     * @param amount The amount of the expense to search for.
     * @param dateStart The start of the date range (inclusive) in milliseconds since the Unix epoch.
     * @param dateEnd The end of the date range (inclusive) in milliseconds since the Unix epoch.
     * @return A Flow emitting the first matching expense, or null if no such expense exists.
     */
/* <<<<<<<<<<  ac493c23-add7-4d9e-b81f-67b1595dc6b1  >>>>>>>>>>> */
    @Query("SELECT * FROM ex¯penses WHERE title = :title AND amount = :amount AND date BETWEEN :dateStart AND :dateEnd LIMIT 1")
    fun detectDuplicate(title: String, amount: Double, dateStart: Long, dateEnd: Long): Flow<Expense?>

}
