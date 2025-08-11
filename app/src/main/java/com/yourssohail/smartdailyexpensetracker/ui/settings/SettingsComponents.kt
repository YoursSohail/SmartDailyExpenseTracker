package com.yourssohail.smartdailyexpensetracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssohail.smartdailyexpensetracker.ui.theme.SmartDailyExpenseTrackerTheme

@Composable
internal fun ThemeSettingOption(
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
            onClick = null
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true, name = "ThemeSettingOption - Selected")
@Composable
private fun ThemeSettingOptionPreview_Selected() {
    SmartDailyExpenseTrackerTheme {
        ThemeSettingOption(
            text = "Dark Mode",
            selected = true,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "ThemeSettingOption - Unselected")
@Composable
private fun ThemeSettingOptionPreview_Unselected() {
    SmartDailyExpenseTrackerTheme {
        ThemeSettingOption(
            text = "Light Mode",
            selected = false,
            onClick = {}
        )
    }
}
