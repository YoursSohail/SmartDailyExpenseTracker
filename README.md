# Expense Buddy

![Expense Buddy Logo](./app/src/main/res/mipmap-xxhdpi/ic_launcher.webp)

## App Overview

Expense Buddy is an Android application built with Jetpack Compose that helps users track their daily expenses efficiently. It features a clean, modern UI with both light and dark themes, and provides functionalities for expense entry, categorized listings, and insightful reports.

## AI Usage Summary

This project involved the use of multiple AI tools. While other AI coding assistants like WindSurf and Cursor were explored during the initial phases, the primary AI tools used for development were ChatGPT for initial brainstorming and the Gemini AI assistant integrated within Android Studio for the bulk of the coding and refactoring tasks.

*   **ChatGPT** was utilized for:
    *   Initial brainstorming and generation of the `requirements.md` file.
    *   Assistance in the app logo creation process.
*   The **Gemini AI assistant**, integrated within Android Studio, was then used for a variety of subsequent development tasks throughout the session:
    *   **Initial Project Planning:** Interpreting the AI-generated `requirements.md` to create a detailed `tasks.md`, outlining architectural choices (MVVM, Clean Architecture), screen structure, and key features.
    *   **Common UI Component Creation:** Generating boilerplate and core logic for reusable UI elements like `FullScreenLoadingIndicator`, `ScreenErrorMessage`, `EmptyStateView`, and `SectionTitle` in `CommonUIComponents.kt`.
    *   **Screen Refactoring:** Systematically updated multiple screens (`ExpenseListComponents.kt`, `ExpenseListScreen.kt`, `ExpenseReportScreen.kt`, `ExpenseEntryScreen.kt`, `SettingsScreen.kt`) to incorporate the newly created common UI components, enhancing consistency and maintainability.
    *   **API Level Compatibility:** Addressed `MediaStore` API level errors by modifying ViewModel code (`ExpenseReportViewModel.kt`) to handle file saving to the Downloads directory correctly across different Android versions (API <29 and API 29+).
    *   **Navigation Consolidation:** Identified and refactored redundant navigation definitions by modifying `AppNavigation.kt` to use a centralized `Screen` sealed class and `bottomNavItems` from `AppDestinations.kt`.
    *   **Documentation:** Assisted in creating and iteratively updating project documentation, including this `README.md` file.

## Key Prompt Logs

Below are a few examples of key prompts used during development.

1.  **Initial `requirements.md` Generation (ChatGPT):**
    *   **Prompt:**
        ```
        You are the best android developer in the world. The pdf provided is the assignment that I need to complete in order to get a job. Use the pdf and provide a requirements.md file that I can copy and paste
        ```
    *   **Effectiveness:** This prompt (used with ChatGPT) laid the groundwork for the project by generating the initial requirements document based on an external PDF.

2.  **Project Initialization and Task Planning (Gemini):**
    *   **Prompt:**
        ```
        You are the best android developer in the world. Go through the requirements.md file understand the project requirement and create a tasks.md accordingly. Then proceed building the project.

        - Make sure to use MVVM and clean architecture for the project and make the UI look modern and elegant by using good color schemes.

        - We will create three screens - Expenselist, expense report and settings, for the expense entry screen use fab in the expense list screen that opens up the entry screen

        - Import all the reqruired dependecies even for the icons (if required) and for showing bar graphs and Room db

        update the tasks.md file accordingly
        ```
    *   **Effectiveness:** Set the stage for project structure and initial task breakdown by the in-IDE AI (Gemini).

3.  **Refactoring with Common UI Components (Gemini):**
    *   **Prompt:**
        ```
        Let's create a new file at `CommonUIComponents.kt` and add a `FullScreenLoadingIndicator` composable. It should be a `Box` composable that fills the max size and centers a `CircularProgressIndicator`."
        ```
    *   **Effectiveness:** Led to the AI-assisted creation of reusable UI elements and improved code maintainability across multiple screens.


4.  **Organizing Screen-Specific UI into `ExpenseListComponents.kt` (Gemini):**
    *   **Prompt:**
        ```
        For the `ExpenseListScreen`, let's improve its structure. Create a new file named `ExpenseListComponents.kt`. Move helper composables like `ListContent` and `AnimatedExpenseListItem` from `ExpenseListScreen.kt` into this new components file. Ensure `ExpenseListScreen.kt` now imports and uses them correctly.
        ```
    *   **Effectiveness:** This led to better organization of the `ExpenseListScreen`'s UI logic, separating the main screen structure from its more granular, list-related components, improving readability and maintainability.

5.  **Batch KDoc Documentation (Gemini):**
    *   **Prompt:**
        ```
        Add docstring to all the methods for:
        - Use case files
        - Repository files
        - ViewModel files
        ```
    *   **Retry Prompt:**
        ```
        Thanks for adding the KDocs. For the Use Case files, could you make the descriptions more specific about what each use case *does* and what its parameters represent? For example, instead of just saying "Executes the use case", for a use case like `GetExpenseByIdUseCase(id: Long)`, the KDoc should explain that it "Retrieves a specific expense by its unique identifier" and that `id` is "the unique ID of the expense to retrieve". Please apply similar descriptive detail to other use cases and their parameters.
        ```
    *   **Effectiveness:** Efficiently applied documentation standards across multiple files. With a follow-up prompt, the KDocs were refined to be more descriptive and context-specific, significantly improving code maintainability and understanding.

6.  **Refactoring Utility Functions (e.g., Formatters) (Gemini):**
    *   **Initial Prompt:**
        ```
        I have a date formatting function \`fun formatDateForDisplay(timestamp: Long): String\` and a currency formatting function \`val indianCurrencyFormat: NumberFormat\` currently in \`ExpenseListViewModel.kt\`.

        Please perform the following:
        1. Create a new Kotlin file named \`Formatters.kt\` in the package \`com.yourssohail.smartdailyexpensetracker.utils\`.
        2. Move both \`formatDateForDisplay\` and \`indianCurrencyFormat\` from \`ExpenseListViewModel.kt\` into this new \`Formatters.kt\` file.
        3. Ensure they are public (or internal, accessible throughout the module).
        4. Update \`ExpenseListViewModel.kt\` to correctly import and use these formatters from their new location.
        ```
    *   **Retry Prompt:**
        ```
        Thanks for moving the formatters to \`utils/Formatters.kt\`. However, \`ExpenseListViewModel.kt\` is now showing errors. Could you please ensure it imports them correctly from \`com.yourssohail.smartdailyexpensetracker.utils.FormattersKt\`? Also, please make \`formatDateForDisplay\` \`internal\` or \`public\` so it's accessible.
        ```
    *   **Effectiveness:** Showcased AI's ability to perform specific refactoring tasks like moving code to a new file and updating import statements, with iterative refinement for correctness (e.g., ensuring proper imports and visibility modifiers after the move).

## Checklist of Features Implemented

*   [x] **Expense Logging:** Easily add new expenses with details such as title, amount, category, date, and optional notes.
*   [x] **Receipt Capture:** Attach images of receipts to expense entries.
*   [x] **Categorization:** Assign expenses to predefined categories (e.g., Food, Travel, Staff, Utility).
*   [x] **Daily Overview:** View a summary of total expenses for the selected day.
*   [x] **Expense Listing:**
    *   [x] Display expenses for a selected date.
    *   [x] Group expenses by category or time.
    *   [x] Edit or delete existing expenses.
    *   [x] Tap on an expense to view its full details in a dialog.
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
*   [x] **Animations:** Implemented animations for adding expenses and list interactions.
*   [x] **Reusable UI Components:** Developed and integrated common UI elements for consistency and maintainability.

## APK Download Link

[Download Expense Buddy APK](https://drive.google.com/file/d/17RKqPzJRXStd5cKGgALrmu7qY0XIUsL3/view?usp=sharing)

## Screenshots

### Light Mode

<p align="left">
  <img src="./screenshots/light/Screenshot_20250810_231601_Expense%20Buddy.jpg" alt="Light Mode Screenshot 1" width="200" height="400"/>
  <img src="./screenshots/light/Screenshot_20250810_174442_Expense%20Buddy.jpg" alt="Light Mode Screenshot 2" width="200" height="400"/>
  <img src="./screenshots/light/Screenshot_20250810_175625_Expense%20Buddy.jpg" alt="Light Mode Screenshot 3" width="200" height="400"/>
  <img src="./screenshots/light/Screenshot_20250810_175636_Expense%20Buddy.jpg" alt="Light Mode Screenshot 4" width="200" height="400"/>
</p>

### Dark Mode

<p align="left">
  <img src="./screenshots/dark/Screenshot_20250810_231608_Expense%20Buddy.jpg" alt="Dark Mode Screenshot 1" width="200" height="400"/>
  <img src="./screenshots/dark/Screenshot_20250810_175656_Expense%20Buddy.jpg" alt="Dark Mode Screenshot 2" width="200" height="400"/>
  <img src="./screenshots/dark/Screenshot_20250810_175702_Expense%20Buddy.jpg" alt="Dark Mode Screenshot 3" width="200" height="400"/>
  <img src="./screenshots/dark/Screenshot_20250810_175714_Expense%20Buddy.jpg" alt="Dark Mode Screenshot 4" width="200" height="400"/>
</p>

## Demo Video

[Link to Demo Video](https://drive.google.com/file/d/101ZyVbAvgxFoTjUVd7OKViE53JBORxvi/view?usp=sharing)


## Resume

[Link to my resume](./resume.pdf)

## Technologies Used

*   Kotlin
*   Jetpack Compose (UI, Material3, Navigation, ViewModel)
*   Room Persistence Library (for local database)
*   Kotlin Coroutines & Flow (for asynchronous operations)
*   Hilt (for Dependency Injection)
*   Jetpack DataStore (for theme preferences)
*   Compose Charts (for report visualization)

## Architecture

The application is structured following Clean Architecture principles to promote separation of concerns, testability, and maintainability. The main layers are:

*   **Presentation Layer (UI):**
    *   Responsible for displaying the application's user interface and handling user interactions.
    *   Comprises Jetpack Compose screens and UI elements.
    *   ViewModels in this layer prepare and manage UI-related data, reacting to user input and interacting with the Domain layer (typically via Use Cases) to fetch or modify data.
    *   Handles navigation between screens.

*   **Domain Layer:**
    *   Contains the core business logic and rules of the application. This layer is independent of the UI and Data layers.
    *   Includes Use Cases (Interactors) that encapsulate specific pieces of business logic (e.g., `AddExpenseUseCase`, `GetExpensesUseCase`).
    *   Defines pure Kotlin data models (Entities) representing the core business objects.
    *   Specifies Repository interfaces that define the contracts for data operations, which are then implemented by the Data layer.

*   **Data Layer:**
    *   Responsible for providing the application with data from various sources and managing data persistence.
    *   Implements the Repository interfaces defined in the Domain layer.
    *   Contains data sources like the Room local database (for expenses) and Jetpack DataStore (for user preferences).
    *   Includes mappers to convert between data models (e.g., Room entities) and domain models.
