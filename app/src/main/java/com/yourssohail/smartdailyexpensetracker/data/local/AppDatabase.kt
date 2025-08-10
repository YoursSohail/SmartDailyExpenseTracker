package com.yourssohail.smartdailyexpensetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yourssohail.smartdailyexpensetracker.data.local.dao.ExpenseDao
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense

@Database(entities = [Expense::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Exposes methods for interacting with the underlying [Expense] table
     */
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Provides a singleton instance of the [AppDatabase].
         *
         * Note: This method will create a new database if no instance exists,
         * or return the existing instance if one exists.
         *
         * @param context The application context
         * @return A singleton instance of the [AppDatabase]
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
