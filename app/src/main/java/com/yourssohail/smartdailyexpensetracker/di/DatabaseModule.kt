package com.yourssohail.smartdailyexpensetracker.di

import android.content.Context
import com.yourssohail.smartdailyexpensetracker.data.local.AppDatabase
import com.yourssohail.smartdailyexpensetracker.data.local.dao.ExpenseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Provides instances at the application level
object DatabaseModule {

    /**
     * Provides the [AppDatabase] instance that is used to access the app's database.
     *
     * @param appContext The [Context] of the application, which is used to initialize the database
     * @return The [AppDatabase] instance that is used to access the app's database
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return AppDatabase.getDatabase(appContext)
    }

    /**
     * Provides the [ExpenseDao] instance that is used to interact with the expense table in the database.
     *
     * @param appDatabase The [AppDatabase] instance that contains the expense table
     * @return The [ExpenseDao] instance that is associated with the expense table
     */
    @Provides
    @Singleton
    fun provideExpenseDao(appDatabase: AppDatabase): ExpenseDao {
        return appDatabase.expenseDao()
    }
}
