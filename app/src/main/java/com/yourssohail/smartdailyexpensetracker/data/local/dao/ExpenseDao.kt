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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun getExpenseById(id: Long): Flow<Expense?>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>

    // Assuming 'todayStart' is the 00:00:00 and 'todayEnd' is 23:59:59.999 for the given day
    @Query("SELECT * FROM expenses WHERE date BETWEEN :todayStart AND :todayEnd ORDER BY date DESC")
    fun getExpensesForToday(todayStart: Long, todayEnd: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :dateStart AND :dateEnd")
    fun getTotalSpentOnDate(dateStart: Long, dateEnd: Long): Flow<Double?>

    // For duplicate detection, checks for the exact title and amount within the given date range (start and end of day)
    @Query("SELECT * FROM expenses WHERE title = :title AND amount = :amount AND date BETWEEN :dateStart AND :dateEnd LIMIT 1")
    fun detectDuplicate(title: String, amount: Double, dateStart: Long, dateEnd: Long): Flow<Expense?>

}
