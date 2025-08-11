package com.yourssohail.smartdailyexpensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    object ExpenseList :
        Screen("expense_list_screen", "Expenses", Icons.AutoMirrored.Filled.ListAlt)

    object ExpenseEntry : Screen("expense_entry", "Add Expense", null)
    object ExpenseReport : Screen("expense_report_screen", "Report", Icons.Default.Assessment)
    object Settings : Screen("settings_screen", "Settings", Icons.Default.Settings)
}

// List of top-level destinations for Bottom Navigation
val bottomNavItems = listOf(
    Screen.ExpenseList,
    Screen.ExpenseReport,
    Screen.Settings
)
