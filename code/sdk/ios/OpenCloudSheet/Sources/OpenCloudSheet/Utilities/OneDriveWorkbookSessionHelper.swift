//
//  OneDriveWorkbookSessionHelper.swift
//  OpenCloudSheet
//
//  Manages Microsoft Excel Online workbook sessions
//

import Foundation

actor OneDriveWorkbookSessionHelper {
    private nonisolated(unsafe) let authenticator: IAuthenticator
    private var sessionCache: [String: String] = [:]

    init(authenticator: IAuthenticator) {
        self.authenticator = authenticator
    }

    func getSessionId(fileId: String, ownerId: String) async throws -> String {
        if let cachedSession = sessionCache[fileId] {
            return cachedSession
        }

        let sessionId = try await createSession(fileId: fileId, ownerId: ownerId)
        sessionCache[fileId] = sessionId
        print("Created and cached session: \(sessionId) for workbook: \(fileId)")
        return sessionId
    }

    func invalidateSession(fileId: String) {
        sessionCache.removeValue(forKey: fileId)
        print("Invalidated session for workbook: \(fileId)")
    }

    func closeSession(fileId: String, ownerId: String) async throws {
        guard let sessionId = sessionCache.removeValue(forKey: fileId) else {
            print("No session to close for workbook: \(fileId)")
            return
        }

        let token = try await authenticator.getAuthToken()
        guard let authToken = token else {
            throw NSError(domain: "OneDriveWorkbookSessionHelper", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No auth token available"])
        }

        let url = URL(string: "\(OneDriveConstants.baseMSGraphURL)/users/\(ownerId)/drive/items/\(fileId)/workbook/closeSession")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("Bearer \(authToken)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue(sessionId, forHTTPHeaderField: "workbook-session-id")

        let (_, _) = try await URLSession.shared.data(for: request)
        print("Closed session: \(sessionId) for workbook: \(fileId)")
    }

    private func createSession(fileId: String, ownerId: String) async throws -> String {
        let token = try await authenticator.getAuthToken()
        guard let authToken = token else {
            throw NSError(domain: "OneDriveWorkbookSessionHelper", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "No auth token available"])
        }

        let url = URL(string: "\(OneDriveConstants.baseMSGraphURL)/users/\(ownerId)/drive/items/\(fileId)/workbook/createSession")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("Bearer \(authToken)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")

        let body = ["persistChanges": true]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 201 else {
            throw NSError(domain: "OneDriveWorkbookSessionHelper", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to create session"])
        }

        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        guard let sessionId = json?["id"] as? String else {
            throw NSError(domain: "OneDriveWorkbookSessionHelper", code: -3,
                         userInfo: [NSLocalizedDescriptionKey: "No session ID in response"])
        }

        return sessionId
    }
}
