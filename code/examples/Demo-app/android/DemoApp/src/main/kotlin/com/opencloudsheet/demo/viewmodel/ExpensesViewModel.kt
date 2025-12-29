package com.opencloudsheet.demo.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val sheet: IWorkSheet<Expense>,
    private val sheetName: String
) : ViewModel() {

    companion object {
        private const val TAG = "ExpensesViewModel"
    }

    private val _uiState = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        loadExpenses()
    }

    fun loadExpenses() {
        viewModelScope.launch {
            try {
                _uiState.value = ExpensesUiState.Loading
                val expenses = sheet.get()
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
                _uiState.value = ExpensesUiState.Creating
                val expense = Expense(
                    name = name,
                    amount = amount,
                    currency = currency
                )
                sheet.create(expense)
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
                _uiState.value = ExpensesUiState.Updating
                sheet.update(expense)
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
                _uiState.value = ExpensesUiState.Deleting
                sheet.delete(expense)
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
