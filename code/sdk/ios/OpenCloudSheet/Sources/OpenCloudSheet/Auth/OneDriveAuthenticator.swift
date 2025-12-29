//
//  OneDriveAuthenticator.swift
//  Cloudsheet-ios-sdk
//

import Foundation
import MSAL

final class OneDriveAuthenticator: IAuthenticator,@unchecked Sendable {
    private let config: OneDriveConfiguration
    private var msalApplication: MSALPublicClientApplication?
    private var currentAccount: MSALAccount?

    init(config: OneDriveConfiguration) {
        self.config = config
    }

    @MainActor
    func login(from viewController: PlatformViewController) async throws {
        try createMSALApplication()
        try loadExistingAccount()

        if currentAccount != nil {
            return
        }

        try await signInUser(from: viewController)
    }


    func getAuthToken() async throws -> String? {
        guard let msalApp = msalApplication else {
            throw NSError(domain: "OneDriveAuthenticator", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "MSAL not initialized"])
        }

        guard let account = currentAccount else {
            throw NSError(domain: "OneDriveAuthenticator", code: -3,
                         userInfo: [NSLocalizedDescriptionKey: "No user logged in"])
        }

        let silentParameters = MSALSilentTokenParameters(scopes: config.scopes, account: account)

        return try await withCheckedThrowingContinuation { continuation in
            msalApp.acquireTokenSilent(with: silentParameters) { result, error in
                if let error = error {
                    continuation.resume(throwing: error)
                    return
                }

                continuation.resume(returning: result?.accessToken)
            }
        }
    }

    func getUserDetails() async throws -> IUserDetails? {
        guard currentAccount != nil else {
            return nil
        }

        guard let token = try await getAuthToken() else {
            return nil
        }

        let url = URL(string: "\(OneDriveConstants.baseMSGraphURL)/me")!
        var request = URLRequest(url: url)
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, _) = try await URLSession.shared.data(for: request)
        let userResponse = try JSONDecoder().decode(UserResponse.self, from: data)

        return OneDriveUserDetails(
            userId: userResponse.id,
            displayName: userResponse.displayName ?? "",
            email: userResponse.mail ?? userResponse.userPrincipalName ?? ""
        )
    }

    func logout() async throws {
        guard let msalApp = msalApplication, let account = currentAccount else {
            return
        }

        try msalApp.remove(account)
        currentAccount = nil
    }
    
    @MainActor
    private func signInUser(from viewController: PlatformViewController) async throws {
        guard let msalApp = msalApplication else {
            throw NSError(domain: "OneDriveAuthenticator", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "MSAL not initialized"])
        }

        let webViewParameters = MSALWebviewParameters(authPresentationViewController: viewController)
        webViewParameters.webviewType = .authenticationSession
        let interactiveParameters = MSALInteractiveTokenParameters(scopes: config.scopes, webviewParameters: webViewParameters)

        return try await withCheckedThrowingContinuation { continuation in
            msalApp.acquireToken(with: interactiveParameters) { [weak self] result, error in
                if let error = error {
                    continuation.resume(throwing: error)
                    return
                }

                guard let result = result else {
                    continuation.resume(throwing: NSError(domain: "OneDriveAuthenticator", code: -2,
                                                         userInfo: [NSLocalizedDescriptionKey: "No result from MSAL"]))
                    return
                }

                self?.currentAccount = result.account
                continuation.resume()
            }
        }
    }

    private func createMSALApplication() throws {
        if msalApplication != nil {
            return
        }

        let authority = try MSALAADAuthority(url: URL(string: "https://login.microsoftonline.com/common")!)
        let msalConfig = MSALPublicClientApplicationConfig(
            clientId: config.clientId,
            redirectUri: config.redirectUri,
            authority: authority
        )

        msalApplication = try MSALPublicClientApplication(configuration: msalConfig)
    }

    private func loadExistingAccount() throws {
        guard let msalApp = msalApplication else {
            return
        }

        if let accounts = try? msalApp.allAccounts(), let account = accounts.first {
            currentAccount = account
        }
    }
}

private struct UserResponse: Codable {
    let id: String
    let displayName: String?
    let mail: String?
    let userPrincipalName: String?
}
