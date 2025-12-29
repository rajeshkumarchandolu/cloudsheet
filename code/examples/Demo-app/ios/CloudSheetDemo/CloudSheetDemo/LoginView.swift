//
//  LoginView.swift
//  CloudSheetDemo
//

import SwiftUI
import OpenCloudSheet

struct LoginView: View {
    @StateObject private var viewModel = LoginViewModel()
    @State private var navigateToWorkBooks = false

    private func getProvidersFromState() -> [ProviderInfo] {
        switch viewModel.uiState {
        case .ready(let providers),
             .providerSignedIn(let providers),
             .loginFailed(_, let providers):
            return providers
        case .loggingIn:
            // Try to get from previous state, or return empty
            return []
        case .checking:
            return []
        }
    }
    
    @ViewBuilder
    private func renderProviderList(providers: [ProviderInfo], currentLoggingInProvider: Provider?) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                ForEach(providers) { providerInfo in
                    ProviderCard(
                        providerInfo: providerInfo,
                        isLoggingIn: providerInfo.provider == currentLoggingInProvider,
                        onSignIn: {
                            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                               let rootViewController = windowScene.windows.first?.rootViewController {
                                viewModel.loginWithOneDrive(from: rootViewController)
                            }
                        }
                    )
                }

                if case .loginFailed(let error, _) = viewModel.uiState {
                    VStack {
                        Text(error)
                            .font(.body)
                            .foregroundColor(.red)
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(8)
                    }
                }
            }
            .padding(.horizontal)
        }

        Spacer()

        Button(action: {
            navigateToWorkBooks = true
        }) {
            Text("Continue")
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(providers.contains(where: { $0.isSignedIn }) ? Color.blue : Color.gray)
                .cornerRadius(8)
        }
        .disabled(!providers.contains(where: { $0.isSignedIn }))
        .padding(.horizontal)
        .padding(.bottom, 24)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                VStack(spacing: 8) {
                    Text("Welcome to CloudSheet")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)

                    Text("Connect your cloud storage providers")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 32)

                switch viewModel.uiState {
                case .checking:
                    Spacer()
                    ProgressView()
                    Spacer()

                case .ready(let providers):
                    renderProviderList(providers: providers, currentLoggingInProvider: nil)
                    
                case .loggingIn(let loggingInProvider):
                    // Get providers from previous state or use empty array
                    let providers = getProvidersFromState()
                    renderProviderList(providers: providers, currentLoggingInProvider: loggingInProvider)
                    
                case .providerSignedIn(let providers):
                    renderProviderList(providers: providers, currentLoggingInProvider: nil)
                    
                case .loginFailed(_, let providers):
                    renderProviderList(providers: providers, currentLoggingInProvider: nil)
                }
            }
            .navigationBarHidden(true)
            .navigationDestination(isPresented: $navigateToWorkBooks) {
                WorkBooksView()
            }
        }
    }
}

struct ProviderCard: View {
    let providerInfo: ProviderInfo
    let isLoggingIn: Bool
    let onSignIn: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(providerInfo.provider == .OneDrive ? "Microsoft OneDrive" : "Unknown")
                            .font(.headline)
                            .fontWeight(.semibold)

                        if providerInfo.isSignedIn {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.blue)
                        }
                    }

                    if providerInfo.isSignedIn {
                        Text("User: \(providerInfo.userName ?? "Signed in")")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                if isLoggingIn {
                    ProgressView()
                } else if !providerInfo.isSignedIn {
                    Button(action: onSignIn) {
                        Text("Sign in")
                            .font(.subheadline)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                    }
                    .buttonStyle(.borderedProminent)
                }
            }
            .padding()
        }
        .background(Color(.systemBackground))
        .cornerRadius(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(providerInfo.isSignedIn ? Color.blue : Color(.separator), lineWidth: providerInfo.isSignedIn ? 2 : 1)
        )
    }
}

#Preview {
    LoginView()
}
