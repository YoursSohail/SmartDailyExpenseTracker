package com.yourssohail.smartdailyexpensetracker.ui.expenses

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssohail.smartdailyexpensetracker.data.local.model.Expense
import com.yourssohail.smartdailyexpensetracker.data.model.CategoryType
import com.yourssohail.smartdailyexpensetracker.data.repository.ExpenseRepository
import com.yourssohail.smartdailyexpensetracker.domain.usecase.AddExpenseUseCase
import com.yourssohail.smartdailyexpensetracker.domain.usecase.DetectDuplicateExpenseUseCase
import com.yourssohail.smartdailyexpensetracker.domain.usecase.GetExpenseByIdUseCase
import com.yourssohail.smartdailyexpensetracker.domain.usecase.ValidateExpenseUseCase
import com.yourssohail.smartdailyexpensetracker.domain.usecase.ValidationResult
import com.yourssohail.smartdailyexpensetracker.ui.navigation.AppDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class ExpenseEntryUiState(
    val title: String = "",
    val amount: String = "",
    val selectedCategory: CategoryType? = null,
    val notes: String = "",
    val date: Long = System.currentTimeMillis(),

    val titleError: String? = null,
    val amountError: String? = null,
    val categoryError: String? = null,
    val notesError: String? = null,

    val isLoading: Boolean = false,
    val isDuplicateWarningVisible: Boolean = false,

    val selectedReceiptUri: String? = null,
    val receiptFileName: String? = null,
    val receiptImageError: String? = null,
    val existingReceiptPath: String? = null,

    val currentExpenseId: Long? = null,
    val isEditMode: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val totalSpentToday: String = "₹0.00"
)

sealed class ExpenseEntryEvent {
    data class ShowToast(val message: String) : ExpenseEntryEvent()
    object ExpenseSaved : ExpenseEntryEvent()
}

@HiltViewModel
class ExpenseEntryViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val validateExpenseUseCase: ValidateExpenseUseCase,
    private val detectDuplicateExpenseUseCase: DetectDuplicateExpenseUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val expenseRepository: ExpenseRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseEntryUiState())
    val uiState: StateFlow<ExpenseEntryUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ExpenseEntryEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val categories: List<CategoryType> = CategoryType.values().toList()

    private val indianCurrencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    init {
        val expenseId = savedStateHandle.get<Long>(AppDestinations.EXPENSE_ID_ARG)
        if (expenseId != null && expenseId != -1L) {
            _uiState.update { it.copy(isLoading = true, isEditMode = true, currentExpenseId = expenseId) }
            viewModelScope.launch {
                val expense = getExpenseByIdUseCase(expenseId).firstOrNull()
                if (expense != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = expense.title,
                            amount = expense.amount.toString(),
                            selectedCategory = try { CategoryType.valueOf(expense.category) } catch (e: IllegalArgumentException) { null },
                            notes = expense.notes ?: "",
                            date = expense.date,
                            selectedReceiptUri = expense.receiptImagePath,
                            receiptFileName = expense.receiptImagePath?.let { File(it).name },
                            existingReceiptPath = expense.receiptImagePath
                        )
                    }
                    updateSaveButtonStatus()
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ExpenseEntryEvent.ShowToast("Error: Could not load expense to edit."))
                    updateSaveButtonStatus()
                }
            }
        } else {
            updateSaveButtonStatus()
        }
        observeTotalSpentToday()
    }

    private fun observeTotalSpentToday() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDayMillis = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDayMillis = calendar.timeInMillis

            expenseRepository.getTotalSpentOnDate(startOfDayMillis, endOfDayMillis)
                .collectLatest { total ->
                    val formattedTotal = indianCurrencyFormat.format(total ?: 0.0)
                    _uiState.update { it.copy(totalSpentToday = formattedTotal) }
                }
        }
    }

    private fun updateSaveButtonStatus() {
        val state = _uiState.value
        val isTitleValid = state.title.isNotBlank() && state.titleError == null
        val isAmountValid = state.amount.toDoubleOrNull()?.let { it > 0.0 } ?: false && state.amountError == null
        val isCategoryValid = state.selectedCategory != null && state.categoryError == null
        _uiState.update {
            it.copy(isSaveEnabled = isTitleValid && isAmountValid && isCategoryValid)
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, titleError = null, isDuplicateWarningVisible = false) }
        updateSaveButtonStatus()
    }

    fun onAmountChange(newAmount: String) {
        if (newAmount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(amount = newAmount, amountError = null, isDuplicateWarningVisible = false) }
        } else if (newAmount.isEmpty()){
             _uiState.update { it.copy(amount = "", amountError = null, isDuplicateWarningVisible = false) }
        }
        updateSaveButtonStatus()
    }

    fun onCategoryChange(category: CategoryType) {
        _uiState.update { it.copy(selectedCategory = category, categoryError = null) }
        updateSaveButtonStatus()
    }

    fun onNotesChange(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes, notesError = null) }
    }

    fun onDateChange(newDate: Long) {
        _uiState.update { it.copy(date = newDate, isDuplicateWarningVisible = false) }
    }

    fun onReceiptImageSelected(uriString: String?, fileName: String?) {
        _uiState.update { it.copy(selectedReceiptUri = uriString, receiptFileName = fileName, receiptImageError = null) }
    }

    fun onRemoveReceiptImage() {
        _uiState.update { it.copy(selectedReceiptUri = null, receiptFileName = null, receiptImageError = null) }
    }

    private fun validateInputs(): Boolean {
        val titleResult = validateExpenseUseCase.validateTitle(uiState.value.title)
        val amountDouble = uiState.value.amount.toDoubleOrNull()
        val amountResult = validateExpenseUseCase.validateAmount(amountDouble)
        val notesResult = validateExpenseUseCase.validateNotes(uiState.value.notes)

        val newTitleError = if (titleResult is ValidationResult.Error) titleResult.message else null
        val newAmountError = if (amountResult is ValidationResult.Error) amountResult.message else null
        val newCategoryError = if (uiState.value.selectedCategory == null) "Category must be selected." else null
        val newNotesError = if (notesResult is ValidationResult.Error) notesResult.message else null

        _uiState.update {
            it.copy(
                titleError = newTitleError,
                amountError = newAmountError,
                categoryError = newCategoryError,
                notesError = newNotesError,
                receiptImageError = null
            )
        }
        updateSaveButtonStatus()
        return newTitleError == null && newAmountError == null && newCategoryError == null && newNotesError == null
    }

    private suspend fun saveReceiptImageToInternalStorage(uriString: String): String? {
        if (File(uriString).exists() && uriString.startsWith(applicationContext.filesDir.absolutePath)) {
            return uriString
        }
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = applicationContext.contentResolver.openInputStream(Uri.parse(uriString))
                val receiptsDir = File(applicationContext.filesDir, "receipts")
                if (!receiptsDir.exists()) { receiptsDir.mkdirs() }
                val extension = getFileExtension(applicationContext, Uri.parse(uriString)) ?: "jpg"
                val fileName = "${UUID.randomUUID()}.$extension"
                val outputFile = File(receiptsDir, fileName)

                inputStream.use { input -> FileOutputStream(outputFile).use { output -> input?.copyTo(output) } }
                outputFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(receiptImageError = "Failed to save receipt image.") }
                null
            }
        }
    }

    private fun getFileExtension(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)?.substringAfterLast('/')
    }

    fun saveExpense(forceSave: Boolean = false) {
        if (!validateInputs()) {
            return
        }
        _uiState.update { it.copy(isLoading = true, isDuplicateWarningVisible = false, receiptImageError = null) }

        viewModelScope.launch {
            val currentState = uiState.value
            var finalReceiptImagePath: String? = currentState.existingReceiptPath

            if (currentState.selectedReceiptUri != currentState.existingReceiptPath) {
                 if (currentState.selectedReceiptUri != null) {
                    finalReceiptImagePath = saveReceiptImageToInternalStorage(currentState.selectedReceiptUri)
                    if (finalReceiptImagePath == null) {
                        _uiState.update { it.copy(isLoading = false) }
                        updateSaveButtonStatus()
                        return@launch
                    }
                } else {
                    finalReceiptImagePath = null
                }
            }

            val title = currentState.title
            val amount = currentState.amount.toDoubleOrNull() ?: 0.0
            val category = currentState.selectedCategory!!.name
            val date = currentState.date
            val notes = currentState.notes.takeIf { it.isNotBlank() }

            if (!forceSave) {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = date
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val dateForDuplicateCheck = calendar.timeInMillis
                val existingExpense = detectDuplicateExpenseUseCase(title, amount, dateForDuplicateCheck).firstOrNull()

                if (existingExpense != null && (!currentState.isEditMode || existingExpense.id != currentState.currentExpenseId)) {
                     _uiState.update { it.copy(isLoading = false, isDuplicateWarningVisible = true) }
                    return@launch
                }
            }

            val expenseToSave = Expense(
                id = if (currentState.isEditMode) currentState.currentExpenseId!! else 0L,
                title = title,
                amount = amount,
                category = category,
                date = date,
                notes = notes,
                receiptImagePath = finalReceiptImagePath
            )

            try {
                addExpenseUseCase(expenseToSave)
                val message = if (currentState.isEditMode) "Expense updated!" else if (forceSave) "Expense saved (override)!" else "Expense saved!"
                _eventFlow.emit(ExpenseEntryEvent.ShowToast(message))
                _eventFlow.emit(ExpenseEntryEvent.ExpenseSaved)
                _uiState.update { it.copy(isLoading = false) }
                if (!currentState.isEditMode) resetFields() else updateSaveButtonStatus()
            } catch (e: Exception) {
                _eventFlow.emit(ExpenseEntryEvent.ShowToast("Error: ${e.message ?: "Failed to save/update"}"))
                _uiState.update { it.copy(isLoading = false) }
                updateSaveButtonStatus()
            }
        }
    }
    
    fun forceSaveExpense() {
        saveExpense(forceSave = true)
    }

    fun dismissDuplicateWarning() {
        _uiState.update { it.copy(isDuplicateWarningVisible = false) }
        updateSaveButtonStatus() // Re-check if the main save button can be enabled
    }

    fun resetFields() {
        _uiState.value = ExpenseEntryUiState(date = System.currentTimeMillis())
        updateSaveButtonStatus()
    }
}
