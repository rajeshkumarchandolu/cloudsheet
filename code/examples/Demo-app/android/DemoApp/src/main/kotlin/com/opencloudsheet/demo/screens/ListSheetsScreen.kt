package com.opencloudsheet.demo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencloudsheet.demo.model.Expense
import com.opencloudsheet.demo.ui.components.SwipeToDeleteItem
import com.opencloudsheet.demo.viewmodel.SheetsUiState
import com.opencloudsheet.demo.viewmodel.SheetsViewModel
import com.opencloudsheet.model.worksheet.IWorkSheet
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSheetsScreen(
    workbookId: String,
    workbookName: String,
    onNavigateBack: () -> Unit,
    onSheetSelected: (String, String) -> Unit,
    viewModel: SheetsViewModel = viewModel(
        factory = SheetsViewModelFactory(workbookId, workbookName)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var sheetToRename by remember { mutableStateOf<IWorkSheet<Expense>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workbookName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Sheet")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is SheetsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is SheetsUiState.Creating -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is SheetsUiState.Success -> {
                    if (state.sheets.isEmpty()) {
                        EmptySheetsView(
                            onCreateClick = { showCreateDialog = true },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        SheetsList(
                            sheets = state.sheets,
                            onSheetClick = { sheet ->
                                onSheetSelected(sheet.getId(), sheet.getName())
                            },
                            onSheetRename = { sheetToRename = it },
                            onSheetDelete = { viewModel.deleteSheet(it) }
                        )
                    }
                }

                is SheetsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateSheetDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { month, year ->
                    viewModel.createSheet(month, year)
                    showCreateDialog = false
                }
            )
        }

        sheetToRename?.let { sheet ->
            RenameSheetDialog(
                sheet = sheet,
                onDismiss = { sheetToRename = null },
                onRename = { newName ->
                    viewModel.renameSheet(sheet, newName)
                    sheetToRename = null
                }
            )
        }
    }
}

@Composable
fun SheetsList(
    sheets: List<IWorkSheet<Expense>>,
    onSheetClick: (IWorkSheet<Expense>) -> Unit,
    onSheetRename: (IWorkSheet<Expense>) -> Unit,
    onSheetDelete: (IWorkSheet<Expense>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sheets, key = { it.getId() }) { sheet ->
            SwipeToDeleteItem(
                onDelete = { onSheetDelete(sheet) },
                onEdit = { onSheetRename(sheet) },
                showEdit = true
            ) {
                SheetCard(
                    sheet = sheet,
                    onClick = { onSheetClick(sheet) }
                )
            }
        }
    }
}

@Composable
fun SheetCard(
    sheet: IWorkSheet<Expense>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = sheet.getName(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ID: ${sheet.getId()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptySheetsView(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No Sheets Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first month tracking sheet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Sheet")
        }
    }
}

@Composable
fun RenameSheetDialog(
    sheet: IWorkSheet<Expense>,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(sheet.getName()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Sheet") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Sheet Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onRename(name.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSheetDialog(
    onDismiss: () -> Unit,
    onCreate: (month: String, year: Int) -> Unit
) {
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear - 5..currentYear + 5).toList()

    var selectedMonth by remember { mutableStateOf(months[Calendar.getInstance().get(Calendar.MONTH)]) }
    var selectedYear by remember { mutableStateOf(currentYear) }
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Month Tracking Sheet") },
        text = {
            Column {
                // Month dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedMonth,
                    onExpandedChange = { expandedMonth = it }
                ) {
                    OutlinedTextField(
                        value = selectedMonth,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Month") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMonth) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedMonth,
                        onDismissRequest = { expandedMonth = false }
                    ) {
                        months.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(month) },
                                onClick = {
                                    selectedMonth = month
                                    expandedMonth = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Year dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedYear,
                    onExpandedChange = { expandedYear = it }
                ) {
                    OutlinedTextField(
                        value = selectedYear.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Year") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYear) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedYear,
                        onDismissRequest = { expandedYear = false }
                    ) {
                        years.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    selectedYear = year
                                    expandedYear = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(selectedMonth, selectedYear)
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ViewModelFactory for SheetsViewModel
class SheetsViewModelFactory(
    private val workbookId: String,
    private val workbookName: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SheetsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SheetsViewModel(workbookId, workbookName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
