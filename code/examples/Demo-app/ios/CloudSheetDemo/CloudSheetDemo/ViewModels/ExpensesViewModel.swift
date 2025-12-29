//
//  ExpensesViewModel.swift
//  CloudSheetDemo
//
//  ViewModel for managing expenses within a worksheet
//

import Foundation
import Combine
import OpenCloudSheet

enum ExpensesUiState {
    case loading
    case creating
    case updating
    case deleting
    case success(sheetName: String, expenses: [Expense])
    case error(message: String)
}

@MainActor
class ExpensesViewModel<Sheet: IWorkSheet>: ObservableObject where Sheet.T == Expense {
    @Published var uiState: ExpensesUiState = .loading

    private let sheet: Sheet
    private let sheetName: String

    init(sheet: Sheet, sheetName: String) {
        self.sheet = sheet
        self.sheetName = sheetName
        loadExpenses()
    }

    func loadExpenses() {
        uiState = .loading

        Task {
            do {
                let expenses = try await sheet.get()
                print("Loaded \(expenses.count) expenses from sheet: \(sheetName)")
                uiState = .success(sheetName: sheetName, expenses: expenses)
            } catch {
                print("Failed to load expenses: \(error)")
                uiState = .error(message: "Failed to load expenses: \(error.localizedDescription)")
            }
        }
    }

    func createExpense(name: String, amount: Double, currency: String) {
        uiState = .creating

        Task {
            do {
                let expense = Expense(name: name, amount: amount, currency: currency)
                _ = try await sheet.create(row: expense)
                print("Created expense: \(name)")
                loadExpenses()
            } catch {
                print("Failed to create expense: \(error)")
                uiState = .error(message: "Failed to create expense: \(error.localizedDescription)")
            }
        }
    }

    func updateExpense(_ expense: Expense) {
        uiState = .updating

        Task {
            do {
                _ = try await sheet.update(row: expense)
                print("Updated expense: \(expense.name)")
                loadExpenses()
            } catch {
                print("Failed to update expense: \(error)")
                uiState = .error(message: "Failed to update expense: \(error.localizedDescription)")
            }
        }
    }

    func deleteExpense(_ expense: Expense) {
        uiState = .deleting

        Task {
            do {
                _ = try await sheet.delete(row: expense)
                print("Deleted expense: \(expense.name)")
                loadExpenses()
            } catch {
                print("Failed to delete expense: \(error)")
                uiState = .error(message: "Failed to delete expense: \(error.localizedDescription)")
            }
        }
    }
}
