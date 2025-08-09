package com.yourssohail.smartdailyexpensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yourssohail.smartdailyexpensetracker.data.model.ThemeSetting
// Removed ApplicationContext import as it's no longer directly used in constructor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// The DataStore instance definition can remain here or be moved if preferred,
// but it's used by DataStoreModule now.
val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences> // Injected DataStore
) {

    private object PreferencesKeys {
        val THEME_SETTING = stringPreferencesKey("theme_setting")
    }

    val themeSetting: Flow<ThemeSetting> = dataStore.data // Use injected dataStore
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_SETTING] ?: ThemeSetting.SYSTEM_DEFAULT.name
            try {
                ThemeSetting.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ThemeSetting.SYSTEM_DEFAULT
            }
        }

    suspend fun setThemeSetting(theme: ThemeSetting) {
        dataStore.edit { preferences -> // Use injected dataStore
            preferences[PreferencesKeys.THEME_SETTING] = theme.name
        }
    }
}
