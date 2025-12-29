//
//  LoginViewModel.swift
//  CloudSheetDemo
//

import Foundation
import SwiftUI
import Combine
import OpenCloudSheet

@MainActor
class LoginViewModel: ObservableObject {
    @Published var uiState: LoginUiState = .checking

    private var availableProviders: [Provider] = [.OneDrive]
    private var providerInfoList: [ProviderInfo] = []

    init() {
        Task {
            await checkProviders()
        }
    }

    func checkProviders() async {
        let details: IUserDetails?
        do {
            details = try await OpenCloudSheetSdk.getUserDetails(.OneDrive)
        } catch {
            details = nil
        }

        let userDetails: [Provider: IUserDetails?] = [.OneDrive: details]

        providerInfoList.removeAll()
        for provider in availableProviders {
            let details = userDetails[provider] ?? nil
            providerInfoList.append(
                ProviderInfo(
                    provider: provider,
                    userName: details?.displayName(),
                    userEmail: details?.email(),
                    isSignedIn: details != nil
                )
            )
        }

        if providerInfoList.contains(where: { $0.isSignedIn }) {
            uiState = .providerSignedIn(providerInfoList)
        } else {
            uiState = .ready(providerInfoList)
        }
    }

    func loginWithOneDrive(from viewController: UIViewController) {
        uiState = .loggingIn(.OneDrive)

        let config = OneDriveConfiguration(
            clientId: "8e09fe9e-83f2-40ab-b31a-73f09416c7bc",
            redirectUri: "msauth.com.opencloudsheet.cloudsheetDemo://auth",
            scopes: ["Files.ReadWrite.All"]
        )

        Task {
            do {
                try await OpenCloudSheetSdk.initializeOneDriveProvider(
                    from: viewController,
                    config: config,
                    appName: "cloudsheetDemo"
                )

                await checkProviders()
            } catch {
                uiState = .loginFailed(
                    error: error.localizedDescription,
                    providers: providerInfoList
                )
            }
        }
    }

    func retryLogin() {
        uiState = .ready(providerInfoList)
    }
}

struct ProviderInfo: Identifiable {
    let id = UUID()
    let provider: Provider
    var userName: String? = nil
    var userEmail: String? = nil
    var isSignedIn: Bool = false
}

enum LoginUiState: Equatable {
    case checking
    case ready([ProviderInfo])
    case loggingIn(Provider)
    case providerSignedIn([ProviderInfo])
    case loginFailed(error: String, providers: [ProviderInfo])

    static func == (lhs: LoginUiState, rhs: LoginUiState) -> Bool {
        switch (lhs, rhs) {
        case (.checking, .checking):
            return true
        case (.ready, .ready):
            return true
        case (.loggingIn(let lProvider), .loggingIn(let rProvider)):
            return lProvider == rProvider
        case (.providerSignedIn, .providerSignedIn):
            return true
        case (.loginFailed(let lError, _), .loginFailed(let rError, _)):
            return lError == rError
        default:
            return false
        }
    }
}
