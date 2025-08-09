package com.yourssohail.smartdailyexpensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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

// Define route constants and argument keys
object AppDestinations {
    const val EXPENSE_ID_ARG = "expenseId"
    const val EXPENSE_LIST_ROUTE = "expense_list_screen"
    const val EXPENSE_ENTRY_BASE_ROUTE = "expense_entry_screen"
    const val EXPENSE_ENTRY_ROUTE_WITH_ARG =
        "$EXPENSE_ENTRY_BASE_ROUTE?$EXPENSE_ID_ARG={$EXPENSE_ID_ARG}"

    fun expenseEntryRouteWithId(expenseId: Long) =
        "$EXPENSE_ENTRY_BASE_ROUTE?$EXPENSE_ID_ARG=$expenseId"

    const val EXPENSE_REPORT_ROUTE = "expense_report_screen"
    const val SETTINGS_ROUTE = "settings_screen"
}

// Assuming bottomNavItems is imported from where it's defined (e.g., another file in this package)
// and Screen sealed class is also available.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val screensWithBottomBar = remember { bottomNavItems.map { it.route } }
            val showBottomBar = currentRoute in screensWithBottomBar

            if (showBottomBar) {
                NavigationBar{
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
            startDestination = AppDestinations.EXPENSE_LIST_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestinations.EXPENSE_LIST_ROUTE) {
                ExpenseListScreen(
                    onNavigateToExpenseEntry = {
                        navController.navigate(AppDestinations.EXPENSE_ENTRY_BASE_ROUTE)
                    },
                    onNavigateToExpenseEdit = { expenseId ->
                        navController.navigate(AppDestinations.expenseEntryRouteWithId(expenseId))
                    }
                )
            }
            composable(
                route = AppDestinations.EXPENSE_ENTRY_ROUTE_WITH_ARG,
                arguments = listOf(navArgument(AppDestinations.EXPENSE_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
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
                        expenseListViewModel?.refreshData()
                        navController.popBackStack()
                    }
                )
            }
            composable(AppDestinations.EXPENSE_REPORT_ROUTE) {
                ExpenseReportScreen()
            }
            composable(AppDestinations.SETTINGS_ROUTE) {
                SettingsScreen()
            }
        }
    }
}
