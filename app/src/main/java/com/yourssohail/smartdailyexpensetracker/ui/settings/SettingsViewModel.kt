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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository
) : ViewModel() {

    // Expose the current theme setting as a StateFlow
    val currentThemeSetting: StateFlow<ThemeSetting> =
        themePreferencesRepository.themeSetting.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keep subscribed for 5s after last collector
            initialValue = ThemeSetting.SYSTEM_DEFAULT // Sensible default
        )

    fun updateThemeSetting(newThemeSetting: ThemeSetting) {
        viewModelScope.launch {
            themePreferencesRepository.setThemeSetting(newThemeSetting)
        }
    }
}
