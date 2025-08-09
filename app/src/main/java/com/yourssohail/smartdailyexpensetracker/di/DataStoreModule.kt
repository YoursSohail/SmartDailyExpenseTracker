package com.yourssohail.smartdailyexpensetracker.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.yourssohail.smartdailyexpensetracker.data.preferences.userPreferencesDataStore // Import the extension property
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
        return appContext.userPreferencesDataStore // Use the extension property here
    }
}
