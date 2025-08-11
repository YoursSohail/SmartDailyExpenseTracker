package com.yourssohail.smartdailyexpensetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssohail.smartdailyexpensetracker.data.model.ThemeSetting
import com.yourssohail.smartdailyexpensetracker.data.preferences.ThemePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Manages UI-related data for theme settings and handles user interactions
 * for updating theme preferences.
 *
 * @param themePreferencesRepository Repository for accessing and modifying theme preferences.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository
) : ViewModel() {

    /**
     * Exposes the current theme setting as a [StateFlow].
     * This flow emits the current [ThemeSetting] (e.g., Light, Dark, System Default)
     * and updates whenever the setting changes in the [themePreferencesRepository].
     * The flow is started when a collector subscribes and is kept active for 5 seconds
     * after the last collector unsubscribes.
     * The initial value is [ThemeSetting.SYSTEM_DEFAULT].
     */
    val currentThemeSetting: StateFlow<ThemeSetting> =
        themePreferencesRepository.themeSetting.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeSetting.SYSTEM_DEFAULT
        )

    /**
     * Updates the application's theme setting.
     * This function launches a coroutine in the [viewModelScope] to call the
     * repository to persist the new theme setting.
     *
     * @param newThemeSetting The [ThemeSetting] to apply and save.
     */
    fun updateThemeSetting(newThemeSetting: ThemeSetting) {
        viewModelScope.launch {
            themePreferencesRepository.setThemeSetting(newThemeSetting)
        }
    }
}
