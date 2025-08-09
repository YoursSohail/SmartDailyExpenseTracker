package com.yourssohail.smartdailyexpensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// import androidx.hilt.navigation.compose.hiltViewModel // Keep if AppNavigation uses it, otherwise can be removed if not used elsewhere in this file
import com.yourssohail.smartdailyexpensetracker.data.model.ThemeSetting
// Corrected import to use ThemePreferencesRepository
import com.yourssohail.smartdailyexpensetracker.data.preferences.ThemePreferencesRepository
import com.yourssohail.smartdailyexpensetracker.ui.navigation.AppNavigation
import com.yourssohail.smartdailyexpensetracker.ui.theme.SmartDailyExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferencesRepository: ThemePreferencesRepository // Corrected repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeSettingState by themePreferencesRepository.themeSetting.collectAsState(initial = ThemeSetting.SYSTEM_DEFAULT)

            val useDarkTheme = when (themeSettingState) {
                ThemeSetting.LIGHT -> false
                ThemeSetting.DARK -> true
                ThemeSetting.SYSTEM_DEFAULT -> isSystemInDarkTheme()
            }

            SmartDailyExpenseTrackerTheme(darkTheme = useDarkTheme) {
                AppNavigation()
            }
        }
    }
}
