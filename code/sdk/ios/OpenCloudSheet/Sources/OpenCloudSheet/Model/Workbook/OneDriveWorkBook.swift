//
//  OneDriveWorkBook.swift
//  OpenCloudSheet
//
//  Microsoft OneDrive implementation of IWorkBook supporting multiple sheet types
//

import Foundation
import OSLog

class OneDriveWorkBook: IWorkBook {
    private let metadataInfo: OneDriveWorkBookMetadataInfo
    private let authenticator: IAuthenticator
    private let workBookEntry: WorkBookEntry
    private let oneDriveClient: OneDriveClient
    private var metadataSheet: OneDriveWorkSheet<SheetMetadata>?
    private let logger = Logger(subsystem: "com.opencloudsheet", category: "OneDriveWorkBook")

    private static let METADATA_SHEET_NAME = "_Metadata"

    init(
        metadataInfo: OneDriveWorkBookMetadataInfo,
        authenticator: IAuthenticator,
        workBookEntry: WorkBookEntry,
        oneDriveClient: OneDriveClient
    ) {
        self.metadataInfo = metadataInfo
        self.authenticator = authenticator
        self.workBookEntry = workBookEntry
        self.oneDriveClient = oneDriveClient
    }

    func initialize() async throws {
        let worksheets = try await getAllWorkSheetsFromExcel()

        if let metadataWorksheet = worksheets.first(where: { $0.name == OneDriveWorkBook.METADATA_SHEET_NAME }) {
            logger.info("Found existing _Metadata sheet")
            self.metadataSheet = OneDriveWorkSheet<SheetMetadata>(
                metadataInfo: metadataInfo,
                workSheetData: metadataWorksheet,
                clazz: SheetMetadata.self,
                authenticator: authenticator,
                oneDriveClient: oneDriveClient
            )
        } else {
            logger.info("Creating new _Metadata sheet")
            let createdWorksheet = try await createWorkSheetInExcel(sheetName: OneDriveWorkBook.METADATA_SHEET_NAME)
            self.metadataSheet = OneDriveWorkSheet<SheetMetadata>(
                metadataInfo: metadataInfo,
                workSheetData: createdWorksheet,
                clazz: SheetMetadata.self,
                authenticator: authenticator,
                oneDriveClient: oneDriveClient
            )
            try await setWorkSheetVisibility(worksheetId: createdWorksheet.id, visibility: "VeryHidden")
        }
    }

    func getId() -> String {
        return metadataInfo.fileId
    }

    func getName() -> String {
        return workBookEntry.name
    }

    func getWorkBookEntry() -> WorkBookEntry {
        return workBookEntry
    }

    // MARK: - IWorkBook Protocol Methods

    func createSheet<T: IWorksheetRow>(type: T.Type, name: String, description: String) async throws -> any IWorkSheet<T> {
        guard !name.isEmpty else {
            throw NSError(domain: "OneDriveWorkBook", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Sheet name cannot be blank"])
        }

        guard let metadataSheet = metadataSheet else {
            throw NSError(domain: "OneDriveWorkBook", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Workbook not initialized. Call initialize() first."])
        }

        let existingSheets = try await metadataSheet.get()
        if existingSheets.contains(where: { $0.sheetName == name }) {
            throw NSError(domain: "OneDriveWorkBook", code: -3,
                         userInfo: [NSLocalizedDescriptionKey: "Sheet with name '\(name)' already exists"])
        }

        let workSheetData = try await createWorkSheetInExcel(sheetName: name)

        let worksheet = OneDriveWorkSheet<T>(
            metadataInfo: metadataInfo,
            workSheetData: workSheetData,
            clazz: type,
            authenticator: authenticator,
            oneDriveClient: oneDriveClient
        )

        let providerMetadataInfo = """
        {"iosClassName":"\(type.getIosClassName())","androidClassName":"\(type.getAndroidClassName())"}
        """

        let sheetMetadata = SheetMetadata(
            sheetName: name,
            worksheetId: workSheetData.id,
            description: description,
            providerMetadataInfo: providerMetadataInfo
        )
        _ = try await metadataSheet.create(row: sheetMetadata)

        logger.info("Created sheet '\(name)' with type \(String(describing: type))")
        return worksheet
    }

    func getSheets() async throws -> [SheetMetadata] {
        guard let metadataSheet = metadataSheet else {
            throw NSError(domain: "OneDriveWorkBook", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Workbook not initialized. Call initialize() first."])
        }

        return try await metadataSheet.get()
    }

    func getSheet<T: IWorksheetRow>(name: String, type: T.Type) async throws -> (any IWorkSheet<T>)? {
        guard let metadataSheet = metadataSheet else {
            throw NSError(domain: "OneDriveWorkBook", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Workbook not initialized. Call initialize() first."])
        }

        let sheets = try await metadataSheet.get()
        guard let sheetMetadata = sheets.first(where: { $0.sheetName == name }) else {
            return nil
        }

        let workSheetData = WorkSheetData(
            id: sheetMetadata.worksheetId,
            name: sheetMetadata.sheetName,
            position: 0
        )

        return OneDriveWorkSheet<T>(
            metadataInfo: metadataInfo,
            workSheetData: workSheetData,
            clazz: type,
            authenticator: authenticator,
            oneDriveClient: oneDriveClient
        )
    }

    func deleteSheet(name: String) async throws {
        guard let metadataSheet = metadataSheet else {
            throw NSError(domain: "OneDriveWorkBook", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Workbook not initialized. Call initialize() first."])
        }

        let sheets = try await metadataSheet.get()
        guard let sheetMetadata = sheets.first(where: { $0.sheetName == name }) else {
            throw NSError(domain: "OneDriveWorkBook", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Sheet with name '\(name)' not found"])
        }

        try await deleteWorkSheetInExcel(worksheetId: sheetMetadata.worksheetId)
        _ = try await metadataSheet.delete(row: sheetMetadata)

        logger.info("Deleted sheet '\(name)'")
    }

    // MARK: - Internal Excel Operations

    private func getAllWorkSheetsFromExcel() async throws -> [WorkSheetData] {
        guard let token = try await authenticator.getAuthToken() else {
            throw NSError(domain: "OneDriveWorkBook", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let url = URL(string: getWorksheetOperationUrl())!
        var request = URLRequest(url: url)
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await oneDriveClient.session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            logger.error("Failed to get worksheets: HTTP \(statusCode) - \(errorBody)")
            throw NSError(domain: "OneDriveWorkBook", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to get worksheets"])
        }

        let workSheetListResponse = try JSONDecoder().decode(WorkSheetListResponse.self, from: data)
        return workSheetListResponse.value
    }

    private func createWorkSheetInExcel(sheetName: String) async throws -> WorkSheetData {
        guard let token = try await authenticator.getAuthToken() else {
            throw NSError(domain: "OneDriveWorkBook", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let createUrl = URL(string: getWorksheetOperationUrl())!
        var createRequest = URLRequest(url: createUrl)
        createRequest.httpMethod = "POST"
        createRequest.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        createRequest.addValue("application/json", forHTTPHeaderField: "Content-Type")

        let createRequestBody = CreateWorkSheetRequest(name: sheetName)
        createRequest.httpBody = try JSONEncoder().encode(createRequestBody)

        let (data, response) = try await oneDriveClient.session.data(for: createRequest)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 201 else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            logger.error("Failed to create worksheet: HTTP \(statusCode) - \(errorBody)")
            throw NSError(domain: "OneDriveWorkBook", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to create worksheet"])
        }

        let workSheetResponse = try JSONDecoder().decode(WorkSheetResponse.self, from: data)

        return WorkSheetData(
            id: workSheetResponse.id,
            name: workSheetResponse.name,
            position: workSheetResponse.position
        )
    }

    private func deleteWorkSheetInExcel(worksheetId: String) async throws {
        guard let token = try await authenticator.getAuthToken() else {
            throw NSError(domain: "OneDriveWorkBook", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let url = URL(string: "\(getWorksheetOperationUrl())/\(worksheetId)")!
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await oneDriveClient.session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 204 else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            logger.error("Failed to delete worksheet: HTTP \(statusCode) - \(errorBody)")
            throw NSError(domain: "OneDriveWorkBook", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to delete worksheet"])
        }
    }

    private func setWorkSheetVisibility(worksheetId: String, visibility: String) async throws {
        guard let token = try await authenticator.getAuthToken() else {
            throw NSError(domain: "OneDriveWorkBook", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let url = URL(string: "\(getWorksheetOperationUrl())/\(worksheetId)")!
        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")

        let requestBody = ["visibility": visibility]
        request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)

        let (data, response) = try await oneDriveClient.session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            let errorBody = String(data: data, encoding: .utf8) ?? ""
            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            logger.error("Failed to set worksheet visibility: HTTP \(statusCode) - \(errorBody)")
            throw NSError(domain: "OneDriveWorkBook", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to set worksheet visibility"])
        }

        logger.info("Set worksheet '\(worksheetId)' visibility to '\(visibility)'")
    }

    private func getBaseWorkbookUrl() -> String {
        return "\(OneDriveConstants.baseMSGraphURL)/users('\(metadataInfo.ownerId)')/drive/items('\(metadataInfo.fileId)')/workbook"
    }

    private func getWorksheetOperationUrl() -> String {
        return "\(getBaseWorkbookUrl())/worksheets"
    }
}
