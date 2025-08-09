package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import javax.inject.Inject // Assuming DI

// Define a sealed class or enum for validation results for more structured feedback
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

class ValidateExpenseUseCase @Inject constructor() { // No repository needed for pure validation logic
    operator fun invoke(expense: Expense): ValidationResult {
        if (expense.title.isBlank()) {
            return ValidationResult.Error("Expense title cannot be blank.")
        }
        if (expense.amount <= 0) {
            return ValidationResult.Error("Expense amount must be a positive value.")
        }
        if (expense.category.isBlank()) { // Assuming category is a string and should not be blank
            return ValidationResult.Error("Expense category must be selected.")
        }
        if (expense.notes != null && expense.notes.length > 100) {
            return ValidationResult.Error("Notes cannot exceed 100 characters.")
        }
        // Add any other specific validation rules here
        return ValidationResult.Success
    }

    // Overload for individual fields if needed, or for direct input strings
    fun validateTitle(title: String): ValidationResult {
        if (title.isBlank()) return ValidationResult.Error("Title cannot be blank.")
        return ValidationResult.Success
    }

    fun validateAmount(amount: Double?): ValidationResult {
        if (amount == null) return ValidationResult.Error("Amount cannot be empty.")
        if (amount <= 0) return ValidationResult.Error("Amount must be positive.")
        return ValidationResult.Success
    }

     fun validateNotes(notes: String?): ValidationResult {
        if (notes != null && notes.length > 100) {
            return ValidationResult.Error("Notes cannot exceed 100 characters.")
        }
        return ValidationResult.Success
    }
}
