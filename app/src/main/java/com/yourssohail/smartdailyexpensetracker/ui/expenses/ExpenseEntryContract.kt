package com.yourssohail.smartdailyexpensetracker.ui.expenses

import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType

/**
 * Represents the UI state for the Expense Entry screen.
 *
 * @property title The current title of the expense.
 * @property amount The current amount of the expense as a string.
 * @property selectedCategory The currently selected [CategoryType] for the expense.
 * @property notes Optional notes for the expense.
 * @property date The selected date for the expense in milliseconds.
 * @property titleError Error message for the title field, if any.
 * @property amountError Error message for the amount field, if any.
 * @property categoryError Error message for the category field, if any.
 * @property notesError Error message for the notes field, if any.
 * @property isLoading True if an operation is in progress (e.g., saving, loading), false otherwise.
 * @property isDuplicateWarningVisible True if a duplicate expense warning should be shown, false otherwise.
 * @property selectedReceiptUri URI string for the selected receipt image.
 * @property receiptFileName Name of the selected receipt image file.
 * @property receiptImageError Error message related to receipt image processing, if any.
 * @property existingReceiptPath Path to an existing receipt image, used in edit mode.
 * @property currentExpenseId The ID of the expense being edited, if in edit mode.
 * @property isEditMode True if the screen is in edit mode for an existing expense, false for new entry.
 * @property isSaveEnabled True if the save button should be enabled based on input validity, false otherwise.
 * @property totalSpentToday Formatted string representing the total amount spent today.
 */
data class ExpenseEntryUiState(
    val title: String = "",
    val amount: String = "",
    val selectedCategory: CategoryType? = null,
    val notes: String = "",
    val date: Long = System.currentTimeMillis(),

    val titleError: String? = null,
    val amountError: String? = null,
    val categoryError: String? = null,
    val notesError: String? = null,

    val isLoading: Boolean = false,
    val isDuplicateWarningVisible: Boolean = false,

    val selectedReceiptUri: String? = null,
    val receiptFileName: String? = null,
    val receiptImageError: String? = null,
    val existingReceiptPath: String? = null,

    val currentExpenseId: Long? = null,
    val isEditMode: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val totalSpentToday: String = "₹0.00"
)

/**
 * Represents one-time events that can be emitted from the [ExpenseEntryViewModel] to the UI.
 */
sealed class ExpenseEntryEvent {
    /**
     * Event to request showing a toast message.
     * @param message The message to be displayed.
     */
    data class ShowToast(val message: String) : ExpenseEntryEvent()
    /** Signals that an expense has been successfully saved or updated. */
    object ExpenseSaved : ExpenseEntryEvent()
}
