# Expense Buddy

[App Logo - To be added]

## App Overview

Expense Buddy is an Android application built with Jetpack Compose that helps users track their daily expenses efficiently. It features a clean, modern UI with both light and dark themes, and provides functionalities for expense entry, categorized listings, and insightful reports.

## AI Usage Summary

This project utilized an AI assistant for a variety of development tasks throughout the session:
*   **Initial Project Planning:** Interpreted project requirements from `requirements.md` to generate an initial `tasks.md`, outlining architectural choices (MVVM, Clean Architecture), screen structure, and key features.
*   **Common UI Component Creation:** Generated boilerplate and core logic for reusable UI elements like `FullScreenLoadingIndicator`, `ScreenErrorMessage`, `EmptyStateView`, and `SectionTitle` in `CommonUIComponents.kt`.
*   **Screen Refactoring:** Systematically updated multiple screens (`ExpenseListComponents.kt`, `ExpenseListScreen.kt`, `ExpenseReportScreen.kt`, `ExpenseEntryScreen.kt`, `SettingsScreen.kt`) to incorporate the newly created common UI components, enhancing consistency and maintainability.
*   **API Level Compatibility:** Addressed `MediaStore` API level errors by modifying ViewModel code (`ExpenseReportViewModel.kt`) to handle file saving to the Downloads directory correctly across different Android versions (API <29 and API 29+).
*   **Navigation Consolidation:** Identified and refactored redundant navigation definitions by modifying `AppNavigation.kt` to use a centralized `Screen` sealed class and `bottomNavItems` from `AppDestinations.kt`.
*   **Documentation:** Assisted in creating and iteratively updating project documentation, including this `README.md` file and the `AI_LOG.md` file, which logs key prompts and interactions.

## Key Prompt Logs

Below are a few examples of key prompts used during development. For a more comprehensive log of AI interactions, please see the `AI_LOG.md` file in the project root.

1.  **Project Initialization and Task Planning:**
    *   **User Prompt:** "You are the best android developer in the world. Go through the requirements.md file understand the project requirement and create a tasks.md accordingly. Then proceed building the project.

- Make sure to use MVVM and clean architecture for the project and make the UI look modern and elegant by using good color schemes.

- We will create three screens - Expenselist, expense report and settings, for the expense entry screen use fab in the expense list screen that opens up the entry screen

- Import all the reqruired dependecies even for the icons (if required) and for showing bar graphs and Room db

update the tasks.md file accordingly"
    *   **Effectiveness:** Set the stage for project structure and initial task breakdown.

2.  **Refactoring with Common UI Components:**
    *   **Initial Prompt:** "Let's create a new file at `app/src/main/java/com/yourssohail/smartdailyexpensetracker/ui/common/CommonUIComponents.kt` and add a `FullScreenLoadingIndicator` composable..."
    *   **Effectiveness:** Led to the creation of reusable UI elements and improved code maintainability across multiple screens.

3.  **Addressing API Level Compatibility for File Saving (`MediaStore`):**
    *   **User Prompt (identifying issue):** "`val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)` error: Field requires API level 29 (current min is 24): `android.provider.MediaStore.Downloads#EXTERNAL_CONTENT_URI`"
    *   **Effectiveness:** Resulted in code that correctly handles file saving to the Downloads directory across different Android versions.

## Checklist of Features Implemented

*   [x] **Expense Logging:** Easily add new expenses with details such as title, amount, category, date, and optional notes.
*   [x] **Receipt Capture:** Attach images of receipts to expense entries.
*   [x] **Categorization:** Assign expenses to predefined categories (e.g., Food, Travel, Staff, Utility).
*   [x] **Daily Overview:** View a summary of total expenses for the selected day.
*   [x] **Expense Listing:**
    *   [x] Display expenses for a selected date.
    *   [x] Group expenses by category or time.
    *   [x] Edit or delete existing expenses.
*   [x] **Reporting:**
    *   [x] View a 7-day spending chart.
    *   [x] See expense totals broken down by category.
    *   [x] Export reports to CSV and PDF (to Downloads folder).
    *   [x] Share report summaries as text, or report files (PDF/CSV).
*   [x] **Duplicate Detection:** Get warnings for potentially duplicate expense entries.
*   [x] **Theme Customization:** Switch between Light, Dark, and System Default themes.
*   [x] **Data Persistence:** Expenses and settings are saved locally using Room and DataStore.
*   [x] **Modern UI:** Built entirely with Jetpack Compose, following Material 3 guidelines.
*   [x] **Offline First:** (Mocked) Designed with offline usability in mind.

## APK Download Link

[Link to APK - To be added]

## Screenshots

[Screenshots - To be added]
*(Please add screenshots of the application in light and dark mode for key screens: Expense List, Expense Entry, Expense Report, Settings.)*

## Resume

[Link to your resume.pdf/resume.docx/resume.txt - To be added]

## Setup & Build

1.  Clone the repository:
    ```bash
    git clone [repository_url] # Replace with your actual repository URL
    ```
2.  Open the project in Android Studio (latest stable version recommended).
3.  Ensure you have the necessary Android SDKs and build tools installed.
4.  Sync Gradle files.
5.  Build and run the application on an emulator or physical device.

## Technologies Used

*   Kotlin
*   Jetpack Compose (UI, Material3, Navigation, ViewModel)
*   Room Persistence Library (for local database)
*   Kotlin Coroutines & Flow (for asynchronous operations)
*   Hilt (for Dependency Injection)
*   Jetpack DataStore (for theme preferences)
*   Compose Charts (for report visualization)

## Architecture

The application aims to follow Clean Architecture principles, separating concerns into:
*   **Data Layer:** Handles data sources (Room database, preferences DataStore) and repositories.
*   **Domain Layer:** Contains use cases and business logic (currently partially implemented with use cases directly in ViewModels for some features).
*   **Presentation Layer (UI):** Consists of Composable screens, ViewModels, and navigation.
