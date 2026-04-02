package com.opencloudsheet.demo.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencloudsheet.OpenCloudSheetSdk
import com.opencloudsheet.Provider
import com.opencloudsheet.demo.model.Expense
import com.opencloudsheet.model.worksheet.IWorkSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing expenses within a worksheet.
 */
class ExpensesViewModel(
    private val workbookId: String,
    private val sheetName: String
) : ViewModel() {

    companion object {
        private const val TAG = "ExpensesViewModel"
    }

    private val _uiState = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    private var sheet: IWorkSheet<Expense>? = null

    init {
        loadWorkbookAndSheet()
    }

    private fun loadWorkbookAndSheet() {
        viewModelScope.launch {
            try {
                _uiState.value = ExpensesUiState.Loading

                // Get workbook from metadata
                val workbooks = OpenCloudSheetSdk.getWorkBooks(Provider.OneDrive)
                val workbook = workbooks.find { it.getId() == workbookId }
                    ?: throw IllegalStateException("Workbook not found")

                // Get the sheet by name
                val foundSheet = workbook.getSheet(sheetName, Expense::class.java)
                    ?: throw IllegalStateException("Sheet not found")

                sheet = foundSheet
                loadExpenses()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load workbook and sheet", e)
                _uiState.value = ExpensesUiState.Error("Failed to load sheet: ${e.message}")
            }
        }
    }

    fun loadExpenses() {
        viewModelScope.launch {
            try {
                val currentSheet = sheet
                if (currentSheet == null) {
                    _uiState.value = ExpensesUiState.Error("Sheet not initialized")
                    return@launch
                }

                _uiState.value = ExpensesUiState.Loading
                val expenses = currentSheet.get()
                Log.d(TAG, "Loaded ${expenses.size} expenses from sheet: $sheetName")
                _uiState.value = ExpensesUiState.Success(
                    sheetName = sheetName,
                    expenses = expenses
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load expenses", e)
                _uiState.value = ExpensesUiState.Error("Failed to load expenses: ${e.message}")
            }
        }
    }

    fun createExpense(name: String, amount: Double, currency: String) {
        viewModelScope.launch {
            try {
                val currentSheet = sheet
                if (currentSheet == null) {
                    _uiState.value = ExpensesUiState.Error("Sheet not initialized")
                    return@launch
                }

                _uiState.value = ExpensesUiState.Creating
                val expense = Expense(
                    name = name,
                    amount = amount,
                    currency = currency
                )
                currentSheet.create(expense)
                Log.d(TAG, "Created expense: $name")
                loadExpenses()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create expense", e)
                _uiState.value = ExpensesUiState.Error("Failed to create expense: ${e.message}")
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                val currentSheet = sheet
                if (currentSheet == null) {
                    _uiState.value = ExpensesUiState.Error("Sheet not initialized")
                    return@launch
                }

                _uiState.value = ExpensesUiState.Updating
                currentSheet.update(expense)
                Log.d(TAG, "Updated expense: ${expense.name}")
                loadExpenses()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update expense", e)
                _uiState.value = ExpensesUiState.Error("Failed to update expense: ${e.message}")
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                val currentSheet = sheet
                if (currentSheet == null) {
                    _uiState.value = ExpensesUiState.Error("Sheet not initialized")
                    return@launch
                }

                _uiState.value = ExpensesUiState.Deleting
                currentSheet.delete(expense)
                Log.d(TAG, "Deleted expense: ${expense.name}")
                loadExpenses()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete expense", e)
                _uiState.value = ExpensesUiState.Error("Failed to delete expense: ${e.message}")
            }
        }
    }
}

sealed class ExpensesUiState {
    object Loading : ExpensesUiState()
    object Creating : ExpensesUiState()
    object Updating : ExpensesUiState()
    object Deleting : ExpensesUiState()
    data class Success(
        val sheetName: String,
        val expenses: List<Expense>
    ) : ExpensesUiState()
    data class Error(val message: String) : ExpensesUiState()
}
