package com.yourssohail.smartdailyexpensetracker.data.model

import androidx.compose.ui.graphics.Color

enum class CategoryType(val color: Color) {
    STAFF(color = Color(0xFFFF9800)),
    TRAVEL(color = Color(0xFF2196F3)),
    FOOD(color = Color(0xFF4CAF50)),
    UTILITY(color = Color(0xFF9C27B0))
}
