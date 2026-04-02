//
//  MetadataManager.swift
//  OpenCloudSheet
//
//  Manager for the Metadata.xlsx workbook that tracks all user workbooks
//

import Foundation

class MetadataManager {
    private let workbook: any IWorkBook
    private let createWorkBookInstance: (WorkBookEntry) throws -> (any IWorkBook)?

    private var workBooksSheet: (any IWorkSheet<WorkBookEntry>)?

    private static let WORKBOOKS_SHEET_NAME = "WorkBooks"

    init(
        workbook: any IWorkBook,
        createWorkBookInstance: @escaping (WorkBookEntry) throws -> (any IWorkBook)?
    ) {
        self.workbook = workbook
        self.createWorkBookInstance = createWorkBookInstance
    }

    func initialize() async throws {
        if let existingSheet = try await workbook.getSheet(name: MetadataManager.WORKBOOKS_SHEET_NAME, type: WorkBookEntry.self) {
            workBooksSheet = existingSheet
            return
        }

        print("Creating the \(MetadataManager.WORKBOOKS_SHEET_NAME) sheet in the \(workbook.getName())")
        workBooksSheet = try await workbook.createSheet(
            type: WorkBookEntry.self,
            name: MetadataManager.WORKBOOKS_SHEET_NAME,
            description: "Tracks all user workbooks"
        )
    }

    func addWorkBook(
        name: String,
        provider: Provider,
        description: String,
        providerMetadataInfo: String
    ) async throws {
        let entry = WorkBookEntry(
            name: name,
            provider: provider.rawValue,
            description: description,
            providerMetadataInfo: providerMetadataInfo
        )

        _ = try await workBooksSheet?.create(row: entry)
    }

    func listWorkBooks() async throws -> [any IWorkBook] {
        guard let sheet = workBooksSheet else {
            return []
        }

        let entries = try await sheet.get()

        var workbooks: [any IWorkBook] = []
        for entry in entries {
            do {
                if let workbook = try createWorkBookInstance(entry) {
                    try await workbook.initialize()
                    workbooks.append(workbook)
                }
            } catch {
                print("Failed to create workbook instance for '\(entry.name)': \(error)")
            }
        }

        return workbooks
    }

    func updateWorkBook(workbookEntry: WorkBookEntry) async throws {
        _ = try await workBooksSheet?.update(row: workbookEntry)
    }

    func deleteWorkBook(workbookEntry: WorkBookEntry) async throws {
        _ = try await workBooksSheet?.delete(row: workbookEntry)
    }
}
