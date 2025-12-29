//
//  SheetsView.swift
//  CloudSheetDemo
//
//  View for displaying and managing worksheets
//

import SwiftUI
import Combine
import OpenCloudSheet

struct SheetsView: View {
    let workbookId: String
    let workbookName: String

    @StateObject private var viewModel: SheetsViewModel
    @State private var showingCreateDialog = false
    @State private var selectedMonth = "January"
    @State private var selectedYear = Calendar.current.component(.year, from: Date())

    let months = ["January", "February", "March", "April", "May", "June",
                  "July", "August", "September", "October", "November", "December"]

    init(workbookId: String, workbookName: String) {
        self.workbookId = workbookId
        self.workbookName = workbookName
        _viewModel = StateObject(wrappedValue: SheetsViewModel(workbookId: workbookId, workbookName: workbookName))
    }

    var body: some View {
        ZStack {
            switch viewModel.uiState {
            case .loading:
                ProgressView("Loading sheets...")

            case .creating:
                ProgressView("Creating sheet...")

            case .success(_, let sheets):
                if sheets.isEmpty {
                    VStack(spacing: 20) {
                        Text("No Sheets")
                            .font(.title2)
                            .foregroundColor(.secondary)
                        Text("Create your first sheet to track expenses")
                            .foregroundColor(.secondary)
                        Button("Create Sheet") {
                            showingCreateDialog = true
                        }
                        .buttonStyle(.borderedProminent)
                    }
                } else {
                    List {
                        ForEach(Array(sheets.indices), id: \.self) { index in
                            let sheet = sheets[index]
                            NavigationLink(destination: {
                                AnyExpensesView(
                                    workbookId: workbookId,
                                    sheetId: sheet.getId(),
                                    sheetName: sheet.getName(),
                                    sheet: sheet
                                )
                            }()) {
                                HStack {
                                    Image(systemName: "doc.text")
                                        .foregroundColor(.blue)
                                    Text(sheet.getName())
                                        .font(.body)
                                }
                            }
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    viewModel.deleteSheet(sheet: sheet)
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
                }
                .padding()
            }
        }
        .navigationTitle(workbookName)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showingCreateDialog = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showingCreateDialog) {
            CreateSheetDialog(
                selectedMonth: $selectedMonth,
                selectedYear: $selectedYear,
                months: months,
                onCreate: {
                    viewModel.createSheet(month: selectedMonth, year: selectedYear)
                    showingCreateDialog = false
                },
                onCancel: {
                    showingCreateDialog = false
                }
            )
        }
    }
}

struct CreateSheetDialog: View {
    @Binding var selectedMonth: String
    @Binding var selectedYear: Int
    let months: [String]
    let onCreate: () -> Void
    let onCancel: () -> Void

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Sheet Details")) {
                    Picker("Month", selection: $selectedMonth) {
                        ForEach(months, id: \.self) { month in
                            Text(month).tag(month)
                        }
                    }
                    Stepper("Year: \(selectedYear)", value: $selectedYear, in: 2000...2100)
                }
            }
            .navigationTitle("Create Sheet")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onCancel()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        onCreate()
                    }
                }
            }
        }
    }
}

// Type-erased wrapper for working with existential IWorkSheet types
struct AnyExpensesView: View {
    let workbookId: String
    let sheetId: String
    let sheetName: String
    let sheet: any IWorkSheet
    
    @StateObject private var viewModel: AnyExpensesViewModel
    @State private var showingCreateDialog = false
    @State private var showingEditDialog = false
    @State private var editingExpense: Expense?
    @State private var expenseName = ""
    @State private var expenseAmount = ""
    @State private var expenseCurrency = "USD"
    
    let currencies = ["USD", "EUR", "GBP", "INR", "JPY", "CNY"]
    
    init(workbookId: String, sheetId: String, sheetName: String, sheet: any IWorkSheet) {
        self.workbookId = workbookId
        self.sheetId = sheetId
        self.sheetName = sheetName
        self.sheet = sheet
        _viewModel = StateObject(wrappedValue: AnyExpensesViewModel(sheet: sheet, sheetName: sheetName))
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

// Type-erased ViewModel for working with any IWorkSheet
@MainActor
private class AnyExpensesViewModel: ObservableObject {
    enum UiState {
        case loading
        case creating
        case updating
        case deleting
        case success(sheetName: String, expenses: [Expense])
        case error(message: String)
    }
    
    @Published var uiState: UiState = .loading
    
    private let sheet: any IWorkSheet
    private let sheetName: String
    
    init(sheet: any IWorkSheet, sheetName: String) {
        self.sheet = sheet
        self.sheetName = sheetName
        loadExpenses()
    }
    
    func loadExpenses() {
        uiState = .loading
        
        Task {
            do {
                // Call get() which returns the associated type T
                // We need to bridge from the protocol to our concrete type
                let rows = try await callGet(on: sheet)
                uiState = .success(sheetName: sheetName, expenses: rows)
            } catch {
                uiState = .error(message: "Failed to load expenses: \(error.localizedDescription)")
            }
        }
    }
    
    private func callGet(on sheet: any IWorkSheet) async throws -> [Expense] {
        // This helper function helps bridge the existential type
        // to the concrete Expense type
        func get<Sheet: IWorkSheet>(from sheet: Sheet) async throws -> [Expense] {
            let result = try await sheet.get()
            guard let expenses = result as? [Expense] else {
                throw NSError(domain: "AnyExpensesViewModel", code: 1, 
                             userInfo: [NSLocalizedDescriptionKey: "Sheet does not contain Expense data"])
            }
            return expenses
        }
        
        return try await get(from: sheet)
    }
    
    func createExpense(name: String, amount: Double, currency: String) {
        uiState = .creating
        
        Task {
            do {
                let expense = Expense(name: name, amount: amount, currency: currency)
                _ = try await callCreate(on: sheet, row: expense)
                loadExpenses()
            } catch {
                uiState = .error(message: "Failed to create expense: \(error.localizedDescription)")
            }
        }
    }
    
    private func callCreate(on sheet: any IWorkSheet, row: Expense) async throws -> Expense {
        func create<Sheet: IWorkSheet>(on sheet: Sheet, row: Expense) async throws -> Expense {
            guard let typedRow = row as? Sheet.T else {
                throw NSError(domain: "AnyExpensesViewModel", code: 2,
                             userInfo: [NSLocalizedDescriptionKey: "Type mismatch for create"])
            }
            let result = try await sheet.create(row: typedRow)
            guard let expense = result as? Expense else {
                throw NSError(domain: "AnyExpensesViewModel", code: 2,
                             userInfo: [NSLocalizedDescriptionKey: "Failed to create expense"])
            }
            return expense
        }
        
        return try await create(on: sheet, row: row)
    }
    
    func updateExpense(_ expense: Expense) {
        uiState = .updating
        
        Task {
            do {
                _ = try await callUpdate(on: sheet, row: expense)
                loadExpenses()
            } catch {
                uiState = .error(message: "Failed to update expense: \(error.localizedDescription)")
            }
        }
    }
    
    private func callUpdate(on sheet: any IWorkSheet, row: Expense) async throws -> Expense {
        func update<Sheet: IWorkSheet>(on sheet: Sheet, row: Expense) async throws -> Expense {
            guard let typedRow = row as? Sheet.T else {
                throw NSError(domain: "AnyExpensesViewModel", code: 3,
                             userInfo: [NSLocalizedDescriptionKey: "Type mismatch for update"])
            }
            let result = try await sheet.update(row: typedRow)
            guard let expense = result as? Expense else {
                throw NSError(domain: "AnyExpensesViewModel", code: 3,
                             userInfo: [NSLocalizedDescriptionKey: "Failed to update expense"])
            }
            return expense
        }
        
        return try await update(on: sheet, row: row)
    }
    
    func deleteExpense(_ expense: Expense) {
        uiState = .deleting
        
        Task {
            do {
                if case .success(let sheetName, let expenses) = uiState {
                    let updatedExpenses = expenses.filter { $0._rowId != expense._rowId }
                    uiState = .success(sheetName: sheetName, expenses: updatedExpenses)
                    
                    _ = try await callDelete(on: sheet, row: expense)
                }
            } catch {
                uiState = .error(message: "Failed to delete expense: \(error.localizedDescription)")
                loadExpenses()
            }
        }
    }
    
    private func callDelete(on sheet: any IWorkSheet, row: Expense) async throws -> Expense {
        func delete<Sheet: IWorkSheet>(on sheet: Sheet, row: Expense) async throws -> Expense {
            guard let typedRow = row as? Sheet.T else {
                throw NSError(domain: "AnyExpensesViewModel", code: 4,
                             userInfo: [NSLocalizedDescriptionKey: "Type mismatch for delete"])
            }
            let result = try await sheet.delete(row: typedRow)
            guard let expense = result as? Expense else {
                throw NSError(domain: "AnyExpensesViewModel", code: 4,
                             userInfo: [NSLocalizedDescriptionKey: "Failed to delete expense"])
            }
            return expense
        }
        
        return try await delete(on: sheet, row: row)
    }
}


