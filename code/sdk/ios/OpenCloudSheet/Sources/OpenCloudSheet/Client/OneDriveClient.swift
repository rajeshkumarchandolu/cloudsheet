//
//  OneDriveClient.swift
//  OpenCloudSheet
//
//  Configures URLSession for OneDrive API calls with automatic session management via interceptor
//

import Foundation

class OneDriveClient {
    private let sessionHelper: OneDriveWorkbookSessionHelper
    private let requestInterceptor: OneDriveRequestInterceptor

    lazy var session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 30
        return URLSession(configuration: config)
    }()

    init(sessionHelper: OneDriveWorkbookSessionHelper) {
        self.sessionHelper = sessionHelper
        self.requestInterceptor = OneDriveRequestInterceptor(sessionHelper: sessionHelper)
    }

    func executeRequest(_ request: URLRequest) async throws -> (Data, URLResponse) {
        return try await requestInterceptor.intercept(request, session: session)
    }

    func getSessionHelper() -> OneDriveWorkbookSessionHelper {
        return sessionHelper
    }
}
