//
//  ExpensesViewModel.swift
//  CloudSheetDemo
//
//  ViewModel for managing expenses within a sheet
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
class ExpensesViewModel: ObservableObject {
    @Published var uiState: ExpensesUiState = .loading

    private let workbookId: String
    private let sheetName: String
    private var workbook: (any IWorkBook)?
    private var sheet: (any IWorkSheet<Expense>)?

    init(workbookId: String, sheetName: String) {
        self.workbookId = workbookId
        self.sheetName = sheetName
        loadWorkbookAndSheet()
    }

    private func loadWorkbookAndSheet() {
        uiState = .loading

        Task {
            do {
                let workbooks = try await OpenCloudSheetSdk.getWorkBooks(.OneDrive)
                guard let foundWorkbook = workbooks.first(where: { $0.getId() == workbookId }) else {
                    uiState = .error(message: "Workbook not found")
                    return
                }

                workbook = foundWorkbook

                guard let foundSheet = try await foundWorkbook.getSheet(name: sheetName, type: Expense.self) else {
                    uiState = .error(message: "Sheet not found")
                    return
                }

                sheet = foundSheet
                loadExpenses()
            } catch {
                uiState = .error(message: "Failed to load workbook: \(error.localizedDescription)")
            }
        }
    }

    func loadExpenses() {
        Task {
            do {
                guard let sheet = sheet else {
                    uiState = .error(message: "Sheet not initialized")
                    return
                }

                let expenses = try await sheet.get()
                uiState = .success(sheetName: sheetName, expenses: expenses)
            } catch {
                uiState = .error(message: "Failed to load expenses: \(error.localizedDescription)")
            }
        }
    }

    func createExpense(name: String, amount: Double, currency: String) {
        uiState = .creating

        Task {
            do {
                guard let sheet = sheet else {
                    uiState = .error(message: "Sheet not initialized")
                    return
                }

                let expense = Expense(name: name, amount: amount, currency: currency)
                _ = try await sheet.create(row: expense)
                loadExpenses()
            } catch {
                uiState = .error(message: "Failed to create expense: \(error.localizedDescription)")
            }
        }
    }

    func updateExpense(_ expense: Expense) {
        uiState = .updating

        Task {
            do {
                guard let sheet = sheet else {
                    uiState = .error(message: "Sheet not initialized")
                    return
                }

                _ = try await sheet.update(row: expense)
                loadExpenses()
            } catch {
                uiState = .error(message: "Failed to update expense: \(error.localizedDescription)")
            }
        }
    }

    func deleteExpense(_ expense: Expense) {
        guard case .success(let sheetName, let expenses) = uiState else { return }

        let updatedExpenses = expenses.filter { $0._rowId != expense._rowId }
        uiState = .success(sheetName: sheetName, expenses: updatedExpenses)

        Task {
            do {
                guard let sheet = sheet else {
                    uiState = .error(message: "Sheet not initialized")
                    loadExpenses()
                    return
                }

                _ = try await sheet.delete(row: expense)
            } catch {
                uiState = .error(message: "Failed to delete expense: \(error.localizedDescription)")
                loadExpenses()
            }
        }
    }
}
