//
//  SheetsViewModel.swift
//  CloudSheetDemo
//
//  ViewModel for managing worksheets within a workbook
//

import Foundation
import Combine
import OpenCloudSheet

enum SheetsUiState {
    case loading
    case creating
    case success(workbookName: String, sheets: [any IWorkSheet])
    case error(message: String)
}

@MainActor
class SheetsViewModel: ObservableObject {
    @Published var uiState: SheetsUiState = .loading

    private let workbookId: String
    private let workbookName: String
    private var workbook: (any IWorkBook)?

    init(workbookId: String, workbookName: String) {
        self.workbookId = workbookId
        self.workbookName = workbookName
        loadWorkBook()
    }

    private func loadWorkBook() {
        uiState = .loading

        Task {
            do {
                let workbooks = try await OpenCloudSheetSdk.getWorkBooks(.OneDrive)
                guard let foundWorkbook = workbooks.first(where: { $0.getId() == workbookId }) else {
                    uiState = .error(message: "Workbook not found")
                    return
                }

                workbook = foundWorkbook
                loadSheets()
            } catch {
                uiState = .error(message: "Failed to load workbook: \(error.localizedDescription)")
            }
        }
    }

    private func loadSheets() {
        Task {
            do {
                guard let workbook = workbook else {
                    uiState = .error(message: "Workbook not initialized")
                    return
                }

                let sheets = try await workbook.getWorkSheets()
                uiState = .success(workbookName: workbookName, sheets: sheets)
            } catch {
                uiState = .error(message: "Failed to load sheets: \(error.localizedDescription)")
            }
        }
    }

    func createSheet(month: String, year: Int) {
        uiState = .creating

        Task {
            do {
                guard let workbook = workbook else {
                    uiState = .error(message: "Workbook not initialized")
                    return
                }

                let sheetName = "\(month) \(year)"
                _ = try await workbook.createWorkSheet(sheetName: sheetName)
                loadSheets()
            } catch {
                uiState = .error(message: "Failed to create sheet: \(error.localizedDescription)")
            }
        }
    }

    func renameSheet(sheet: any IWorkSheet, newName: String) {
        Task {
            do {
                guard let workbook = workbook else {
                    uiState = .error(message: "Workbook not initialized")
                    return
                }

                try await workbook.renameWorksheet(sheet: sheet, newName: newName)
                loadSheets()
            } catch {
                uiState = .error(message: "Failed to rename sheet: \(error.localizedDescription)")
            }
        }
    }

    func deleteSheet(sheet: any IWorkSheet) {
        Task {
            do {
                guard let workbook = workbook else {
                    uiState = .error(message: "Workbook not initialized")
                    return
                }

                if case .success(let workbookName, let sheets) = uiState {
                    let updatedList = sheets.filter { $0.getId() != sheet.getId() }
                    uiState = .success(workbookName: workbookName, sheets: updatedList)

                    try await workbook.deleteWorkSheet(sheet: sheet)
                }
            } catch {
                uiState = .error(message: "Failed to delete sheet: \(error.localizedDescription)")
                loadSheets()
            }
        }
    }
}
