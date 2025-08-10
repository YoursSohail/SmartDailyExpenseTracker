package com.yourssohail.smartdailyexpensetracker.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats a given timestamp into a date string based on the provided pattern.
 *
 * @param timestamp The time in milliseconds since the epoch.
 * @param pattern The date pattern (e.g., "dd MMM yyyy", "yyyy-MM-dd HH:mm:ss").
 * @param locale The locale to use for formatting. Defaults to `Locale.getDefault()`.
 * @return The formatted date string.
 */
fun formatDate(timestamp: Long, pattern: String, locale: Locale = Locale.getDefault()): String {
    val sdf = SimpleDateFormat(pattern, locale)
    return sdf.format(Date(timestamp))
}

/**
 * Currency formatter for Indian Rupees (INR).
 * Example: ₹1,234.56
 */
internal val CURRENCY_FORMATTER_INR: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))


object DatePatterns {
    const val DAY_DATE = "EEE, dd MMM yyyy"
    const val SHORT_COMPONENTS = "dd MMM yyyy"
    const val FULL_DISPLAY_WITH_TIME = "EEE, dd MMM yyyy, hh:mm a"
    const val MEDIUM_DATETIME_DISPLAY = "dd MMM yyyy, hh:mm a"
    const val REPORT_TIMESTAMP = "yyyy-MM-dd HH:mm:ss"
    const val CSV_DATE = "dd/MM/yyyy"
    const val FILE_TIMESTAMP = "yyyyMMdd_HHmmss"
    const val SHORT_DATE_REPORTS = "dd/MM"
}
