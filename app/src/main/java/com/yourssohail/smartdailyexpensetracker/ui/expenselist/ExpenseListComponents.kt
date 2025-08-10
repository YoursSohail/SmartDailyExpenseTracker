package com.yourssohail.smartdailyexpensetracker.ui.expenselist

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssohail.smartdailyexpensetracker.domain.model.Expense // Updated import
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.ui.common.EmptyStateView
import com.yourssohail.smartdailyexpensetracker.ui.common.FullScreenLoadingIndicator
import com.yourssohail.smartdailyexpensetracker.ui.common.ScreenErrorMessage
import com.yourssohail.smartdailyexpensetracker.ui.common.SectionTitle
import com.yourssohail.smartdailyexpensetracker.utils.CURRENCY_FORMATTER_INR
import com.yourssohail.smartdailyexpensetracker.utils.DatePatterns
import com.yourssohail.smartdailyexpensetracker.utils.formatDate
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.EnumMap
import java.util.Locale


@Composable
fun ExpenseDetailDialog(
    expense: Expense,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(expense.title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Amount
                Text(
                    text = CURRENCY_FORMATTER_INR.format(expense.amount),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Category
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Category,
                        contentDescription = "Category Icon",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Category: ${
                            expense.category.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    Locale.getDefault()
                                ) else it.toString()
                            }
                        }",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "Date Icon",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Date: ${formatDate(expense.date, DatePatterns.FULL_DISPLAY_WITH_TIME)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Notes
                if (expense.notes?.isNotBlank() == true) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Filled.Notes,
                            contentDescription = "Notes Icon",
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Notes:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = expense.notes,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Receipt Image
                expense.receiptImagePath?.let { path ->
                    if (path.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            "Receipt:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        val imageBitmap = remember(path) {
                            try {
                                val file = File(path)
                                if (file.exists()) {
                                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Receipt for ${expense.title}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = "Receipt image could not be loaded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = Modifier.padding(vertical = 16.dp)
    )
}


@Composable
private fun getTimeOfDayHeaderPresentation(timeOfDay: TimeOfDay): Pair<String, String> {
    return when (timeOfDay) {
        TimeOfDay.MORNING -> "🌅" to "Morning (12 AM - 11:59 AM)"
        TimeOfDay.AFTERNOON -> "☀️" to "Afternoon (12 PM - 4:59 PM)"
        TimeOfDay.EVENING -> "🌙" to "Evening (5 PM - 11:59 PM)"
    }
}

@Composable
private fun getCategoryHeaderPresentation(category: CategoryType): Pair<ImageVector, Color> {
    val icon = Icons.AutoMirrored.Filled.Label
    return icon to category.color
}


@Composable
internal fun rememberDatePickerDialog(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit
): DatePickerDialog {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    calendar.timeInMillis = initialDateMillis

    return remember(context, initialDateMillis) {
        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDayOfMonth)
                }
                onDateSelected(newCal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AnimatedExpenseListItem(
    expense: Expense,
    index: Int,
    onDeleteClick: (Expense) -> Unit,
    onEditClick: (Long) -> Unit,
    onItemClick: (Expense) -> Unit,
    isScrolling: Boolean,
    modifier: Modifier = Modifier
) {
    var targetVisible by remember(expense.id) { mutableStateOf(false) }
    LaunchedEffect(key1 = expense.id, key2 = isScrolling) {
        if (!targetVisible) {
            val delayMillis = if (isScrolling) 0L else index * 70L
            if (delayMillis > 0L) {
                kotlinx.coroutines.delay(delayMillis)
            }
            targetVisible = true
        }
    }

    AnimatedVisibility(
        visible = targetVisible,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth / 2 },
            animationSpec = tween(durationMillis = if (isScrolling) 0 else 350)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 300)
        ),
        modifier = modifier
    ) {
        ExpenseListItem(
            expense = expense,
            onDeleteClick = { onDeleteClick(expense) },
            onEditClick = { onEditClick(expense.id) },
            onItemClick = onItemClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ListContent(
    uiState: ExpenseListUiState,
    onDeleteExpense: (Expense) -> Unit,
    onNavigateToExpenseEdit: (expenseId: Long) -> Unit,
    onItemClick: (Expense) -> Unit,
    onRefresh: () -> Unit
) {
    val listState = rememberLazyListState()

    when {
        uiState.isLoading -> {
            FullScreenLoadingIndicator()
        }

        uiState.errorMessage != null -> {
            ScreenErrorMessage(message = uiState.errorMessage, onRetry = onRefresh)
        }

        uiState.expenses.isEmpty() && !uiState.isLoading -> {
            EmptyStateView(
                message = "No expenses recorded for ${
                    formatDate(
                        uiState.selectedDate,
                        DatePatterns.SHORT_COMPONENTS
                    )
                }.\\nTap the '+' button to add one!"
            )
        }

        else -> {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                if (uiState.groupBy == GroupByOption.TIME) {
                    if (uiState.timeOfDayGroupedExpenses.values.all { it.isEmpty() } && uiState.expenses.isNotEmpty() && !uiState.isLoading) {
                        item {
                            SectionTitle(
                                text = "Could not group expenses by time of day. Displaying as list:",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                        itemsIndexed(
                            uiState.expenses,
                            key = { _, expense -> "fallback-time-${expense.id}" }) { index, expense ->
                            AnimatedExpenseListItem(
                                expense,
                                index,
                                onDeleteExpense,
                                onNavigateToExpenseEdit,
                                onItemClick,
                                isScrolling = listState.isScrollInProgress
                            )
                        }
                    } else {
                        uiState.timeOfDayGroupedExpenses.forEach { (timeOfDay, expensesInTimeOfDay) ->
                            if (expensesInTimeOfDay.isNotEmpty()) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillParentMaxWidth(),
                                        color = Color.Transparent,
                                        shadowElevation = 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val (emoji, name) = getTimeOfDayHeaderPresentation(
                                                timeOfDay
                                            )
                                            Text(
                                                text = emoji,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            SectionTitle(
                                                text = name,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    }
                                }
                                itemsIndexed(
                                    expensesInTimeOfDay,
                                    key = { _, expense -> expense.id }) { index, expense ->
                                    AnimatedExpenseListItem(
                                        expense = expense,
                                        index = index,
                                        onDeleteClick = onDeleteExpense,
                                        onEditClick = onNavigateToExpenseEdit,
                                        onItemClick = onItemClick,
                                        isScrolling = listState.isScrollInProgress,
                                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                } else { // GroupByOption.CATEGORY
                    if (uiState.groupedExpenses.isEmpty() && uiState.expenses.isNotEmpty() && !uiState.isLoading) {
                        item {
                            SectionTitle(
                                text = "Could not group expenses by category. Displaying as list:",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                        itemsIndexed(
                            uiState.expenses,
                            key = { _, expense -> "fallback-category-${expense.id}" }) { index, expense ->
                            AnimatedExpenseListItem(
                                expense,
                                index,
                                onDeleteExpense,
                                onNavigateToExpenseEdit,
                                onItemClick,
                                isScrolling = listState.isScrollInProgress
                            )
                        }
                    } else {
                        uiState.groupedExpenses.forEach { (category, expensesInCategory) ->
                            if (expensesInCategory.isNotEmpty()) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillParentMaxWidth(),
                                        color = Color.Transparent,
                                        shadowElevation = 0.dp
                                    ) {
                                        val (iconVector, iconTint) = getCategoryHeaderPresentation(
                                            category
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = iconVector,
                                                contentDescription = category.name,
                                                modifier = Modifier.size(20.dp),
                                                tint = iconTint
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            SectionTitle(
                                                text = category.name.replaceFirstChar { nameChar ->
                                                    if (nameChar.isLowerCase()) nameChar.titlecase(
                                                        Locale.getDefault()
                                                    ) else nameChar.toString()
                                                },
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    color = iconTint
                                                )
                                            )
                                        }
                                    }
                                }
                                itemsIndexed(
                                    expensesInCategory,
                                    key = { _, expense -> expense.id }) { index, expense ->
                                    AnimatedExpenseListItem(
                                        expense = expense,
                                        index = index,
                                        onDeleteClick = onDeleteExpense,
                                        onEditClick = onNavigateToExpenseEdit,
                                        onItemClick = onItemClick,
                                        isScrolling = listState.isScrollInProgress,
                                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomGroupToggleSwitch(
    currentOption: GroupByOption,
    onOptionSelected: (GroupByOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackHeight = 32.dp
    val segmentWidth = 48.dp

    Row(
        modifier = modifier
            .height(trackHeight)
            .width(segmentWidth * 2)
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
    ) {
        // Left segment (Category)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (currentOption == GroupByOption.CATEGORY)
                        MaterialTheme.colorScheme.surface
                    else
                        Color.Transparent
                )
                .clickable { onOptionSelected(GroupByOption.CATEGORY) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Category,
                modifier = Modifier.size(20.dp),
                contentDescription = "Group by Category",
                tint = if (currentOption == GroupByOption.CATEGORY)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline)
        )

        // Right segment (Time)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (currentOption == GroupByOption.TIME)
                        MaterialTheme.colorScheme.surface
                    else
                        Color.Transparent
                )
                .clickable { onOptionSelected(GroupByOption.TIME) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = "Group by Time",
                modifier = Modifier.size(20.dp),
                tint = if (currentOption == GroupByOption.TIME)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}


@Composable
fun ExpenseListItem(
    expense: Expense,
    onDeleteClick: (Expense) -> Unit,
    onEditClick: (Long) -> Unit,
    onItemClick: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .clickable { onItemClick(expense) }, // Added clickable here
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = CURRENCY_FORMATTER_INR.format(expense.amount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = "Category",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = expense.category.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(
                                Locale.getDefault()
                            ) else it.toString()
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "Date",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDate(expense.date, DatePatterns.MEDIUM_DATETIME_DISPLAY),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (expense.notes?.isNotBlank() == true) {
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                expense.receiptImagePath?.let { path ->
                    if (path.isNotBlank()) {
                        val imageBitmap = remember(path) {
                            try {
                                val file = File(path)
                                if (file.exists()) {
                                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                        }

                        if (imageBitmap != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Receipt for ${expense.title}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Photo,
                                    contentDescription = "Receipt image not available",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Receipt image not found at path",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            Box {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options for expense: ${expense.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { showOptionsMenu = true }
                )
                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onEditClick(expense.id) // Changed to pass expense.id
                            showOptionsMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Expense")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showDeleteDialog = true
                            showOptionsMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Expense",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete the expense: \"${expense.title}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick(expense)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseDetailDialogPreview() {
    MaterialTheme {
        ExpenseDetailDialog(
            expense = Expense(
                id = 1L,
                title = "Detailed Conference Dinner",
                amount = 125.75,
                category = CategoryType.FOOD.name,
                date = System.currentTimeMillis() - (2 * 86400000), // 2 days ago
                notes = "This was a very long note about the conference dinner. It included several courses, discussions about future projects, and networking opportunities with key industry figures. The food was excellent, particularly the dessert which was a chocolate lava cake. The venue also had a great ambiance overlooking the city skyline.",
                receiptImagePath = null // Add a dummy image path for full preview if possible, or test with null
            ),
            onDismiss = {}
        )
    }
}

@Preview
@Composable
fun ExpenseListItemPreview() {
    val expense = Expense(
        title = "Lunch with colleagues",
        amount = 1250.75,
        category = "Food",
        date = System.currentTimeMillis() - 86400000, // Yesterday
        notes = "Team lunch at the new Italian place. Covered for John and Jane.",
        receiptImagePath = null
    )
    ExpenseListItem(
        expense = expense,
        onDeleteClick = {},
        onEditClick = {},
        onItemClick = {}
    )
}


@Preview(showBackground = true, name = "CustomGroupToggleSwitch - Time Selected")
@Composable
fun CustomGroupToggleSwitchTimeSelectedPreview() {
    MaterialTheme { // Wrap in MaterialTheme for previews to get proper styling
        var selectedOption by remember { mutableStateOf(GroupByOption.TIME) }
        CustomGroupToggleSwitch(
            currentOption = selectedOption,
            onOptionSelected = { selectedOption = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "CustomGroupToggleSwitch - Category Selected")
@Composable
fun CustomGroupToggleSwitchCategorySelectedPreview() {
    MaterialTheme {
        var selectedOption by remember { mutableStateOf(GroupByOption.CATEGORY) }
        CustomGroupToggleSwitch(
            currentOption = selectedOption,
            onOptionSelected = { selectedOption = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}


@Preview(showBackground = true, name = "Animated Expense Item")
@Composable
fun AnimatedExpenseListItemPreview() {
    AnimatedExpenseListItem(
        expense = Expense(
            id = 1,
            title = "Lunch Preview",
            amount = 12.75,
            category = CategoryType.FOOD.name,
            date = System.currentTimeMillis(),
            notes = "With colleagues"
        ),
        index = 0,
        onDeleteClick = {},
        onEditClick = {},
        onItemClick = {},
        isScrolling = false
    )
}

@Preview(showBackground = true, name = "ListContent - Loading")
@Composable
fun ListContentLoadingPreview() {
    ListContent(
        uiState = ExpenseListUiState(isLoading = true),
        onDeleteExpense = {},
        onNavigateToExpenseEdit = {},
        onItemClick = {},
        onRefresh = {})
}

@Preview(showBackground = true, name = "ListContent - Empty (Time Group)")
@Composable
fun ListContentEmptyTimePreview() {
    ListContent(
        uiState = ExpenseListUiState(
            expenses = emptyList(),
            timeOfDayGroupedExpenses = TimeOfDay.values().associateWith { emptyList<Expense>() },
            groupBy = GroupByOption.TIME,
            isLoading = false,
            selectedDate = System.currentTimeMillis()
        ),
        onDeleteExpense = {}, onNavigateToExpenseEdit = {}, onItemClick = {}, onRefresh = {}
    )
}

@Preview(showBackground = true, name = "ListContent - Empty (Category Group)")
@Composable
fun ListContentEmptyCategoryPreview() {
    ListContent(
        uiState = ExpenseListUiState(
            expenses = emptyList(),
            groupedExpenses = emptyMap(),
            groupBy = GroupByOption.CATEGORY,
            isLoading = false,
            selectedDate = System.currentTimeMillis()
        ),
        onDeleteExpense = {}, onNavigateToExpenseEdit = {}, onItemClick = {}, onRefresh = {}
    )
}

@Preview(showBackground = true, name = "ListContent - Data (Time of Day)")
@Composable
fun ListContentWithDataTimePreview() {
    val cal = Calendar.getInstance()
    val now = System.currentTimeMillis()

    val morningExpense = Expense(
        id = 1,
        title = "Coffee",
        amount = 4.50,
        category = CategoryType.FOOD.name,
        date = cal.apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 9) }.timeInMillis
    )
    val afternoonExpense = Expense(
        id = 2,
        title = "Lunch",
        amount = 12.50,
        category = CategoryType.FOOD.name,
        date = cal.apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 13) }.timeInMillis
    )
    val eveningExpense = Expense(
        id = 3,
        title = "Dinner",
        amount = 22.00,
        category = CategoryType.FOOD.name,
        date = cal.apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 19) }.timeInMillis
    )
    val anotherMorning = Expense(
        id = 4,
        title = "Breakfast Burrito",
        amount = 7.00,
        category = CategoryType.FOOD.name,
        date = cal.apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 8) }.timeInMillis
    )


    val allExpenses = listOf(morningExpense, afternoonExpense, eveningExpense, anotherMorning)
    val timeGrouped = EnumMap<TimeOfDay, List<Expense>>(TimeOfDay::class.java).apply {
        put(TimeOfDay.MORNING, listOf(morningExpense, anotherMorning))
        put(TimeOfDay.AFTERNOON, listOf(afternoonExpense))
        put(TimeOfDay.EVENING, listOf(eveningExpense))
    }

    ListContent(
        uiState = ExpenseListUiState(
            expenses = allExpenses,
            timeOfDayGroupedExpenses = timeGrouped,
            groupBy = GroupByOption.TIME,
            isLoading = false,
            totalExpenseCountForSelectedDate = allExpenses.size,
            totalSpentForSelectedDate = allExpenses.sumOf { it.amount }
        ),
        onDeleteExpense = {}, onNavigateToExpenseEdit = {}, onItemClick = {}, onRefresh = {}
    )
}

@Preview(showBackground = true, name = "ListContent - Data (Category)")
@Composable
fun ListContentWithDataCategoryPreview() {
    val cal = Calendar.getInstance()
    val now = System.currentTimeMillis()
    val expensesList = listOf(
        Expense(
            id = 1,
            title = "Pizza",
            amount = 20.0,
            category = CategoryType.FOOD.name,
            date = now
        ),
        Expense(
            id = 2,
            title = "Bus Ticket",
            amount = 2.50,
            category = CategoryType.TRAVEL.name,
            date = now - 10000
        ),
        Expense(
            id = 3,
            title = "Pens",
            amount = 5.0,
            category = CategoryType.STAFF.name,
            date = now - 20000
        ),
        Expense(
            id = 4,
            title = "Electricity Bill",
            amount = 50.0,
            category = CategoryType.UTILITY.name,
            date = now - 30000
        ),
        Expense(
            id = 5,
            title = "Groceries",
            amount = 30.0,
            category = CategoryType.FOOD.name,
            date = now - 40000
        )
    )
    val categoryGrouped =
        expensesList.groupBy { exp -> CategoryType.valueOf(exp.category.uppercase()) }
            .mapValues { entry -> entry.value.sortedByDescending { it.date } }

    ListContent(
        uiState = ExpenseListUiState(
            expenses = expensesList,
            groupedExpenses = categoryGrouped,
            groupBy = GroupByOption.CATEGORY,
            isLoading = false,
            totalExpenseCountForSelectedDate = expensesList.size,
            totalSpentForSelectedDate = expensesList.sumOf { it.amount }
        ),
        onDeleteExpense = {}, onNavigateToExpenseEdit = {}, onItemClick = {}, onRefresh = {}
    )
}

@Preview(showBackground = true, name = "ListContent - Error")
@Composable
fun ListContentErrorPreview() {
    ListContent(
        uiState = ExpenseListUiState(
            isLoading = false,
            errorMessage = "Failed to load expenses. Please try again."
        ),
        onDeleteExpense = {}, onNavigateToExpenseEdit = {}, onItemClick = {}, onRefresh = {}
    )
}

