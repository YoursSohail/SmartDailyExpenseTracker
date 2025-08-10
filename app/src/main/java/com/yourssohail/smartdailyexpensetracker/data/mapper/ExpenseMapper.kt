package com.yourssohail.smartdailyexpensetracker.data.mapper

import com.yourssohail.smartdailyexpensetracker.data.local.entity.ExpenseEntity as DataExpenseEntity // Updated import
import com.yourssohail.smartdailyexpensetracker.domain.model.Expense
import kotlin.collections.map

fun DataExpenseEntity.toDomain(): Expense {
    return Expense(
        id = this.id,
        title = this.title,
        amount = this.amount,
        category = this.category,
        date = this.date,
        notes = this.notes,
        receiptImagePath = this.receiptImagePath
    )
}

fun Expense.toDataEntity(): DataExpenseEntity {
    return DataExpenseEntity(
        id = this.id, // If id is 0L (domain default for new), it will be 0 for Room for autoGenerate.
        title = this.title,
        amount = this.amount,
        category = this.category,
        date = this.date, // The data entity has a default for 'date' if not provided.
        notes = this.notes,
        receiptImagePath = this.receiptImagePath
    )
}

fun List<DataExpenseEntity>.toDomainModels(): List<Expense> {
    return this.map { it.toDomain() }
}

fun List<Expense>.toDataEntities(): List<DataExpenseEntity> {
    return this.map { it.toDataEntity() }
}
