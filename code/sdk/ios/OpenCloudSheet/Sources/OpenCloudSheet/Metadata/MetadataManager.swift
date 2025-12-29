//
//  MetadataManager.swift
//  OpenCloudSheet
//
//  Manager for the Metadata.xlsx workbook that tracks all user workbooks
//

import Foundation

class MetadataManager {
    private let workbook: any IWorkBook
    private let createWorkBookInstance: (IWorksheetRow.Type, WorkBookEntry) -> (any IWorkBook)?

    private var workBooksSheet: OneDriveWorkSheet<WorkBookEntry>?

    private static let WORKBOOKS_SHEET_NAME = "WorkBooks"

    init(
        workbook: any IWorkBook,
        createWorkBookInstance: @escaping (IWorksheetRow.Type, WorkBookEntry) -> (any IWorkBook)?
    ) {
        self.workbook = workbook
        self.createWorkBookInstance = createWorkBookInstance
    }

    func initialize() async throws {
        let existingSheets = try await workbook.getWorkSheets()
        let existingWorkBooksSheet = existingSheets.first { $0.getName() == MetadataManager.WORKBOOKS_SHEET_NAME }

        if let sheet = existingWorkBooksSheet as? OneDriveWorkSheet<WorkBookEntry> {
            workBooksSheet = sheet
            return
        }

        print("Creating the \(MetadataManager.WORKBOOKS_SHEET_NAME) sheet in the \(workbook.getName())")
        let newSheet = try await workbook.createWorkSheet(sheetName: MetadataManager.WORKBOOKS_SHEET_NAME)
        workBooksSheet = newSheet as? OneDriveWorkSheet<WorkBookEntry>
    }

    func addWorkBook(
        name: String,
        className: String,
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
            guard let clazz = await resolveClass(for: entry) else {
                print("Failed to load class for workbook: \(entry.name)")
                continue
            }

            if let workbook = createWorkBookInstance(clazz, entry) {
                workbooks.append(workbook)
            }
        }

        return workbooks
    }

    private func resolveClass(for entry: WorkBookEntry) async -> IWorksheetRow.Type? {
        let metadataInfo = parseProviderMetadata(entry.providerMetadataInfo)

        guard let iosClassName = metadataInfo?.iosClassName else {
            print("ERROR: Unable to resolve class for workbook '\(entry.name)': iosClassName not found in providerMetadataInfo. Workbook will be skipped.")
            return nil
        }

        guard let clazz = await TypeRegistry.shared.resolve(className: iosClassName) else {
            print("ERROR: Unable to resolve class for workbook '\(entry.name)': Class '\(iosClassName)' not found in TypeRegistry. Did you forget to call OpenCloudSheetSdk.registerModels([YourModel.self])? Workbook will be skipped.")
            return nil
        }

        return clazz
    }

    private func parseProviderMetadata(_ json: String) -> OneDriveWorkBookMetadataInfo? {
        guard let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(OneDriveWorkBookMetadataInfo.self, from: data)
    }

    func updateWorkBook(workbookEntry: WorkBookEntry) async throws {
        _ = try await workBooksSheet?.update(row: workbookEntry)
    }

    func deleteWorkBook(workbookEntry: WorkBookEntry) async throws {
        _ = try await workBooksSheet?.delete(row: workbookEntry)
    }
}
