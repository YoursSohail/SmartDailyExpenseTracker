# Project Tasks: Smart Daily Expense Tracker

## Phase 1: Project Setup & Core Implementation
1.  **Project Initialization & Structure:**
    *   [ ] Set up a new Android Studio project with Jetpack Compose.
    *   [ ] Define package structure for Clean Architecture (data, domain, presentation layers).
    *   [ ] Choose a modern color palette for light and dark themes. Create `Color.kt`.
    *   [ ] Define typography in `Type.kt`.
    *   [ ] Set up basic Light/Dark themes in `Theme.kt`.

2.  **Dependency Management (build.gradle files):**
    *   [ ] Add Jetpack Compose dependencies (UI, Material3, Navigation, ViewModel, LiveData/StateFlow).
    *   [ ] Add Room dependencies (Runtime, KTX, Compiler).
    *   [ ] Add Coroutines dependencies.
    *   [ ] Add Material Icons (if needed beyond built-ins).
    *   [ ] Add a charting library (e.g., MPAndroidChart wrapper or Compose-native).
    *   [ ] (Optional: Hilt/Koin for Dependency Injection).
    *   [ ] Sync Gradle.

3.  **Data Layer (Room & Repository):**
    *   [ ] Define `ExpenseEntity` data class/entity (`com.yourssohail.smartdailyexpensetracker.data.local.entity.ExpenseEntity`).
        *   Fields: `id` (PK), `title`, `amount`, `category` (enum/String), `date` (Long/Date), `notes` (String, optional), `receiptImagePath` (String, optional).
    *   [ ] Define `Category` Enum (`com.yourssohail.smartdailyexpensetracker.data.model.CategoryType` -> Staff, Travel, Food, Utility).
    *   [ ] Create Room `ExpenseDao` (`com.yourssohail.smartdailyexpensetracker.data.local.dao.ExpenseDao`).
        *   Methods: `insertExpense`, `updateExpense`, `deleteExpense`, `getExpenseById`, `getAllExpenses`, `getExpensesByDateRange`, `getExpensesForToday`, `getTotalSpentOnDate`, `detectDuplicate` (based on title, amount, date).
    *   [ ] Create Room `AppDatabase` (`com.yourssohail.smartdailyexpensetracker.data.local.AppDatabase`).
    *   [ ] Implement `ExpenseRepository` interface and its implementation (`com.yourssohail.smartdailyexpensetracker.data.repository.ExpenseRepository`).
        *   Inject `ExpenseDao`.
        *   Expose Flow-based methods for UI.
        *   Include mock logic for offline-first sync.

4.  **Domain Layer (Use Cases - Optional but recommended for Clean Arch):**
    *   [ ] Create `AddExpenseUseCase`.
    *   [ ] Create `GetExpensesUseCase`.
    *   [ ] Create `GetDailyTotalUseCase`.
    *   [ ] Create `Get هفت-DayReportUseCase`.
    *   [ ] Create `ValidateExpenseUseCase`.
    *   [ ] Create `DeleteExpenseUseCase`.

## Phase 2: Feature Development - Screens

5.  **Expense Entry (Dialog/Screen from FAB):**
    *   [ ] Design Composable for Expense Entry UI (`com.yourssohail.smartdailyexpensetracker.ui.expenses.ExpenseEntryScreen.kt` or Dialog).
        *   Fields: Title (TextField), Amount (Number Keyboard TextField), Category (Dropdown/ExposedDropdownMenu), Notes (TextField), Receipt Image (Button to pick, placeholder).
    *   [ ] Create `ExpenseEntryViewModel` (`com.yourssohail.smartdailyexpensetracker.ui.expenses.ExpenseEntryViewModel.kt`).
        *   Handle UI state (input fields, validation errors).
        *   Implement validation logic (amount > 0, title non-empty, notes max 100 chars).
        *   Implement duplicate detection before saving (call repository/use case).
        *   Implement saving expense (call repository/use case).
        *   Manage Toast messages/events for confirmation.
    *   [ ] Implement animation on adding expense (e.g., list item animates in).

6.  **Expense List Screen (`com.yourssohail.smartdailyexpensetracker.ui.expenselist.ExpenseListScreen.kt`):**
    *   [ ] Design Composable for Expense List UI.
        *   Display "Total Spent Today" at the top.
        *   LazyColumn/List for expenses.
        *   FAB for opening Expense Entry.
        *   Filter controls: Date picker, Group by (Category/Time) toggle.
    *   [ ] Create `ExpenseListViewModel` (`com.yourssohail.smartdailyexpensetracker.ui.expenselist.ExpenseListViewModel.kt`).
        *   Fetch and observe expenses (default to today).
        *   Calculate and expose "Total Spent Today".
        *   Handle filter changes and update list.
        *   Calculate and expose total count and amount for the current view.
    *   [ ] Implement `ExpenseListItem` Composable.
    *   [ ] Implement Empty State view when no expenses are found.
    *   [ ] Ensure Light/Dark mode compatibility.
    *   [ ] Add animations for list loading/updates.

7.  **Expense Report Screen (`com.yourssohail.smartdailyexpensetracker.ui.report.ExpenseReportScreen.kt`):**
    *   [ ] Design Composable for Expense Report UI.
        *   Sections for daily totals (last 7 days), category-wise totals.
        *   Placeholder for Bar/Line chart.
        *   Buttons for "Export PDF" (simulated), "Export CSV" (simulated), "Share".
    *   [ ] Create `ExpenseReportViewModel` (`com.yourssohail.smartdailyexpensetracker.ui.report.ExpenseReportViewModel.kt`).
        *   Fetch data for the last 7 days.
        *   Process data for daily and category totals.
        *   Prepare data for the chart.
    *   [ ] Integrate charting library and display chart.
    *   [ ] Implement PDF/CSV export simulation (e.g., Toast message).
    *   [ ] Implement Share Intent with summary text.

8.  **Settings Screen (`com.yourssohail.smartdailyexpensetracker.ui.settings.SettingsScreen.kt`):**
    *   [ ] Design Composable for Settings UI.
        *   Theme switcher (Light/Dark/System).
    *   [ ] Create `SettingsViewModel` (`com.yourssohail.smartdailyexpensetracker.ui.settings.SettingsViewModel.kt`).
        *   Manage theme preference.
    *   [ ] Implement theme persistence (e.g., using Jetpack DataStore).

## Phase 3: Navigation & Polish

9.  **Navigation:**
    *   [ ] Define navigation graph (`com.yourssohail.smartdailyexpensetracker.ui.navigation.AppNavigation.kt`).
        *   Screens: ExpenseList (start destination), ExpenseReport, Settings.
        *   Expense Entry might be a dialog or a separate screen in the nav graph.
    *   [ ] Set up `NavController` in `MainActivity.kt`.
    *   [ ] Implement Bottom Navigation Bar or similar for main screen switching.

10. **Reusable UI Components (`com.yourssohail.smartdailyexpensetracker.ui.components`):**
    *   [ ] Create `ExpenseItemCard` Composable.
    *   [ ] Create generic `StyledButton`, `StyledTextField`, `Dropdown` Composables.
    *   [ ] Create `EmptyStateView` Composable.

11. **Refinements & Bonus Features:**
    *   [ ] Verify all "Mandatory Bonus Features" from `requirements.md` are implemented.
        *   [ ] Theme switcher (Light/Dark mode)
        *   [ ] Persist data locally (Room/Datastore)
        *   [ ] Animation on adding expenses
        *   [ ] Duplicate expense detection
        *   [ ] Validation for all fields (Amount > 0, Title non-empty)
        *   [ ] Offline-first sync (mocked in repository)
        *   [ ] Reusable UI components
    *   [ ] Refine UI/UX, animations, and transitions.
    *   [ ] Perform thorough manual testing on different devices/emulators.

## Phase 4: Documentation & Deliverables

12. **AI Usage Log:**
    *   [ ] Create `AI_LOG.md` and continuously update with prompts and AI interactions.

13. **Documentation & Final Output:**
    *   [ ] Update/Create `README.md` with project overview, setup, and features.
    *   [ ] Write AI Usage Summary (3-5 sentences).
    *   [ ] Generate signed release APK.
    *   [ ] Take screenshots of all screens (light and dark mode).
    *   [ ] Zip source code, AI log, summary, APK, and screenshots for submission.

---
**Next Steps:**
*   Start with **Phase 1: Project Setup & Core Implementation**.
*   Proceed by creating the necessary directories and files.
*   Update this `tasks.md` file by checking off completed items.
