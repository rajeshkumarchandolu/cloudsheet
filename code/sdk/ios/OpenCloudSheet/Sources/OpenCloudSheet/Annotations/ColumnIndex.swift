//
//  ColumnIndex.swift
//  OpenCloudSheet
//
//  Annotation to specify custom column order in Excel worksheets
//

import Foundation

@propertyWrapper
public struct ColumnIndex {
    public let wrappedValue: Int

    public init(wrappedValue: Int) {
        self.wrappedValue = wrappedValue
    }
}
