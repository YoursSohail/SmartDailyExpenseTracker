package com.yourssohail.smartdailyexpensetracker.ui.expenses

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssohail.smartdailyexpensetracker.domain.model.Expense // Updated import
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.domain.repository.ExpenseRepository
import com.yourssohail.smartdailyexpensetracker.domain.usecase.AddExpenseUseCase
import com.yourssohail.smartdailyexpensetracker.domain.usecase.DetectDuplicateExpenseUseCase
import com.yourssohail.smartdailyexpensetracker.domain.usecase.GetExpenseByIdUseCase
import com.yourssohail.smartdailyexpensetracker.domain.usecase.ValidateExpenseUseCase
import com.yourssohail.smartdailyexpensetracker.domain.usecase.ValidationResult
import com.yourssohail.smartdailyexpensetracker.ui.navigation.EXPENSE_ID_ARG
import com.yourssohail.smartdailyexpensetracker.utils.CURRENCY_FORMATTER_INR
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Expense Entry screen.
 * Handles logic for adding new expenses or editing existing ones, including input validation,
 * duplicate detection, and receipt image management.
 *
 * @param applicationContext The application context, used for file operations.
 * @param addExpenseUseCase Use case for adding or updating an expense.
 * @param validateExpenseUseCase Use case for validating expense inputs.
 * @param detectDuplicateExpenseUseCase Use case for detecting duplicate expenses.
 * @param getExpenseByIdUseCase Use case for fetching an expense by its ID.
 * @param expenseRepository Repository for accessing expense data, used here for total spent today.
 * @param savedStateHandle Handle for accessing navigation arguments (e.g., expense ID for editing).
 */
@HiltViewModel
class ExpenseEntryViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val validateExpenseUseCase: ValidateExpenseUseCase,
    private val detectDuplicateExpenseUseCase: DetectDuplicateExpenseUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val expenseRepository: ExpenseRepository, // For total spent today
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseEntryUiState())
    /**
     * The current UI state of the Expense Entry screen, observed by the UI.
     */
    val uiState: StateFlow<ExpenseEntryUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ExpenseEntryEvent>()
    /**
     * A flow of one-time events to be consumed by the UI.
     */
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * List of available [CategoryType]s for expense categorization.
     */
    val categories: List<CategoryType> = CategoryType.entries.toList()

    init {
        val expenseId = savedStateHandle.get<Long>(EXPENSE_ID_ARG)
        if (expenseId != null && expenseId != -1L) { // -1L is the default if not passed
            _uiState.update { it.copy(isLoading = true, isEditMode = true, currentExpenseId = expenseId) }
            viewModelScope.launch {
                val expense = getExpenseByIdUseCase(expenseId).firstOrNull()
                if (expense != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = expense.title,
                            amount = expense.amount.toString(),
                            selectedCategory = try { CategoryType.valueOf(expense.category) } catch (e: IllegalArgumentException) { null },
                            notes = expense.notes ?: "",
                            date = expense.date,
                            selectedReceiptUri = expense.receiptImagePath, // Initially, selected is the existing one
                            receiptFileName = expense.receiptImagePath?.let { File(it).name },
                            existingReceiptPath = expense.receiptImagePath
                        )
                    }
                    updateSaveButtonStatus()
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ExpenseEntryEvent.ShowToast("Error: Could not load expense to edit."))
                    updateSaveButtonStatus() // Ensure save button state is correct
                }
            }
        } else {
            updateSaveButtonStatus() // Initial state for new expense
        }
        observeTotalSpentToday()
    }

    /**
     * Observes and updates the total amount spent today, formatted as Indian currency.
     */
    private fun observeTotalSpentToday() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            // Set to start of today
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDayMillis = calendar.timeInMillis

            // Set to end of today
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDayMillis = calendar.timeInMillis

            expenseRepository.getTotalSpentOnDate(startOfDayMillis, endOfDayMillis)
                .collectLatest { total ->
                    val formattedTotal = CURRENCY_FORMATTER_INR.format(total ?: 0.0)
                    _uiState.update { it.copy(totalSpentToday = formattedTotal) }
                }
        }
    }

    /**
     * Updates the enabled state of the save button based on the validity of current inputs.
     */
    private fun updateSaveButtonStatus() {
        val state = _uiState.value
        val isTitleValid = state.title.isNotBlank() && state.titleError == null
        val isAmountValid = state.amount.toDoubleOrNull()?.let { it > 0.0 } ?: false && state.amountError == null
        val isCategoryValid = state.selectedCategory != null && state.categoryError == null
        // Notes are optional, so not part of save button enablement criteria unless they have an error
        val isNotesValid = state.notesError == null
        _uiState.update {
            it.copy(isSaveEnabled = isTitleValid && isAmountValid && isCategoryValid && isNotesValid)
        }
    }

    /**
     * Handles changes to the expense title input field.
     * @param newTitle The new title string.
     */
    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, titleError = null, isDuplicateWarningVisible = false) }
        updateSaveButtonStatus()
    }

    /**
     * Handles changes to the expense amount input field.
     * Allows only numeric input (digits and a single decimal point).
     * @param newAmount The new amount string.
     */
    fun onAmountChange(newAmount: String) {
        if (newAmount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(amount = newAmount, amountError = null, isDuplicateWarningVisible = false) }
        } else if (newAmount.isEmpty()){
             _uiState.update { it.copy(amount = "", amountError = null, isDuplicateWarningVisible = false) }
        }
        updateSaveButtonStatus()
    }

    /**
     * Handles changes to the selected expense category.
     * @param category The newly selected [CategoryType].
     */
    fun onCategoryChange(category: CategoryType) {
        _uiState.update { it.copy(selectedCategory = category, categoryError = null) }
        updateSaveButtonStatus()
    }

    /**
     * Handles changes to the expense notes input field.
     * @param newNotes The new notes string.
     */
    fun onNotesChange(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes, notesError = null) }
        updateSaveButtonStatus() // Notes validation can affect save state if they become invalid
    }

    /**
     * Handles changes to the selected expense date.
     * @param newDate The new date in milliseconds.
     */
    fun onDateChange(newDate: Long) {
        _uiState.update { it.copy(date = newDate, isDuplicateWarningVisible = false) }
        // Date change might affect duplicate check, but save button status is not directly dependent on date itself
    }

    /**
     * Handles the selection of a new receipt image.
     * @param uriString The URI string of the selected image.
     * @param fileName The name of the selected image file.
     */
    fun onReceiptImageSelected(uriString: String?, fileName: String?) {
        _uiState.update { it.copy(selectedReceiptUri = uriString, receiptFileName = fileName, receiptImageError = null) }
    }

    /**
     * Handles the removal of the currently selected or existing receipt image.
     */
    fun onRemoveReceiptImage() {
        _uiState.update { it.copy(selectedReceiptUri = null, receiptFileName = null, receiptImageError = null, existingReceiptPath = null ) } // also clear existing if user explicitly removes
    }

    /**
     * Validates all user inputs for the expense entry.
     * Updates the UI state with any validation error messages.
     * @return True if all inputs are valid, false otherwise.
     */
    private fun validateInputs(): Boolean {
        val titleResult = validateExpenseUseCase.validateTitle(uiState.value.title)
        val amountDouble = uiState.value.amount.toDoubleOrNull()
        val amountResult = validateExpenseUseCase.validateAmount(amountDouble)
        // Category is validated for non-null directly
        val notesResult = validateExpenseUseCase.validateNotes(uiState.value.notes) // Assuming notes has max length

        val newTitleError = if (titleResult is ValidationResult.Error) titleResult.message else null
        val newAmountError = if (amountResult is ValidationResult.Error) amountResult.message else null
        val newCategoryError = if (uiState.value.selectedCategory == null) "Category must be selected." else null
        val newNotesError = if (notesResult is ValidationResult.Error) notesResult.message else null


        _uiState.update {
            it.copy(
                titleError = newTitleError,
                amountError = newAmountError,
                categoryError = newCategoryError,
                notesError = newNotesError,
                receiptImageError = null // Clear previous receipt error on new validation
            )
        }
        updateSaveButtonStatus() // Update save button based on new error states
        return newTitleError == null && newAmountError == null && newCategoryError == null && newNotesError == null
    }

    /**
     * Saves the selected receipt image from its URI to internal app storage.
     * If the URI already points to an image in internal storage (e.g., during edit mode), it returns the path directly.
     *
     * @param uriString The URI string of the image to save.
     * @return The absolute path to the saved image file in internal storage, or null if saving failed.
     */
    private suspend fun saveReceiptImageToInternalStorage(uriString: String): String? {
        // If the uriString is already an absolute path within our app's files dir, assume it's the existing one or already copied.
        if (File(uriString).exists() && uriString.startsWith(applicationContext.filesDir.absolutePath)) {
            return uriString
        }

        return withContext(Dispatchers.IO) {
            try {
                val inputStream = applicationContext.contentResolver.openInputStream(Uri.parse(uriString))
                val receiptsDir = File(applicationContext.filesDir, "receipts")
                if (!receiptsDir.exists()) { receiptsDir.mkdirs() }

                val extension = getFileExtension(applicationContext, Uri.parse(uriString)) ?: "jpg" // default to jpg
                val fileName = "${UUID.randomUUID()}.$extension" // Create a unique file name
                val outputFile = File(receiptsDir, fileName)

                inputStream.use { input -> FileOutputStream(outputFile).use { output -> input?.copyTo(output) } }
                outputFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace() // Log error
                _uiState.update { it.copy(receiptImageError = "Failed to save receipt image.") }
                null
            }
        }
    }

    /**
     * Gets the file extension from a content URI.
     * @param context The application context.
     * @param uri The URI of the file.
     * @return The file extension (e.g., "jpg", "png") or null if it cannot be determined.
     */
    private fun getFileExtension(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)?.substringAfterLast('/')
    }

    /**
     * Saves or updates the current expense.
     * Performs validation and duplicate checks before saving.
     * Handles receipt image saving to internal storage.
     *
     * @param forceSave If true, bypasses the duplicate expense warning and saves directly.
     *                  Defaults to false.
     */
    fun saveExpense(forceSave: Boolean = false) {
        if (!validateInputs()) {
            return
        }
        _uiState.update { it.copy(isLoading = true, isDuplicateWarningVisible = false, receiptImageError = null) }

        viewModelScope.launch {
            val currentState = uiState.value
            var finalReceiptImagePath: String? = currentState.existingReceiptPath // Start with existing path

            // Only process image if it has changed from existing or if new one is selected
            if (currentState.selectedReceiptUri != currentState.existingReceiptPath) {
                 if (currentState.selectedReceiptUri != null) {
                    finalReceiptImagePath = saveReceiptImageToInternalStorage(currentState.selectedReceiptUri)
                    if (finalReceiptImagePath == null) { // Image saving failed
                        _uiState.update { it.copy(isLoading = false) } // Receipt error already set by saveReceiptImageToInternalStorage
                        updateSaveButtonStatus()
                        return@launch
                    }
                } else { // User removed an image
                    finalReceiptImagePath = null
                }
            }


            val title = currentState.title
            val amount = currentState.amount.toDoubleOrNull() ?: 0.0 // Already validated, but good to have default
            val category = currentState.selectedCategory!!.name // Already validated non-null
            val date = currentState.date
            val notes = currentState.notes.takeIf { it.isNotBlank() } // Store null if notes are blank

            // Duplicate Check
            if (!forceSave) {
                // Prepare date for duplicate check (start of the selected day)
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = date
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val dateForDuplicateCheck = calendar.timeInMillis
                val existingExpense = detectDuplicateExpenseUseCase(title, amount, dateForDuplicateCheck).firstOrNull()

                // If a duplicate is found AND (it's not edit mode OR it's edit mode but the found ID is different from current)
                if (existingExpense != null && (!currentState.isEditMode || existingExpense.id != currentState.currentExpenseId)) {
                     _uiState.update { it.copy(isLoading = false, isDuplicateWarningVisible = true) }
                    updateSaveButtonStatus()
                    return@launch
                }
            }

            val expenseToSave = Expense(
                id = if (currentState.isEditMode) currentState.currentExpenseId!! else 0L, // Use 0 for new expense for auto-generate
                title = title,
                amount = amount,
                category = category,
                date = date,
                notes = notes,
                receiptImagePath = finalReceiptImagePath
            )

            try {
                addExpenseUseCase(expenseToSave)
                val message = if (currentState.isEditMode) "Expense updated!" else if (forceSave) "Expense saved (override)!" else "Expense saved!"
                _eventFlow.emit(ExpenseEntryEvent.ShowToast(message))
                _eventFlow.emit(ExpenseEntryEvent.ExpenseSaved)
                _uiState.update { it.copy(isLoading = false) } // Reset loading state
                if (!currentState.isEditMode) resetFields() else updateSaveButtonStatus() // Reset for new entry, just update for edit
            } catch (e: Exception) {
                _eventFlow.emit(ExpenseEntryEvent.ShowToast("Error: ${e.message ?: "Failed to save/update"}"))
                _uiState.update { it.copy(isLoading = false) }
                updateSaveButtonStatus()
            }
        }
    }
    
    /**
     * Forces saving the expense, bypassing the duplicate warning.
     * This is typically called after the user confirms they want to save despite a duplicate warning.
     */
    fun forceSaveExpense() {
        saveExpense(forceSave = true)
    }

    /**
     * Dismisses the duplicate expense warning dialog/message.
     */
    fun dismissDuplicateWarning() {
        _uiState.update { it.copy(isDuplicateWarningVisible = false) }
        updateSaveButtonStatus() // Re-check if the main save button can be enabled
    }

    /**
     * Resets all input fields and UI state to their default for entering a new expense.
     * The date is reset to the current system time.
     */
    fun resetFields() {
        // Reset to initial state, keeping today's date
        _uiState.value = ExpenseEntryUiState(date = System.currentTimeMillis(), totalSpentToday = _uiState.value.totalSpentToday)
        updateSaveButtonStatus() // Update save button state for fresh form
    }
}
