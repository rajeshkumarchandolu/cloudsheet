package com.opencloudsheet.demo.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opencloudsheet.demo.model.Expense
import com.opencloudsheet.demo.ui.components.SwipeToDeleteItem
import com.opencloudsheet.demo.viewmodel.ExpensesUiState
import com.opencloudsheet.demo.viewmodel.ExpensesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    workbookId: String,
    sheetId: String,
    sheetName: String,
    onNavigateBack: () -> Unit,
    viewModel: ExpensesViewModel = viewModel(
        factory = ExpensesViewModelFactory(workbookId, sheetName)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sheetName) },
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
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ExpensesUiState.Loading,
                is ExpensesUiState.Creating,
                is ExpensesUiState.Updating,
                is ExpensesUiState.Deleting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is ExpensesUiState.Success -> {
                    if (state.expenses.isEmpty()) {
                        EmptyExpensesView(
                            onCreateClick = { showCreateDialog = true },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        ExpensesList(
                            expenses = state.expenses,
                            onEditClick = { expenseToEdit = it },
                            onDeleteClick = { viewModel.deleteExpense(it) }
                        )
                    }
                }

                is ExpensesUiState.Error -> {
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
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.loadExpenses() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }

        // Create dialog
        if (showCreateDialog) {
            ExpenseDialog(
                title = "Add Expense",
                onDismiss = { showCreateDialog = false },
                onConfirm = { name, amount, currency ->
                    viewModel.createExpense(name, amount, currency)
                    showCreateDialog = false
                }
            )
        }

        // Edit dialog
        expenseToEdit?.let { expense ->
            ExpenseDialog(
                title = "Edit Expense",
                initialExpense = expense,
                onDismiss = { expenseToEdit = null },
                onConfirm = { name, amount, currency ->
                    expense.name = name
                    expense.amount = amount
                    expense.currency = currency
                    viewModel.updateExpense(expense)
                    expenseToEdit = null
                }
            )
        }
    }
}

@Composable
fun ExpensesList(
    expenses: List<Expense>,
    onEditClick: (Expense) -> Unit,
    onDeleteClick: (Expense) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Name",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.7f)
                    )
                    Spacer(modifier = Modifier.width(100.dp)) // Space for action buttons
                }
            }
        }

        // Expense items
        items(expenses, key = { it.getUniqueRowId() }) { expense ->
            SwipeToDeleteItem(
                onDelete = { onDeleteClick(expense) },
                onEdit = { onEditClick(expense) },
                showEdit = true
            ) {
                ExpenseCard(
                    expense = expense
                )
            }
        }
    }
}

@Composable
fun ExpenseCard(
    expense: Expense
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(modifier = Modifier.weight(0.7f)) {
                Text(
                    text = "${expense.currency} ${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmptyExpensesView(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No Expenses Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add your first expense to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Expense")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDialog(
    title: String,
    initialExpense: Expense? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, currency: String) -> Unit
) {
    var name by remember { mutableStateOf(initialExpense?.name ?: "") }
    var amount by remember { mutableStateOf(initialExpense?.amount?.toString() ?: "") }
    var currency by remember { mutableStateOf(initialExpense?.currency ?: "USD") }

    val currencies = listOf("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Expense Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Currency dropdown
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        currencies.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr) },
                                onClick = {
                                    currency = curr
                                    expanded = false
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
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amountValue > 0) {
                        onConfirm(name.trim(), amountValue, currency)
                    }
                },
                enabled = name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ViewModelFactory for ExpensesViewModel
class ExpensesViewModelFactory(
    private val workbookId: String,
    private val sheetName: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpensesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpensesViewModel(workbookId, sheetName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
