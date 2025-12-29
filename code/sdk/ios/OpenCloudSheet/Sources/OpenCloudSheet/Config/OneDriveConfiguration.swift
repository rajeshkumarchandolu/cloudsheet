//
//  OneDriveConfiguration.swift
//  Cloudsheet-ios-sdk
//
//  Configuration for Microsoft OneDrive authentication
//

import Foundation

public struct OneDriveConfiguration : Sendable {
    let clientId: String
    let redirectUri: String
    let scopes: [String]

    public init(
        clientId: String,
        redirectUri: String,
        scopes: [String] = OneDriveConstants.defaultScopes
    ) {
        self.clientId = clientId
        self.redirectUri = redirectUri
        self.scopes = scopes
    }
}
