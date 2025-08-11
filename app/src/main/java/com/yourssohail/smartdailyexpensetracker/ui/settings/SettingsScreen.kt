package com.yourssohail.smartdailyexpensetracker.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourssohail.smartdailyexpensetracker.data.model.ThemeSetting
import com.yourssohail.smartdailyexpensetracker.ui.common.SectionTitle
import com.yourssohail.smartdailyexpensetracker.ui.theme.SmartDailyExpenseTrackerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentThemeSetting by viewModel.currentThemeSetting.collectAsState()

    SettingsScreenContent(
        currentThemeSetting = currentThemeSetting,
        onThemeSelected = viewModel::updateThemeSetting
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    currentThemeSetting: ThemeSetting,
    onThemeSelected: (ThemeSetting) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SectionTitle(
                text = "Theme Preferences",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(Modifier.selectableGroup()) {
                ThemeSettingOption(
                    text = "Light Mode",
                    selected = currentThemeSetting == ThemeSetting.LIGHT,
                    onClick = { onThemeSelected(ThemeSetting.LIGHT) }
                )
                ThemeSettingOption(
                    text = "Dark Mode",
                    selected = currentThemeSetting == ThemeSetting.DARK,
                    onClick = { onThemeSelected(ThemeSetting.DARK) }
                )
                ThemeSettingOption(
                    text = "System Default",
                    selected = currentThemeSetting == ThemeSetting.SYSTEM_DEFAULT,
                    onClick = { onThemeSelected(ThemeSetting.SYSTEM_DEFAULT) }
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Settings Screen - Light Mode")
@Composable
fun SettingsScreenPreview_Light() {
    SmartDailyExpenseTrackerTheme {
        SettingsScreenContent(
            currentThemeSetting = ThemeSetting.LIGHT,
            onThemeSelected = {}
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen - Dark Mode")
@Composable
fun SettingsScreenPreview_Dark() {
    SmartDailyExpenseTrackerTheme {
        SettingsScreenContent(
            currentThemeSetting = ThemeSetting.DARK,
            onThemeSelected = {}
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen - System Default")
@Composable
fun SettingsScreenPreview_System() {
    SmartDailyExpenseTrackerTheme {
        SettingsScreenContent(
            currentThemeSetting = ThemeSetting.SYSTEM_DEFAULT,
            onThemeSelected = {}
        )
    }
}
