//
//  OneDriveListDirectory.swift
//  OpenCloudSheet
//
//  Lists directory contents from OneDrive
//

import Foundation

class OneDriveListDirectory: IListDirectoryContents {
    private let authProvider: IAuthenticator
    private let oneDriveClient: OneDriveClient

    init(authProvider: IAuthenticator, oneDriveClient: OneDriveClient) {
        self.authProvider = authProvider
        self.oneDriveClient = oneDriveClient
    }

    func listDirectoryContents(directory: ICloudFile?) async throws -> [ICloudFile] {
        let accessToken = try await getAccessToken()
        var allItems: [ICloudFile] = []
        var currentUrl: URL? = buildUrl(directory: directory)

        while let url = currentUrl {
            let pageResponse = try await fetchChildrenPage(url: url, token: accessToken)
            let pageItems = pageResponse.value.map { OneDriveFile(item: $0) }

            allItems.append(contentsOf: pageItems)
            currentUrl = pageResponse.odataNextLink != nil ? URL(string: pageResponse.odataNextLink!) : nil
        }

        return allItems
    }

    private func getAccessToken() async throws -> String {
        guard let token = try await authProvider.getAuthToken() else {
            throw NSError(domain: "OneDriveListDirectory", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No authentication token available"])
        }
        return token
    }

    private func fetchChildrenPage(url: URL, token: String) async throws -> ListChildrenResponse {
        let request = createAuthorizedRequest(url: url, token: token)
        let responseBody = try await executeRequest(request: request)
        return try parseChildrenResponse(responseBody: responseBody)
    }

    private func createAuthorizedRequest(url: URL, token: String) -> URLRequest {
        var request = URLRequest(url: url)
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        return request
    }

    private func executeRequest(request: URLRequest) async throws -> String {
        let (data, response) = try await oneDriveClient.session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw NSError(domain: "OneDriveListDirectory", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to list items"])
        }

        return String(data: data, encoding: .utf8) ?? ""
    }

    private func parseChildrenResponse(responseBody: String) throws -> ListChildrenResponse {
        guard let data = responseBody.data(using: .utf8) else {
            throw NSError(domain: "OneDriveListDirectory", code: -3,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to parse response"])
        }

        return try JSONDecoder().decode(ListChildrenResponse.self, from: data)
    }

    private func buildUrl(directory: ICloudFile?) -> URL {
        let baseUrl = OneDriveConstants.baseMSGraphURL

        if let dir = directory {
            return URL(string: "\(baseUrl)/me/drive/items/\(dir.getId())/children")!
        } else {
            return URL(string: "\(baseUrl)/me/drive/root/children")!
        }
    }
}
