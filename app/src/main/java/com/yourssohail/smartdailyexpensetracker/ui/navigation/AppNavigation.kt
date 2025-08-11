package com.yourssohail.smartdailyexpensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yourssohail.smartdailyexpensetracker.ui.expenselist.ExpenseListScreen
import com.yourssohail.smartdailyexpensetracker.ui.expenselist.ExpenseListViewModel
import com.yourssohail.smartdailyexpensetracker.ui.expenses.ExpenseEntryScreen
import com.yourssohail.smartdailyexpensetracker.ui.report.ExpenseReportScreen
import com.yourssohail.smartdailyexpensetracker.ui.settings.SettingsScreen


const val EXPENSE_ID_ARG = "expenseId"

// Helper to construct the route for ExpenseEntry with optional argument
fun expenseEntryRouteDefinition(): String {
    return "${Screen.ExpenseEntry.route}?$EXPENSE_ID_ARG={$EXPENSE_ID_ARG}"
}

fun navigateToExpenseEntry(expenseId: Long? = null): String {
    return if (expenseId != null && expenseId != -1L) {
        "${Screen.ExpenseEntry.route}?$EXPENSE_ID_ARG=$expenseId"
    } else {
        Screen.ExpenseEntry.route
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            // bottomNavItems is imported from AppDestinations.kt
            val screensWithBottomBar = remember { bottomNavItems.map { it.route } }
            val showBottomBar = screensWithBottomBar.any { currentRoute?.startsWith(it) == true }

            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                ){
                    val currentDestination = navBackStackEntry?.destination
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                screen.icon?.let {
                                    Icon(it, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.ExpenseList.route, // Use Screen object
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.ExpenseList.route) { // Use Screen object
                ExpenseListScreen(
                    onNavigateToExpenseEntry = {
                        navController.navigate(navigateToExpenseEntry()) // Navigate to base route for new entry
                    },
                    onNavigateToExpenseEdit = { expenseId ->
                        navController.navigate(navigateToExpenseEntry(expenseId))
                    }
                )
            }
            composable(
                route = expenseEntryRouteDefinition(), // Use helper for route definition
                arguments = listOf(navArgument(EXPENSE_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L // Default for new expense
                })
            ) {
                val previousBackStackEntry = remember(navController.previousBackStackEntry) {
                    navController.previousBackStackEntry
                }
                val expenseListViewModel: ExpenseListViewModel? = previousBackStackEntry?.let {
                    hiltViewModel(it)
                }

                ExpenseEntryScreen(
                    onExpenseSaved = {
                        // Attempt to refresh data in the ExpenseListViewModel
                        expenseListViewModel?.refreshData()
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.ExpenseReport.route) { // Use Screen object
                ExpenseReportScreen()
            }
            composable(Screen.Settings.route) { // Use Screen object
                SettingsScreen()
            }
        }
    }
}
