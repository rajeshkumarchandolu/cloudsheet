//
//  WorkBooksViewModel.swift
//  CloudSheetDemo
//
//  ViewModel for managing workbooks list and creation
//

import Foundation
import Combine
import OpenCloudSheet

enum WorkBooksUiState {
    case loading
    case creating
    case success(workbooks: [any IWorkBook])
    case error(message: String)
}

@MainActor
class WorkBooksViewModel: ObservableObject {
    @Published var uiState: WorkBooksUiState = .loading

    init() {
        loadWorkBooks()
    }

    func loadWorkBooks() {
        uiState = .loading

        Task {
            do {
                let workbooks = try await OpenCloudSheetSdk.getWorkBooks(.OneDrive)
                uiState = .success(workbooks: workbooks)
            } catch {
                uiState = .error(message: "Failed to load workbooks: \(error.localizedDescription)")
            }
        }
    }

    func createWorkBook(name: String, description: String) {
        uiState = .creating

        Task {
            do {
                _ = try await OpenCloudSheetSdk.createWorkBook(
                    provider: .OneDrive,
                    workbookName: name,
                    description: description,
                    type: Expense.self
                )
                loadWorkBooks()
            } catch {
                uiState = .error(message: "Failed to create workbook: \(error.localizedDescription)")
            }
        }
    }

    func updateWorkBook(workbook: any IWorkBook, newName: String, newDescription: String) {
        Task {
            do {
                var entry = workbook.getWorkBookEntry()
                entry.name = newName
                entry.description = newDescription

                try await OpenCloudSheetSdk.updateWorkBook(provider: .OneDrive, workbookEntry: entry)
                loadWorkBooks()
            } catch {
                uiState = .error(message: "Failed to update workbook: \(error.localizedDescription)")
            }
        }
    }

    func deleteWorkBook(workbook: any IWorkBook) {
        Task {
            do {
                if case .success(let workbooks) = uiState {
                    let updatedList = workbooks.filter { $0.getId() != workbook.getId() }
                    uiState = .success(workbooks: updatedList)

                    try await OpenCloudSheetSdk.deleteWorkBook(provider: .OneDrive, workbookEntry: workbook.getWorkBookEntry())
                }
            } catch {
                uiState = .error(message: "Failed to delete workbook: \(error.localizedDescription)")
                loadWorkBooks()
            }
        }
    }
}
