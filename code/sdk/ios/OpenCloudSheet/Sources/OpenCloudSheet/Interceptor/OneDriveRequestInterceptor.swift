//
//  OneDriveRequestInterceptor.swift
//  OpenCloudSheet
//
//  Interceptor for automatic Microsoft Excel Online session management.
//  Proactively adds session headers to all workbook requests for optimal performance.
//  Detects session expiry errors, invalidates cached session, creates new session, and retries request.
//

import Foundation
import OSLog

class OneDriveRequestInterceptor {
    private let sessionHelper: OneDriveWorkbookSessionHelper
    private let logger = Logger(subsystem: "com.opencloudsheet", category: "OneDriveRequestInterceptor")

    private static let SESSION_HEADER = "workbook-session-id"

    init(sessionHelper: OneDriveWorkbookSessionHelper) {
        self.sessionHelper = sessionHelper
    }

    func intercept(_ request: URLRequest, session: URLSession) async throws -> (Data, URLResponse) {
        var modifiedRequest = request

        if isWorkbookRequest(request),
           request.value(forHTTPHeaderField: OneDriveRequestInterceptor.SESSION_HEADER) == nil {

            if let (fileId, ownerId) = extractIds(from: request.url?.absoluteString ?? "") {
                let sessionId = try await sessionHelper.getSessionId(fileId: fileId, ownerId: ownerId)
                modifiedRequest.setValue(sessionId, forHTTPHeaderField: OneDriveRequestInterceptor.SESSION_HEADER)
                logger.debug("Added session header to workbook request")
            }
        }

        var (data, response) = try await session.data(for: modifiedRequest)

        if let httpResponse = response as? HTTPURLResponse,
           isSessionError(response: httpResponse, data: data),
           let (fileId, ownerId) = extractIds(from: request.url?.absoluteString ?? "") {

            logger.info("Session error detected, recreating session...")

            await sessionHelper.invalidateSession(fileId: fileId)
            let newSessionId = try await sessionHelper.getSessionId(fileId: fileId, ownerId: ownerId)

            modifiedRequest.setValue(newSessionId, forHTTPHeaderField: OneDriveRequestInterceptor.SESSION_HEADER)
            (data, response) = try await session.data(for: modifiedRequest)

            logger.debug("Request retried with new session")
        }

        return (data, response)
    }

    private func isWorkbookRequest(_ request: URLRequest) -> Bool {
        return request.url?.absoluteString.contains("/workbook") ?? false
    }

    private func isSessionError(response: HTTPURLResponse, data: Data) -> Bool {
        guard response.statusCode == 404 || response.statusCode == 400 else {
            return false
        }

        if let body = String(data: data, encoding: .utf8) {
            return body.lowercased().contains("session") ||
                   body.contains("sessionNotFound") ||
                   body.contains("InvalidSessionId")
        }

        return false
    }

    private func extractIds(from url: String) -> (fileId: String, ownerId: String)? {
        let fileIdPattern = "/drive/items\\('([^']+)'\\)"
        let ownerIdPattern = "/users\\('([^']+)'\\)"

        guard let fileIdRegex = try? NSRegularExpression(pattern: fileIdPattern),
              let ownerIdRegex = try? NSRegularExpression(pattern: ownerIdPattern) else {
            return nil
        }

        let nsUrl = url as NSString
        let fileIdMatches = fileIdRegex.matches(in: url, range: NSRange(location: 0, length: nsUrl.length))
        let ownerIdMatches = ownerIdRegex.matches(in: url, range: NSRange(location: 0, length: nsUrl.length))

        guard let fileIdMatch = fileIdMatches.first,
              fileIdMatch.numberOfRanges > 1,
              let ownerIdMatch = ownerIdMatches.first,
              ownerIdMatch.numberOfRanges > 1 else {
            return nil
        }

        let fileId = nsUrl.substring(with: fileIdMatch.range(at: 1))
        let ownerId = nsUrl.substring(with: ownerIdMatch.range(at: 1))

        return (fileId, ownerId)
    }
}
