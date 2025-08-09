package com.yourssohail.smartdailyexpensetracker.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String, // We'll link this to CategoryType enum later
    val date: Long = System.currentTimeMillis(), // Default to current time
    val notes: String? = null,
    val receiptImagePath: String? = null
)
