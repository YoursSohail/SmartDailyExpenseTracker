package com.yourssohail.smartdailyexpensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.yourssohail.smartdailyexpensetracker.ui.expenses.ExpenseEntryScreen
import com.yourssohail.smartdailyexpensetracker.ui.expenselist.ExpenseListScreen
import com.yourssohail.smartdailyexpensetracker.ui.expenselist.ExpenseListViewModel
import com.yourssohail.smartdailyexpensetracker.ui.report.ExpenseReportScreen
import com.yourssohail.smartdailyexpensetracker.ui.settings.SettingsScreen

// Define route constants and argument keys
object AppDestinations {
    const val EXPENSE_ID_ARG = "expenseId"
    const val EXPENSE_LIST_ROUTE = "expense_list_screen"
    const val EXPENSE_ENTRY_BASE_ROUTE = "expense_entry_screen"
    const val EXPENSE_ENTRY_ROUTE_WITH_ARG = "$EXPENSE_ENTRY_BASE_ROUTE?$EXPENSE_ID_ARG={$EXPENSE_ID_ARG}"
    fun expenseEntryRouteWithId(expenseId: Long) = "$EXPENSE_ENTRY_BASE_ROUTE?$EXPENSE_ID_ARG=$expenseId"

    const val EXPENSE_REPORT_ROUTE = "expense_report_screen"
    const val SETTINGS_ROUTE = "settings_screen"
}

// Update Screen objects to use new route constants if they are used by bottomNavItems directly
// This part depends on how 'bottomNavItems' and 'Screen' sealed class are defined.
// For simplicity, I'm assuming Screen.ExpenseList.route etc. will match these new constants.
// If not, bottomNavItems needs to be updated to use AppDestinations constants.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            // Assuming bottomNavItems' routes are updated to match AppDestinations constants
            val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

            if (showBottomBar) {
                NavigationBar {
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
                                navController.navigate(screen.route) { // screen.route should align with AppDestinations
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
            startDestination = AppDestinations.EXPENSE_LIST_ROUTE, // Updated start destination
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestinations.EXPENSE_LIST_ROUTE) { // Updated route
                ExpenseListScreen(
                    onNavigateToExpenseEntry = {
                        navController.navigate(AppDestinations.EXPENSE_ENTRY_BASE_ROUTE) // Navigate without ID for new entry
                    },
                    onNavigateToExpenseEdit = { expenseId ->
                        navController.navigate(AppDestinations.expenseEntryRouteWithId(expenseId)) // Navigate with ID for editing
                    }
                )
            }
            composable(
                route = AppDestinations.EXPENSE_ENTRY_ROUTE_WITH_ARG, // Updated route definition
                arguments = listOf(navArgument(AppDestinations.EXPENSE_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L // Default value for new expense
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
                    // ExpenseEntryViewModel will now use SavedStateHandle to get the expenseId
                )
            }
            composable(AppDestinations.EXPENSE_REPORT_ROUTE) { // Assuming this route is updated in Screen object or bottomNavItems
                ExpenseReportScreen()
            }
            composable(AppDestinations.SETTINGS_ROUTE) { // Assuming this route is updated
                SettingsScreen()
            }
        }
    }
}

// Note: The 'bottomNavItems' and 'Screen' sealed class definitions might need adjustments
// to use or align with 'AppDestinations' constants for routes.
// For example, if you have:
// sealed class Screen(val route: String, ...) {
//    object ExpenseList : Screen(AppDestinations.EXPENSE_LIST_ROUTE, ...)
//    ...
// }
// Then it would work seamlessly.
