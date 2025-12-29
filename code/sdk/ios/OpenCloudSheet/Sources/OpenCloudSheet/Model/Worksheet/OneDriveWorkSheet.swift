//
//  OneDriveWorkSheet.swift
//  OpenCloudSheet
//
//  Microsoft OneDrive implementation of IWorkSheet
//

import Foundation
import OSLog

class OneDriveWorkSheet<T: IWorksheetRow>: IWorkSheet {
    private let metadataInfo: OneDriveWorkBookMetadataInfo
    private let workSheetData: WorkSheetData
    private let clazz: T.Type
    private let authenticator: IAuthenticator
    private let oneDriveClient: OneDriveClient

    private var isInitialized: Bool = false
    private var tableId: String?

    private let logger = Logger(subsystem: "com.opencloudsheet", category: "OneDriveWorkSheet")

    init(
        metadataInfo: OneDriveWorkBookMetadataInfo,
        workSheetData: WorkSheetData,
        clazz: T.Type,
        authenticator: IAuthenticator,
        oneDriveClient: OneDriveClient
    ) {
        self.metadataInfo = metadataInfo
        self.workSheetData = workSheetData
        self.clazz = clazz
        self.authenticator = authenticator
        self.oneDriveClient = oneDriveClient
    }

    func getId() -> String {
        return workSheetData.id
    }

    func getName() -> String {
        return workSheetData.name
    }

    func get() async throws -> [T] {
        try await ensureInitialized()

        guard let token = try await getAuthToken() else {
            throw NSError(domain: "OneDriveWorkSheet", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let url = URL(string: getTableRowsUrl())!
        var request = URLRequest(url: url)
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await oneDriveClient.executeRequest(request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            logger.error("Failed to get table rows: HTTP \(statusCode) - \(errorBody)")
            throw NSError(domain: "OneDriveWorkSheet", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to get table rows: HTTP \(statusCode)"])
        }

        let responseBody = String(data: data, encoding: .utf8) ?? ""
        guard let json = OneDriveResponseHelper.parse(responseBody),
              let valueArray = json["value"] as? [[String: Any]] else {
            return []
        }

        return OneDriveResponseHelper.parseTableRows(valueArray, clazz)
    }

    func create(row: T) async throws -> T {
        try await ensureInitialized()

        var mutableRow = row
        mutableRow._rowId = OneDriveWorksheetRowHelpers.generateUUID()
        let now = OneDriveWorksheetRowHelpers.getCurrentTimestamp()
        mutableRow._createdAt = now
        mutableRow._updatedAt = now

        let rowValues = OneDriveResponseHelper.toValueArray(mutableRow)

        let requestBody: [String: Any] = [
            "values": [rowValues]
        ]

        guard let token = try await getAuthToken() else {
            throw NSError(domain: "OneDriveWorkSheet", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let addRowUrl = "\(getTableRowsUrl())/add"
        let url = URL(string: addRowUrl)!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)

        let (data, response) = try await oneDriveClient.executeRequest(request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 201 else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            logger.error("Failed to create row: HTTP \(statusCode) - \(errorBody)")
            throw NSError(domain: "OneDriveWorkSheet", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to create row: HTTP \(statusCode)"])
        }

        logger.info("Row created successfully with ID: \(mutableRow._rowId)")
        return mutableRow
    }

    func update(row: T) async throws -> T {
        try await ensureInitialized()

        guard !row._rowId.isEmpty else {
            throw NSError(domain: "OneDriveWorkSheet", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Row ID not set. Ensure the row was fetched via get() before updating."])
        }

        let rowIndex = try await findRowIndexByUniqueId(row._rowId)
        var mutableRow = row
        mutableRow._updatedAt = OneDriveWorksheetRowHelpers.getCurrentTimestamp()

        let rowValues = OneDriveResponseHelper.toValueArray(mutableRow)

        let requestBody: [String: Any] = [
            "values": [rowValues]
        ]

        guard let token = try await getAuthToken() else {
            throw NSError(domain: "OneDriveWorkSheet", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let updateRowUrl = "\(getTableRowsUrl())/itemAt(index=\(rowIndex))"
        let url = URL(string: updateRowUrl)!
        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)

        let (data, response) = try await oneDriveClient.executeRequest(request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            logger.error("Failed to update row: HTTP \(statusCode) - \(errorBody)")
            throw NSError(domain: "OneDriveWorkSheet", code: -3,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to update row: HTTP \(statusCode)"])
        }

        logger.info("Row updated successfully. ID: \(mutableRow._rowId), Index: \(rowIndex)")
        return mutableRow
    }

    func delete(row: T) async throws -> T {
        try await ensureInitialized()

        guard !row._rowId.isEmpty else {
            throw NSError(domain: "OneDriveWorkSheet", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Row ID not set. Ensure the row was fetched via get() before deleting."])
        }

        let rowIndex = try await findRowIndexByUniqueId(row._rowId)

        guard let token = try await getAuthToken() else {
            throw NSError(domain: "OneDriveWorkSheet", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let deleteRowUrl = "\(getTableRowsUrl())/itemAt(index=\(rowIndex))"
        let url = URL(string: deleteRowUrl)!
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await oneDriveClient.executeRequest(request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 204 else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            logger.error("Failed to delete row: HTTP \(statusCode) - \(errorBody)")
            throw NSError(domain: "OneDriveWorkSheet", code: -3,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to delete row: HTTP \(statusCode)"])
        }

        logger.info("Row deleted successfully. ID: \(row._rowId), Index: \(rowIndex)")
        return row
    }

    private func initialize() async throws {
        if isInitialized {
            return
        }

        try OneDriveWorksheetRowHelpers.validateColumnIndexAnnotations(clazz)

        let tableExists = try await checkTableExists()

        if !tableExists {
            try await createTable()
        } else {
            let excelColumns = try await fetchTableColumns()
            let missingColumns = OneDriveWorksheetRowHelpers.getMissingColumns(clazz, worksheetColumns: excelColumns)

            if !missingColumns.isEmpty {
                logger.info("Detected \(missingColumns.count) new columns in schema, adding them")
                for (index, columnName) in missingColumns {
                    try await createColumn(columnName: columnName, index: index)
                }
            }

            try await fixDefaultColumnNamesIfNeeded()
        }

        let excelColumns = try await fetchTableColumns()
        let expectedColumns = OneDriveWorksheetRowHelpers.buildColumnMapping(clazz)
        try OneDriveWorksheetRowHelpers.validateColumnMapping(expectedColumns: expectedColumns, worksheetColumns: excelColumns)

        logger.info("Worksheet \(self.workSheetData.name) initialized successfully")
        isInitialized = true
    }

    private func checkTableExists() async throws -> Bool {
        guard let token = try await getAuthToken() else {
            throw NSError(domain: "OneDriveWorkSheet", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let url = URL(string: getTablesListUrl())!
        var request = URLRequest(url: url)
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await oneDriveClient.executeRequest(request)

        guard let httpResponse = response as? HTTPURLResponse else {
            return false
        }

        if httpResponse.statusCode != 200 {
            return false
        }

        let responseBody = String(data: data, encoding: .utf8) ?? ""
        guard let json = OneDriveResponseHelper.parse(responseBody),
              let tablesArray = json["value"] as? [[String: Any]] else {
            return false
        }

        if !tablesArray.isEmpty {
            guard let firstTable = tablesArray.first,
                  let id = firstTable["id"] as? String else {
                throw NSError(
                    domain: "OneDriveWorkSheet",
                    code: -2,
                    userInfo: [NSLocalizedDescriptionKey: "Table exists in worksheet but unable to extract table ID from API response. Cannot proceed without table ID."]
                )
            }
            tableId = id
            logger.info("Found existing table with ID: \(id)")
            return true
        }

        return false
    }

    private func createTable() async throws {
        logger.info("Creating table")

        let columnMapping = OneDriveWorksheetRowHelpers.buildColumnMapping(clazz)
        let maxColumnIndex = columnMapping.keys.max() ?? 0
        let endColumn = getExcelColumnName(columnIndex: maxColumnIndex)

        let tableAddress = "'\(workSheetData.name)'!A1:\(endColumn)1"

        logger.info("Creating table with address: \(tableAddress) (\(maxColumnIndex + 1) columns)")

        let createTableUrl = "\(OneDriveConstants.baseMSGraphURL)/users('\(metadataInfo.ownerId)')/drive/items('\(metadataInfo.fileId)')/workbook/tables/add"

        let requestBody: [String: Any] = [
            "address": tableAddress,
            "hasHeaders": true
        ]

        guard let token = try await getAuthToken() else {
            throw NSError(domain: "OneDriveWorkSheet", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let url = URL(string: createTableUrl)!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)

        let (data, response) = try await oneDriveClient.executeRequest(request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 201 else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            logger.error("Failed to create table: \(statusCode) - \(errorBody)")
            throw NSError(domain: "OneDriveWorkSheet", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to create table"])
        }

        let responseBody = String(data: data, encoding: .utf8) ?? ""
        guard let json = OneDriveResponseHelper.parse(responseBody),
              let id = json["id"] as? String else {
            throw NSError(domain: "OneDriveWorkSheet", code: -3,
                         userInfo: [NSLocalizedDescriptionKey: "No table ID in response"])
        }

        tableId = id
        logger.info("Table created successfully with ID: \(id)")

        for (index, columnName) in columnMapping {
            try await renameColumn(columnIndex: index, newName: columnName)
        }
    }

    private func getExcelColumnName(columnIndex: Int) -> String {
        var index = columnIndex
        var columnName = ""

        while index >= 0 {
            let char = Character(UnicodeScalar(65 + (index % 26))!)
            columnName = String(char) + columnName
            index = (index / 26) - 1
        }

        return columnName
    }

    private func renameColumn(columnIndex: Int, newName: String) async throws {
        logger.info("Renaming column at index \(columnIndex) to '\(newName)'")

        guard let token = try await getAuthToken() else {
            throw NSError(domain: "OneDriveWorkSheet", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let renameUrl = "\(getTableUrl())/columns/itemAt(index=\(columnIndex))"

        let requestBody: [String: Any] = [
            "name": newName
        ]

        let url = URL(string: renameUrl)!
        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)

        let (data, response) = try await oneDriveClient.executeRequest(request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            logger.error("Failed to rename column: \(statusCode) - \(errorBody)")
            throw NSError(domain: "OneDriveWorkSheet", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to rename column at index \(columnIndex)"])
        }

        logger.info("Column at index \(columnIndex) renamed to '\(newName)' successfully")
    }

    private func fetchTableColumns() async throws -> [Int: String] {
        guard let token = try await getAuthToken() else {
            throw NSError(domain: "OneDriveWorkSheet", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let columnsUrl = "\(getTableUrl())/columns"

        let url = URL(string: columnsUrl)!
        var request = URLRequest(url: url)
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await oneDriveClient.executeRequest(request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            logger.error("Failed to fetch columns: \(statusCode) - \(errorBody)")
            throw NSError(domain: "OneDriveWorkSheet", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to fetch columns"])
        }

        let responseBody = String(data: data, encoding: .utf8) ?? ""
        guard let json = OneDriveResponseHelper.parse(responseBody),
              let columnsArray = json["value"] as? [[String: Any]] else {
            return [:]
        }

        var columns: [Int: String] = [:]
        for columnObj in columnsArray {
            if let index = columnObj["index"] as? Int,
               let name = columnObj["name"] as? String {
                columns[index] = name
            }
        }

        return columns
    }

    private func createColumn(columnName: String, index: Int) async throws {
        logger.info("Creating column '\(columnName)' at index \(index)")

        guard let token = try await getAuthToken() else {
            throw NSError(domain: "OneDriveWorkSheet", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let columnsUrl = "\(getTableUrl())/columns"

        let requestBody: [String: Any] = [
            "name": columnName,
            "index": index
        ]

        let url = URL(string: columnsUrl)!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)

        let (data, response) = try await oneDriveClient.executeRequest(request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 201 else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            logger.error("Failed to create column: \(statusCode) - \(errorBody)")
            throw NSError(domain: "OneDriveWorkSheet", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to create column '\(columnName)'"])
        }

        logger.info("Column '\(columnName)' created successfully")
    }

    private func fixDefaultColumnNamesIfNeeded() async throws {
        let excelColumns = try await fetchTableColumns()
        let expectedColumns = OneDriveWorksheetRowHelpers.buildColumnMapping(clazz)

        var columnsToRename: [Int: String] = [:]

        for (index, expectedName) in expectedColumns {
            if let currentName = excelColumns[index] {
                let defaultName = "Column\(index + 1)"

                if currentName == defaultName && currentName != expectedName {
                    columnsToRename[index] = expectedName
                }
            }
        }

        if !columnsToRename.isEmpty {
            logger.info("Detected \(columnsToRename.count) columns with default names, renaming them")
            for (index, newName) in columnsToRename {
                do {
                    try await renameColumn(columnIndex: index, newName: newName)
                } catch {
                    logger.error("Failed to rename column at index \(index) to '\(newName)': \(error)")
                }
            }
        }
    }

    private func getTablesListUrl() -> String {
        return "\(OneDriveConstants.baseMSGraphURL)/users('\(metadataInfo.ownerId)')/drive/items('\(metadataInfo.fileId)')/workbook/worksheets('\(workSheetData.id)')/tables"
    }

    private func getTableUrl() -> String {
        guard let id = tableId else {
            fatalError("Table ID not set. Table must be initialized first.")
        }
        return "\(getTablesListUrl())/\(id)"
    }

    private func ensureInitialized() async throws {
        if !isInitialized {
            try await initialize()
        }
    }

    private func findRowIndexByUniqueId(_ uniqueRowId: String) async throws -> Int {
        let allRows = try await get()

        for (index, row) in allRows.enumerated() {
            if row._rowId == uniqueRowId {
                return index
            }
        }

        throw NSError(domain: "OneDriveWorkSheet", code: -1,
                     userInfo: [NSLocalizedDescriptionKey: "Row with ID '\(uniqueRowId)' not found in worksheet"])
    }

    private func getAuthToken() async throws -> String? {
        return try await authenticator.getAuthToken()
    }

    private func getTableRowsUrl() -> String {
        return "\(getTableUrl())/rows"
    }
}
