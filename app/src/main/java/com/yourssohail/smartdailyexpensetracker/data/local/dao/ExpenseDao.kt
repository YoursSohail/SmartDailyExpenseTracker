package com.yourssohail.smartdailyexpensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yourssohail.smartdailyexpensetracker.data.local.entity.ExpenseEntity // Updated import
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long // Updated type

    @Update
    suspend fun updateExpense(expense: ExpenseEntity) // Updated type

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity) // Updated type

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun getExpenseById(id: Long): Flow<ExpenseEntity?> // Updated type

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>> // Updated type

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<ExpenseEntity>> // Updated type

    @Query("SELECT * FROM expenses WHERE date BETWEEN :todayStart AND :todayEnd ORDER BY date DESC")
    fun getExpensesForToday(todayStart: Long, todayEnd: Long): Flow<List<ExpenseEntity>> // Updated type

    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :dateStart AND :dateEnd")
    fun getTotalSpentOnDate(dateStart: Long, dateEnd: Long): Flow<Double?>

    @Query("SELECT * FROM expenses WHERE title = :title AND amount = :amount AND date BETWEEN :dateStart AND :dateEnd LIMIT 1")
    fun detectDuplicate(title: String, amount: Double, dateStart: Long, dateEnd: Long): Flow<ExpenseEntity?> // Updated type
}
