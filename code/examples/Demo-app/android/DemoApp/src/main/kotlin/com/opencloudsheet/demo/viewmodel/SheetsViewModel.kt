package com.opencloudsheet.demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencloudsheet.OpenCloudSheetSdk
import com.opencloudsheet.Provider
import com.opencloudsheet.demo.model.Expense
import com.opencloudsheet.model.metadata.SheetMetadata
import com.opencloudsheet.model.workbook.IWorkBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing worksheets within a workbook.
 */
class SheetsViewModel(
    private val workbookId: String,
    private val workbookName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<SheetsUiState>(SheetsUiState.Loading)
    val uiState: StateFlow<SheetsUiState> = _uiState.asStateFlow()

    private var workbook: IWorkBook? = null

    init {
        loadWorkBook()
    }

    private fun loadWorkBook() {
        viewModelScope.launch {
            try {
                _uiState.value = SheetsUiState.Loading

                // Get workbook from metadata
                val workbooks = OpenCloudSheetSdk.getWorkBooks(Provider.OneDrive)
                val foundWorkbook = workbooks.find { it.getId() == workbookId }
                    ?: throw IllegalStateException("Workbook not found")

                workbook = foundWorkbook

                loadSheets()
            } catch (e: Exception) {
                _uiState.value = SheetsUiState.Error("Failed to load worksheet: ${e.message}")
            }
        }
    }

    private fun loadSheets() {
        viewModelScope.launch {
            try {
                if (workbook == null) {
                    _uiState.value = SheetsUiState.Error("Workbook not initialized")
                    return@launch
                }

                val sheets = workbook!!.getSheets()
                _uiState.value = SheetsUiState.Success(
                    workbookName = workbookName,
                    sheets = sheets
                )
            } catch (e: Exception) {
                _uiState.value = SheetsUiState.Error("Failed to load sheets: ${e.message}")
            }
        }
    }

    fun createSheet(month: String, year: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = SheetsUiState.Creating

                if (workbook == null) {
                    _uiState.value = SheetsUiState.Error("Workbook not initialized")
                    return@launch
                }

                val sheetName = "$month $year"
                workbook!!.createSheet(
                    type = Expense::class.java,
                    name = sheetName,
                    description = "Expenses for $sheetName"
                )

                // Reload sheets
                loadSheets()
            } catch (e: Exception) {
                _uiState.value = SheetsUiState.Error("Failed to create sheet: ${e.message}")
            }
        }
    }

    fun deleteSheet(sheet: SheetMetadata) {
        viewModelScope.launch {
            try {
                if (workbook == null) {
                    _uiState.value = SheetsUiState.Error("Workbook not initialized")
                    return@launch
                }

                val currentState = _uiState.value
                if (currentState is SheetsUiState.Success) {
                    // Optimistically remove from UI
                    val updatedList = currentState.sheets.filter { it.sheetName != sheet.sheetName }
                    _uiState.value = SheetsUiState.Success(
                        workbookName = currentState.workbookName,
                        sheets = updatedList
                    )

                    // Delete from backend
                    workbook!!.deleteSheet(sheet.sheetName)
                }
            } catch (e: Exception) {
                _uiState.value = SheetsUiState.Error("Failed to delete sheet: ${e.message}")
                loadSheets()
            }
        }
    }

}

sealed class SheetsUiState {
    object Loading : SheetsUiState()
    object Creating : SheetsUiState()
    data class Success(
        val workbookName: String,
        val sheets: List<SheetMetadata>
    ) : SheetsUiState()
    data class Error(val message: String) : SheetsUiState()
}
