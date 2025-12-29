//
//  IUserDetails.swift
//  Cloudsheet-ios-sdk
//
//  Protocol for cloud storage user details
//

import Foundation

public protocol IUserDetails: Sendable {
    func id() -> String
    func displayName() -> String
    func email() -> String
}
