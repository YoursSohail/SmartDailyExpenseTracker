package com.yourssohail.smartdailyexpensetracker.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.yourssohail.smartdailyexpensetracker.data.preferences.userPreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing DataStore-related dependencies.
 * This module is installed in the [SingletonComponent], meaning the provided
 * dependencies will have a singleton scope.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /**
     * Provides the [DataStore] instance used to store user preferences.
     * This makes use of the `userPreferencesDataStore` extension property.
     *
     * @param appContext The application context, injected by Hilt.
     * @return The singleton [DataStore<Preferences>] instance for user preferences.
     */
    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
        return appContext.userPreferencesDataStore
    }
}
