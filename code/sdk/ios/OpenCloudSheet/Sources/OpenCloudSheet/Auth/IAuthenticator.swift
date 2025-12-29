//
//  IAuthenticator.swift
//  Cloudsheet-ios-sdk
//
//  Protocol for cloud storage authentication
//

import Foundation

protocol IAuthenticator {
    func login(from viewController: PlatformViewController) async throws
    func getAuthToken() async throws -> String?
    func getUserDetails() async throws -> IUserDetails?
    func logout() async throws
}
