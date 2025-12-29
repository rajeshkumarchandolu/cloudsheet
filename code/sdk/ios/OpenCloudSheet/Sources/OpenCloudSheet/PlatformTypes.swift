//
//  PlatformTypes.swift
//  OpenCloudSheet
//
//  Platform-specific type aliases for cross-platform support
//

#if os(iOS)
import UIKit
public typealias PlatformViewController = UIViewController
#elseif os(macOS)
import AppKit
public typealias PlatformViewController = NSViewController
#endif
