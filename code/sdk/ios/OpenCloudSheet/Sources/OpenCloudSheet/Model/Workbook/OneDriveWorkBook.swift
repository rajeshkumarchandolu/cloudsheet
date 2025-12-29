//
//  OneDriveWorkBook.swift
//  OpenCloudSheet
//
//  Microsoft OneDrive implementation of IWorkBook
//

import Foundation

class OneDriveWorkBook<T: IWorksheetRow>: IWorkBook {
    private let metadataInfo: OneDriveWorkBookMetadataInfo
    private let clazz: T.Type
    private let authenticator: IAuthenticator
    private let workBookEntry: WorkBookEntry
    private let oneDriveClient: OneDriveClient

    init(
        metadataInfo: OneDriveWorkBookMetadataInfo,
        clazz: T.Type,
        authenticator: IAuthenticator,
        workBookEntry: WorkBookEntry,
        oneDriveClient: OneDriveClient
    ) {
        self.metadataInfo = metadataInfo
        self.clazz = clazz
        self.authenticator = authenticator
        self.workBookEntry = workBookEntry
        self.oneDriveClient = oneDriveClient
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

    func getWorkSheets() async throws -> [any IWorkSheet] {
        guard let token = try await authenticator.getAuthToken() else {
            throw NSError(domain: "OneDriveWorkBook", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let url = URL(string: getWorksheetOperationUrl())!
        var request = URLRequest(url: url)
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await oneDriveClient.session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw NSError(domain: "OneDriveWorkBook", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to get worksheets"])
        }

        let workSheetListResponse = try JSONDecoder().decode(WorkSheetListResponse.self, from: data)

        return workSheetListResponse.value
            .filter { $0.name != "Sheet1" }
            .map { workSheetData in
                OneDriveWorkSheet<T>(
                    metadataInfo: metadataInfo,
                    workSheetData: workSheetData,
                    clazz: clazz,
                    authenticator: authenticator,
                    oneDriveClient: oneDriveClient
                )
            }
    }

    func createWorkSheet(sheetName: String) async throws -> any IWorkSheet {
        guard !sheetName.isEmpty else {
            throw NSError(domain: "OneDriveWorkBook", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Sheet name cannot be blank"])
        }

        guard let token = try await authenticator.getAuthToken() else {
            throw NSError(domain: "OneDriveWorkBook", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let existingSheets = try await getWorkSheets()
        if existingSheets.count == 1 && existingSheets[0].getName() == "Sheet1" {
            print("Found default Sheet1, renaming it to: \(sheetName)")
            let defaultSheet = existingSheets[0]
            try await renameWorksheet(sheet: defaultSheet, newName: sheetName)

            let workSheetData = WorkSheetData(
                id: defaultSheet.getId(),
                name: sheetName,
                position: 0
            )
            return OneDriveWorkSheet<T>(
                metadataInfo: metadataInfo,
                workSheetData: workSheetData,
                clazz: clazz,
                authenticator: authenticator,
                oneDriveClient: oneDriveClient
            )
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
            throw NSError(domain: "OneDriveWorkBook", code: -3,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to create worksheet"])
        }

        let workSheetResponse = try JSONDecoder().decode(WorkSheetResponse.self, from: data)

        let workSheetData = WorkSheetData(
            id: workSheetResponse.id,
            name: workSheetResponse.name,
            position: workSheetResponse.position
        )

        return OneDriveWorkSheet<T>(
            metadataInfo: metadataInfo,
            workSheetData: workSheetData,
            clazz: clazz,
            authenticator: authenticator,
            oneDriveClient: oneDriveClient
        )
    }

    func deleteWorkSheet(sheet: any IWorkSheet) async throws {
        guard let token = try await authenticator.getAuthToken() else {
            throw NSError(domain: "OneDriveWorkBook", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let url = URL(string: "\(getWorksheetOperationUrl())/\(sheet.getId())")!
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (_, response) = try await oneDriveClient.session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 204 else {
            throw NSError(domain: "OneDriveWorkBook", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to delete worksheet"])
        }

        print("Worksheet \(sheet.getName()) deleted successfully")
    }

    func renameWorksheet(sheet: any IWorkSheet, newName: String) async throws {
        guard !newName.isEmpty else {
            throw NSError(domain: "OneDriveWorkBook", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "New sheet name cannot be blank"])
        }

        guard let token = try await authenticator.getAuthToken() else {
            throw NSError(domain: "OneDriveWorkBook", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let url = URL(string: "\(getWorksheetOperationUrl())/\(sheet.getId())")!
        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")

        let requestBody = ["name": newName]
        request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)

        let (_, response) = try await oneDriveClient.session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw NSError(domain: "OneDriveWorkBook", code: -3,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to rename worksheet"])
        }

        print("Worksheet renamed from \(sheet.getName()) to \(newName) successfully")
    }

    private func getBaseWorkbookUrl() -> String {
        return "\(OneDriveConstants.baseMSGraphURL)/users('\(metadataInfo.ownerId)')/drive/items('\(metadataInfo.fileId)')/workbook"
    }

    private func getWorksheetOperationUrl() -> String {
        return "\(getBaseWorkbookUrl())/worksheets"
    }
}
