package com.yourssohail.smartdailyexpensetracker.domain.usecase

import com.yourssohail.smartdailyexpensetracker.domain.model.Expense // Updated import
import com.yourssohail.smartdailyexpensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving a single expense by its unique identifier.
 */
class GetExpenseByIdUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    /**
     * Retrieves an expense from the repository by its ID.
     *
     * @param id The unique ID of the expense to retrieve.
     * @return A Flow emitting the [Expense] object if found, or `null` if no expense with the given ID exists.
     */
    operator fun invoke(id: Long): Flow<Expense?> {
        return repository.getExpenseById(id)
    }
}
