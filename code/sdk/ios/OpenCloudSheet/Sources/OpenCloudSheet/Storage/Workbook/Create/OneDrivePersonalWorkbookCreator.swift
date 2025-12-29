//
//  OneDrivePersonalWorkbookCreator.swift
//  OpenCloudSheet
//
//  Creates Excel workbooks in the current user's personal OneDrive
//

import Foundation

class OneDrivePersonalWorkbookCreator: ICreateWorkBook {
    private let authProvider: IAuthenticator
    private let oneDriveClient: OneDriveClient

    init(authProvider: IAuthenticator, oneDriveClient: OneDriveClient) {
        self.authProvider = authProvider
        self.oneDriveClient = oneDriveClient
    }

    func createWorkbook(folder: ICloudFile?, name: String) async throws -> ICloudFile {
        guard !name.isEmpty else {
            throw NSError(domain: "OneDrivePersonalWorkbookCreator", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Sheet name cannot be blank"])
        }

        if let folder = folder, !folder.isFolder() {
            throw NSError(domain: "OneDrivePersonalWorkbookCreator", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Provided folder must be a folder, not a file"])
        }

        guard let token = try await authProvider.getAuthToken() else {
            throw NSError(domain: "OneDrivePersonalWorkbookCreator", code: -3,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let fileName = name.hasSuffix(".xlsx") ? name : "\(name).xlsx"

        let url = buildCreateFileUrl(folder: folder)
        let requestBody = try buildExcelFileRequestBody(fileName: fileName)

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = requestBody

        let (data, response) = try await oneDriveClient.session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 201 else {
            throw NSError(domain: "OneDrivePersonalWorkbookCreator", code: -4,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to create Excel file"])
        }

        return try parseCreatedFile(data: data)
    }

    private func buildCreateFileUrl(folder: ICloudFile?) -> URL {
        let baseUrl = OneDriveConstants.baseMSGraphURL

        if let folder = folder {
            return URL(string: "\(baseUrl)/me/drive/items/\(folder.getId())/children")!
        } else {
            return URL(string: "\(baseUrl)/me/drive/root/children")!
        }
    }

    private func buildExcelFileRequestBody(fileName: String) throws -> Data {
        let jsonBody: [String: Any] = [
            "name": fileName,
            "file": [:],
            "@microsoft.graph.conflictBehavior": "fail"
        ]

        return try JSONSerialization.data(withJSONObject: jsonBody)
    }

    private func parseCreatedFile(data: Data) throws -> ICloudFile {
        let driveItem = try JSONDecoder().decode(DriveItem.self, from: data)
        return OneDriveFile(item: driveItem)
    }
}
