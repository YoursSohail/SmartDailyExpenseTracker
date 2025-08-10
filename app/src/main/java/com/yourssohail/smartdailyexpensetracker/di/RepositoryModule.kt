package com.yourssohail.smartdailyexpensetracker.di

import com.yourssohail.smartdailyexpensetracker.data.local.dao.ExpenseDao
import com.yourssohail.smartdailyexpensetracker.domain.repository.ExpenseRepository // Updated import
import com.yourssohail.smartdailyexpensetracker.data.repository.ExpenseRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository dependencies.
 * This module is installed in the [SingletonComponent], ensuring that
 * repository instances are singletons.
 */
@Module
@InstallIn(SingletonComponent::class) // Often repositories are singletons
object RepositoryModule {

    /**
     * Provides the [ExpenseRepository] singleton instance.
     * This implementation uses [ExpenseRepositoryImpl].
     *
     * @param expenseDao The [ExpenseDao] instance, injected by Hilt, to be used by the repository.
     * @return The singleton instance of [ExpenseRepository].
     */
    @Provides
    @Singleton
    fun provideExpenseRepository(expenseDao: ExpenseDao): ExpenseRepository {
        return ExpenseRepositoryImpl(expenseDao)
    }
}
