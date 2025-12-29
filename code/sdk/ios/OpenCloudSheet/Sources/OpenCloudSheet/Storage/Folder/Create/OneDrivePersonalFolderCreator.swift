//
//  OneDrivePersonalFolderCreator.swift
//  OpenCloudSheet
//
//  Creates folders in the current user's personal OneDrive
//

import Foundation

class OneDrivePersonalFolderCreator: ICreateFolder {
    private let authProvider: IAuthenticator
    private let listDirectoryContents: IListDirectoryContents
    private let oneDriveClient: OneDriveClient

    init(
        authProvider: IAuthenticator,
        listDirectoryContents: IListDirectoryContents,
        oneDriveClient: OneDriveClient
    ) {
        self.authProvider = authProvider
        self.listDirectoryContents = listDirectoryContents
        self.oneDriveClient = oneDriveClient
    }

    func createFolder(parentDirectory: ICloudFile?, folderName: String) async throws -> ICloudFile {
        guard !folderName.isEmpty else {
            throw NSError(domain: "OneDrivePersonalFolderCreator", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Folder name cannot be blank"])
        }

        if let parent = parentDirectory, !parent.isFolder() {
            throw NSError(domain: "OneDrivePersonalFolderCreator", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Parent must be a directory"])
        }

        if let existingFolder = try await checkFolderExists(parentDirectory: parentDirectory, folderName: folderName) {
            print("Folder already exists: \(folderName)")
            return existingFolder
        }

        return try await createFolderAtLocation(parentDirectory: parentDirectory, folderName: folderName)
    }

    private func checkFolderExists(parentDirectory: ICloudFile?, folderName: String) async throws -> ICloudFile? {
        do {
            let items = try await listDirectoryContents.listDirectoryContents(directory: parentDirectory)
            return items.first { $0.isFolder() && $0.getName() == folderName }
        } catch {
            print("Failed to check if folder exists: \(folderName) - \(error)")
            return nil
        }
    }

    private func createFolderAtLocation(
        parentDirectory: ICloudFile?,
        folderName: String
    ) async throws -> ICloudFile {
        guard let token = try await authProvider.getAuthToken() else {
            throw NSError(domain: "OneDrivePersonalFolderCreator", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }

        let url = buildCreateFolderUrl(parentDirectory: parentDirectory)
        let requestBody = try buildFolderRequestBody(folderName: folderName)

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = requestBody

        let (data, response) = try await oneDriveClient.session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 201 else {
            throw NSError(domain: "OneDrivePersonalFolderCreator", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to create folder '\(folderName)'"])
        }

        return try parseFolder(data: data)
    }

    private func buildCreateFolderUrl(parentDirectory: ICloudFile?) -> URL {
        let baseUrl = OneDriveConstants.baseMSGraphURL

        if let parent = parentDirectory {
            return URL(string: "\(baseUrl)/me/drive/items/\(parent.getId())/children")!
        } else {
            return URL(string: "\(baseUrl)/me/drive/root/children")!
        }
    }

    private func buildFolderRequestBody(folderName: String) throws -> Data {
        let jsonBody: [String: Any] = [
            "name": folderName,
            "folder": [:],
            "@microsoft.graph.conflictBehavior": "fail"
        ]

        return try JSONSerialization.data(withJSONObject: jsonBody)
    }

    private func parseFolder(data: Data) throws -> ICloudFile {
        let driveItem = try JSONDecoder().decode(DriveItem.self, from: data)
        return OneDriveFile(item: driveItem)
    }
}
