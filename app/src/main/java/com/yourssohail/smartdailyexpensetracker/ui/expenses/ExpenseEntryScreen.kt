package com.yourssohail.smartdailyexpensetracker.ui.expenses

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.ui.common.ProgressButton
import com.yourssohail.smartdailyexpensetracker.ui.common.SectionTitle
import com.yourssohail.smartdailyexpensetracker.utils.DatePatterns
import com.yourssohail.smartdailyexpensetracker.utils.formatDate
import java.io.File
import java.io.FileInputStream
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEntryScreen(
    viewModel: ExpenseEntryViewModel = hiltViewModel(),
    onExpenseSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current // For Toast

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            var displayName: String? = null
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
            }
            viewModel.onReceiptImageSelected(it.toString(), displayName ?: "Selected_Image")
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ExpenseEntryEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                is ExpenseEntryEvent.ExpenseSaved -> {
                    onExpenseSaved()
                }
            }
        }
    }

    ExpenseEntryScreenContent(
        uiState = uiState,
        categories = viewModel.categories,
        onTitleChange = viewModel::onTitleChange,
        onAmountChange = viewModel::onAmountChange,
        onCategoryChange = viewModel::onCategoryChange,
        onDateChange = viewModel::onDateChange,
        onNotesChange = viewModel::onNotesChange,
        onRemoveReceiptImage = viewModel::onRemoveReceiptImage,
        onSaveExpense = viewModel::saveExpense,
        onForceSaveExpense = viewModel::forceSaveExpense,
        onDismissDuplicateWarning = viewModel::dismissDuplicateWarning,
        onCloseScreen = onExpenseSaved, // Top bar close icon action
        onLaunchImagePicker = { imagePickerLauncher.launch("image/*") }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseEntryScreenContent(
    uiState: ExpenseEntryUiState,
    categories: List<CategoryType>,
    onTitleChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (CategoryType) -> Unit,
    onDateChange: (Long) -> Unit,
    onNotesChange: (String) -> Unit,
    onRemoveReceiptImage: () -> Unit,
    onSaveExpense: () -> Unit,
    onForceSaveExpense: () -> Unit,
    onDismissDuplicateWarning: () -> Unit,
    onCloseScreen: () -> Unit,
    onLaunchImagePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current // For DatePickerDialog and Image loading

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditMode) "Edit Expense" else "Add New Expense")
                },
                navigationIcon = {
                    IconButton(onClick = onCloseScreen) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Total Spent Today: ${uiState.totalSpentToday}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                isError = uiState.titleError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.titleError != null) {
                Text(
                    uiState.titleError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = onAmountChange,
                label = { Text("Amount (₹)") },
                isError = uiState.amountError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.amountError != null) {
                Text(
                    uiState.amountError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.selectedCategory?.name ?: "Select Category",
                    onValueChange = { /* Read only */ },
                    label = { Text("Category") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    isError = uiState.categoryError != null
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                onCategoryChange(category)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            if (uiState.categoryError != null) {
                Text(
                    uiState.categoryError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = uiState.date
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                context,
                { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                    val newCal = Calendar.getInstance()
                        .apply { set(selectedYear, selectedMonth, selectedDayOfMonth) }
                    onDateChange(newCal.timeInMillis)
                }, year, month, day
            ).apply { datePicker.maxDate = System.currentTimeMillis() }

            OutlinedTextField(
                value = formatDate(uiState.date, DatePatterns.CSV_DATE),
                onValueChange = {},
                label = { Text("Date") },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        Icons.Default.DateRange,
                        "Select Date",
                        Modifier.clickable { datePickerDialog.show() })
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { newText ->
                    if (newText.length <= 100) {
                        onNotesChange(newText)
                    }
                },
                label = { Text("Optional Notes") },
                isError = uiState.notesError != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 3,
                supportingText = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${uiState.notes.length}/100",
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (uiState.notesError != null) {
                            Text(
                                text = uiState.notesError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            )

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                SectionTitle(
                    text = "Receipt Image (Optional)",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))

                if (uiState.selectedReceiptUri == null) {
                    OutlinedButton(
                        onClick = onLaunchImagePicker,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Add Receipt Icon",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Add Receipt Image")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val imageBitmap: ImageBitmap? = remember(uiState.selectedReceiptUri) {
                            uiState.selectedReceiptUri?.let { uriString ->
                                try {
                                    val uri = Uri.parse(uriString)
                                    if (uriString.startsWith("content://")) {
                                        context.contentResolver.openInputStream(uri)
                                            ?.use { inputStream ->
                                                BitmapFactory.decodeStream(inputStream)
                                                    ?.asImageBitmap()
                                            }
                                    } else {
                                        val file = File(uriString)
                                        if (file.exists()) {
                                            FileInputStream(file).use { inputStream ->
                                                BitmapFactory.decodeStream(inputStream)
                                                    ?.asImageBitmap()
                                            }
                                        } else null
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    null
                                }
                            }
                        }
                        imageBitmap?.let {
                            Image(
                                bitmap = it,
                                contentDescription = "Receipt Preview",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Text(
                            text = uiState.receiptFileName ?: "Selected Image",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        IconButton(onClick = onRemoveReceiptImage) {
                            Icon(Icons.Filled.Clear, contentDescription = "Remove Receipt Image")
                        }
                    }
                }

                if (uiState.receiptImageError != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        uiState.receiptImageError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (uiState.isDuplicateWarningVisible) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Possible Duplicate!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "An expense with similar details already exists. Are you sure you want to save this one?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            OutlinedButton(onClick = onDismissDuplicateWarning) {
                                Text("Cancel")
                            }
                            androidx.compose.material3.Button(onClick = onForceSaveExpense) {
                                Text("Save Anyway")
                            }
                        }
                    }
                }
            }

            ProgressButton(
                text = if (uiState.isEditMode) "Update Expense" else "Save Expense",
                onClick = onSaveExpense,
                isLoading = uiState.isLoading,
                enabled = uiState.isSaveEnabled && !uiState.isDuplicateWarningVisible,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Expense Entry Add Mode")
@Composable
fun ExpenseEntryScreenPreview() {
    MaterialTheme {
        ExpenseEntryScreenContent(
            uiState = ExpenseEntryUiState(
                title = "Lunch",
                amount = "120.50",
                selectedCategory = CategoryType.FOOD,
                notes = "Had a great lunch with colleagues.",
                date = System.currentTimeMillis(),
                totalSpentToday = "₹550.75",
                isEditMode = false,
                receiptFileName = "receipt_lunch.jpg",
            ),
            categories = CategoryType.entries.toList(),
            onTitleChange = {},
            onAmountChange = {},
            onCategoryChange = {},
            onDateChange = {},
            onNotesChange = {},
            onRemoveReceiptImage = {},
            onSaveExpense = {},
            onForceSaveExpense = {},
            onDismissDuplicateWarning = {},
            onCloseScreen = {},
            onLaunchImagePicker = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Expense Entry Edit Mode with Duplicate Warning")
@Composable
fun ExpenseEntryScreenEditModePreview() {
    MaterialTheme {
        ExpenseEntryScreenContent(
            uiState = ExpenseEntryUiState(
                title = "Dinner",
                amount = "300.00",
                selectedCategory = CategoryType.FOOD,
                notes = "Team dinner",
                date = System.currentTimeMillis() - (1000 * 60 * 60 * 24), // Yesterday
                totalSpentToday = "₹550.75",
                isEditMode = true,
                isDuplicateWarningVisible = true
            ),
            categories = CategoryType.entries.toList(),
            onTitleChange = {},
            onAmountChange = {},
            onCategoryChange = {},
            onDateChange = {},
            onNotesChange = {},
            onRemoveReceiptImage = {},
            onSaveExpense = {},
            onForceSaveExpense = {},
            onDismissDuplicateWarning = {},
            onCloseScreen = {},
            onLaunchImagePicker = {}
        )
    }
}
