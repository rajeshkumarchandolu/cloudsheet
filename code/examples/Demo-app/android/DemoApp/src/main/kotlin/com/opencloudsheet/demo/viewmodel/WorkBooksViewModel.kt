package com.opencloudsheet.demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencloudsheet.OpenCloudSheetSdk
import com.opencloudsheet.Provider
import com.opencloudsheet.model.workbook.IWorkBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing workbooks list and creation.
 */
class WorkBooksViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<WorkBooksUiState>(WorkBooksUiState.Loading)
    val uiState: StateFlow<WorkBooksUiState> = _uiState.asStateFlow()

    init {
        loadWorkBooks()
    }

    fun loadWorkBooks() {
        viewModelScope.launch {
            try {
                _uiState.value = WorkBooksUiState.Loading
                val workbooks = OpenCloudSheetSdk.getWorkBooks(Provider.OneDrive)
                _uiState.value = WorkBooksUiState.Success(workbooks)
            } catch (e: Exception) {
                _uiState.value = WorkBooksUiState.Error("Failed to load workbooks: ${e.message}")
            }
        }
    }

    fun createWorkBook(name: String, description: String) {
        viewModelScope.launch {
            try {
                _uiState.value = WorkBooksUiState.Creating
                OpenCloudSheetSdk.createWorkBook(
                    provider = Provider.OneDrive,
                    workbookName = name,
                    description = description
                )
                // Reload workbooks to show the newly created one
                loadWorkBooks()
            } catch (e: Exception) {
                _uiState.value = WorkBooksUiState.Error("Failed to create workbook: ${e.message}")
            }
        }
    }

    fun updateWorkBook(workbook: IWorkBook, newName: String, newDescription: String) {
        viewModelScope.launch {
            try {
                val entry = workbook.getWorkBookEntry()
                val updatedEntry = entry.copy(
                    name = newName,
                    description = newDescription
                )

                // Update in backend
                OpenCloudSheetSdk.updateWorkBook(Provider.OneDrive, updatedEntry)

                // Reload to reflect changes
                loadWorkBooks()
            } catch (e: Exception) {
                _uiState.value = WorkBooksUiState.Error("Failed to update workbook: ${e.message}")
            }
        }
    }

    fun deleteWorkBook(workbook: IWorkBook) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is WorkBooksUiState.Success) {
                    // Optimistically remove from UI
                    val updatedList = currentState.workbooks.filter { it.getId() != workbook.getId() }
                    _uiState.value = WorkBooksUiState.Success(updatedList)

                    // Delete from backend
                    OpenCloudSheetSdk.deleteWorkBook(Provider.OneDrive, workbook.getWorkBookEntry())
                }
            } catch (e: Exception) {
                // Reload workbooks on error to restore state
                _uiState.value = WorkBooksUiState.Error("Failed to delete workbook: ${e.message}")
                loadWorkBooks()
            }
        }
    }
}

sealed class WorkBooksUiState {
    object Loading : WorkBooksUiState()
    object Creating : WorkBooksUiState()
    data class Success(val workbooks: List<IWorkBook>) : WorkBooksUiState()
    data class Error(val message: String) : WorkBooksUiState()
}
