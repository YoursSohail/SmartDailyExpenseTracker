package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.domain.model.Expense 
import javax.inject.Inject

/**
 * Represents the result of a validation operation.
 * Can be either [Success] or [Error].
 */
sealed class ValidationResult {
    /** Indicates that the validation was successful. */
    object Success : ValidationResult()
    /**
     * Indicates that the validation failed.
     * @param message A descriptive message explaining the validation error.
     */
    data class Error(val message: String) : ValidationResult()
}

/**
 * Use case for validating expense data.
 * This class provides methods to validate an entire [Expense] object or individual fields.
 */
class ValidateExpenseUseCase @Inject constructor() { // No repository needed for pure validation logic

    /**
     * Validates all relevant fields of an [Expense] object.
     *
     * @param expense The [Expense] object to validate.
     * @return [ValidationResult.Success] if all validations pass, otherwise [ValidationResult.Error]
     *         with a specific error message.
     */
    operator fun invoke(expense: Expense): ValidationResult { 
        if (expense.title.isBlank()) {
            return ValidationResult.Error("Expense title cannot be blank.")
        }
        if (expense.amount <= 0) {
            return ValidationResult.Error("Expense amount must be a positive value.")
        }
        if (expense.category.isBlank()) {
            return ValidationResult.Error("Expense category must be selected.")
        }
        if (expense.notes != null && expense.notes.length > 100) {
            return ValidationResult.Error("Notes cannot exceed 100 characters.")
        }
        // Add any other specific validation rules here
        return ValidationResult.Success
    }

    /**
     * Validates an expense title.
     *
     * @param title The title string to validate.
     * @return [ValidationResult.Success] if the title is not blank, otherwise [ValidationResult.Error].
     */
    fun validateTitle(title: String): ValidationResult {
        if (title.isBlank()) return ValidationResult.Error("Title cannot be blank.")
        return ValidationResult.Success
    }

    /**
     * Validates an expense amount.
     *
     * @param amount The amount to validate.
     * @return [ValidationResult.Success] if the amount is not null and positive, otherwise [ValidationResult.Error].
     */
    fun validateAmount(amount: Double?): ValidationResult {
        if (amount == null) return ValidationResult.Error("Amount cannot be empty.")
        if (amount <= 0) return ValidationResult.Error("Amount must be positive.")
        return ValidationResult.Success
    }

    /**
     * Validates expense notes.
     *
     * @param notes The notes string to validate.
     * @return [ValidationResult.Success] if notes are null or their length does not exceed 100 characters,
     *         otherwise [ValidationResult.Error].
     */
     fun validateNotes(notes: String?): ValidationResult {
        if (notes != null && notes.length > 100) {
            return ValidationResult.Error("Notes cannot exceed 100 characters.")
        }
        return ValidationResult.Success
    }
}
