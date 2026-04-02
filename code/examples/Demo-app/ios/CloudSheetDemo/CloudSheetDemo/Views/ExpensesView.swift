//
//  ExpensesView.swift
//  CloudSheetDemo
//
//  View for displaying and managing expenses
//

import SwiftUI
import OpenCloudSheet

struct ExpensesView: View {
    let workbookId: String
    let sheetName: String

    @StateObject private var viewModel: ExpensesViewModel
    @State private var showingCreateDialog = false
    @State private var showingEditDialog = false
    @State private var editingExpense: Expense?
    @State private var expenseName = ""
    @State private var expenseAmount = ""
    @State private var expenseCurrency = "USD"

    let currencies = ["USD", "EUR", "GBP", "INR", "JPY", "CNY"]

    init(workbookId: String, sheetName: String) {
        self.workbookId = workbookId
        self.sheetName = sheetName
        _viewModel = StateObject(wrappedValue: ExpensesViewModel(workbookId: workbookId, sheetName: sheetName))
    }

    var body: some View {
        ZStack {
            switch viewModel.uiState {
            case .loading:
                ProgressView("Loading expenses...")

            case .creating:
                ProgressView("Creating expense...")

            case .updating:
                ProgressView("Updating expense...")

            case .deleting:
                ProgressView("Deleting expense...")

            case .success(_, let expenses):
                if expenses.isEmpty {
                    VStack(spacing: 20) {
                        Text("No Expenses")
                            .font(.title2)
                            .foregroundColor(.secondary)
                        Text("Add your first expense")
                            .foregroundColor(.secondary)
                        Button("Add Expense") {
                            showingCreateDialog = true
                        }
                        .buttonStyle(.borderedProminent)
                    }
                } else {
                    List {
                        ForEach(Array(expenses.indices), id: \.self) { index in
                            let expense = expenses[index]
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(expense.name)
                                        .font(.headline)
                                    Text("Amount: \(expense.amount, specifier: "%.2f") \(expense.currency)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }

                                Spacer()

                                Button {
                                    editingExpense = expense
                                    expenseName = expense.name
                                    expenseAmount = String(expense.amount)
                                    expenseCurrency = expense.currency
                                    showingEditDialog = true
                                } label: {
                                    Image(systemName: "pencil")
                                        .foregroundColor(.blue)
                                }
                                .buttonStyle(.plain)
                            }
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    viewModel.deleteExpense(expense)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                        }
                    }
                }

            case .error(let message):
                VStack(spacing: 20) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 50))
                        .foregroundColor(.red)
                    Text("Error")
                        .font(.title2)
                    Text(message)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.secondary)
                    Button("Retry") {
                        viewModel.loadExpenses()
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding()
            }
        }
        .navigationTitle(sheetName)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    expenseName = ""
                    expenseAmount = ""
                    expenseCurrency = "USD"
                    showingCreateDialog = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showingCreateDialog) {
            ExpenseFormSheet(
                title: "Add Expense",
                expenseName: $expenseName,
                expenseAmount: $expenseAmount,
                expenseCurrency: $expenseCurrency,
                currencies: currencies,
                onSave: {
                    if let amount = Double(expenseAmount) {
                        viewModel.createExpense(name: expenseName, amount: amount, currency: expenseCurrency)
                    }
                    showingCreateDialog = false
                },
                onCancel: {
                    showingCreateDialog = false
                }
            )
        }
        .sheet(isPresented: $showingEditDialog) {
            ExpenseFormSheet(
                title: "Edit Expense",
                expenseName: $expenseName,
                expenseAmount: $expenseAmount,
                expenseCurrency: $expenseCurrency,
                currencies: currencies,
                onSave: {
                    if let amount = Double(expenseAmount), var expense = editingExpense {
                        expense.name = expenseName
                        expense.amount = amount
                        expense.currency = expenseCurrency
                        viewModel.updateExpense(expense)
                    }
                    showingEditDialog = false
                    editingExpense = nil
                },
                onCancel: {
                    showingEditDialog = false
                    editingExpense = nil
                }
            )
        }
    }
}

struct ExpenseFormSheet: View {
    let title: String
    @Binding var expenseName: String
    @Binding var expenseAmount: String
    @Binding var expenseCurrency: String
    let currencies: [String]
    let onSave: () -> Void
    let onCancel: () -> Void

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Expense Details")) {
                    TextField("Name", text: $expenseName)
                    TextField("Amount", text: $expenseAmount)
                        .keyboardType(.decimalPad)
                    Picker("Currency", selection: $expenseCurrency) {
                        ForEach(currencies, id: \.self) { currency in
                            Text(currency).tag(currency)
                        }
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onCancel()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onSave()
                    }
                    .disabled(expenseName.isEmpty || expenseAmount.isEmpty)
                }
            }
        }
    }
}
