//
//  OneDrivePersonalWorkbookDeleter.swift
//  OpenCloudSheet
//
//  Deletes workbooks from the current user's personal OneDrive
//

import Foundation

class OneDrivePersonalWorkbookDeleter: IDeleteWorkBook {
    private let authenticator: IAuthenticator
    private let oneDriveClient: OneDriveClient

    init(authenticator: IAuthenticator, oneDriveClient: OneDriveClient) {
        self.authenticator = authenticator
        self.oneDriveClient = oneDriveClient
    }

    func deleteWorkBook(ownerId: String, fileId: String) async throws {
        guard let authToken = try await authenticator.getAuthToken() else {
            throw NSError(domain: "OneDrivePersonalWorkbookDeleter", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let deleteUrl = "\(OneDriveConstants.baseMSGraphURL)/users('\(ownerId)')/drive/items('\(fileId)')"

        print("Deleting workbook file: \(fileId)")

        let url = URL(string: deleteUrl)!
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        request.addValue("Bearer \(authToken)", forHTTPHeaderField: "Authorization")

        let (_, response) = try await oneDriveClient.session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 204 else {
            throw NSError(domain: "OneDrivePersonalWorkbookDeleter", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to delete workbook file"])
        }

        print("Workbook file deleted successfully: \(fileId)")
    }
}
