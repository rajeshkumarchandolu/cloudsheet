//
//  IPlatformTypeInfo.swift
//  OpenCloudSheet
//
//  Protocol for providing platform-specific type information to enable cross-platform workbook compatibility
//

import Foundation

public protocol IPlatformTypeInfo {
    static func getIosClassName() -> String
    static func getAndroidClassName() -> String
}

public extension IPlatformTypeInfo {
    static func getIosClassName() -> String {
        return String(reflecting: Self.self)
    }
}
