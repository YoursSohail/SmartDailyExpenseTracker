package com.yourssohail.smartdailyexpensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yourssohail.smartdailyexpensetracker.data.model.ThemeSetting
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to the DataStore for app settings, specifically for theme preferences.
 * This is used by [DataStoreModule] to provide the "settings" DataStore.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Provides access to the DataStore for user-specific preferences.
 * This is used by [DataStoreModule] to provide the "user_preferences" DataStore.
 */
val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

/**
 * Repository for managing theme preferences stored in DataStore.
 * It allows observing and updating the current theme setting.
 *
 * @param context The application context, injected by Hilt.
 */
@Singleton
class ThemePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val THEME_SETTING = stringPreferencesKey("theme_setting")
    }

    /**
     * A [Flow] that emits the current [ThemeSetting].
     * It observes changes in the DataStore and maps the stored theme name
     * to the corresponding [ThemeSetting] enum value.
     * Defaults to [ThemeSetting.SYSTEM_DEFAULT] if no setting is found or if the stored value is invalid.
     */
    val themeSetting: Flow<ThemeSetting> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_SETTING] ?: ThemeSetting.SYSTEM_DEFAULT.name
            try {
                ThemeSetting.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ThemeSetting.SYSTEM_DEFAULT // Fallback if stored value is somehow corrupted
            }
        }

    /**
     * Updates the stored theme setting in the DataStore.
     *
     * @param theme The new theme setting to be saved.
     */
    suspend fun setThemeSetting(theme: ThemeSetting) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_SETTING] = theme.name
        }
    }
}
