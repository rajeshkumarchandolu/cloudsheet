//
//  OneDriveUserDetails.swift
//  Cloudsheet-ios-sdk
//
//  Microsoft OneDrive user details implementation
//

import Foundation

struct OneDriveUserDetails: IUserDetails, Codable {
    private let userId: String
    private let name: String
    private let userEmail: String

    init(userId: String, displayName: String, email: String) {
        self.userId = userId
        self.name = displayName
        self.userEmail = email
    }

    func id() -> String {
        return userId
    }

    func displayName() -> String {
        return name
    }

    func email() -> String {
        return userEmail
    }
}
