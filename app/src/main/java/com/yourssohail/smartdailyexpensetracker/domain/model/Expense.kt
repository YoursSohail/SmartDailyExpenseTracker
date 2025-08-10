package com.yourssohail.smartdailyexpensetracker.domain.model

data class Expense(
    val id: Long = 0L, // Default for new, unpersisted entities
    val title: String,
    val amount: Double,
    val category: String,
    val date: Long,
    val notes: String? = null,
    val receiptImagePath: String? = null
)
