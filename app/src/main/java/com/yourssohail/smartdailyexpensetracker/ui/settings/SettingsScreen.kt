package com.yourssohail.smartdailyexpensetracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourssohail.smartdailyexpensetracker.data.model.ThemeSetting
import com.yourssohail.smartdailyexpensetracker.ui.common.SectionTitle // Added import

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
    // Add navController or onNavigateBack if needed for navigation
) {
    val currentThemeSetting by viewModel.currentThemeSetting.collectAsState()

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
                modifier = Modifier.padding(bottom = 8.dp) // Retained padding
            )

            Column(Modifier.selectableGroup()) {
                ThemeSettingOption(
                    text = "Light Mode",
                    selected = currentThemeSetting == ThemeSetting.LIGHT,
                    onClick = { viewModel.updateThemeSetting(ThemeSetting.LIGHT) }
                )
                ThemeSettingOption(
                    text = "Dark Mode",
                    selected = currentThemeSetting == ThemeSetting.DARK,
                    onClick = { viewModel.updateThemeSetting(ThemeSetting.DARK) }
                )
                ThemeSettingOption(
                    text = "System Default",
                    selected = currentThemeSetting == ThemeSetting.SYSTEM_DEFAULT,
                    onClick = { viewModel.updateThemeSetting(ThemeSetting.SYSTEM_DEFAULT) }
                )
            }
        }
    }
}

@Composable
private fun ThemeSettingOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null // onClick is handled by the Row's selectable modifier
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
