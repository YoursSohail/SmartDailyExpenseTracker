package com.yourssohail.smartdailyexpensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    object ExpenseList : Screen("expense_list", "Expenses", Icons.Default.ListAlt)
    object ExpenseEntry : Screen("expense_entry", "Add Expense", null) // No icon for bottom bar
    object ExpenseReport : Screen("expense_report", "Report", Icons.Default.Assessment)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

// List of top-level destinations for Bottom Navigation
val bottomNavItems = listOf(
    Screen.ExpenseList,
    Screen.ExpenseReport,
    Screen.Settings
)
