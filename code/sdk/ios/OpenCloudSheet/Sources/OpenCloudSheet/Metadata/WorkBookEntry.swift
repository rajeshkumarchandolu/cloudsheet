//
//  WorkBookEntry.swift
//  OpenCloudSheet
//
//  Data class for metadata schema
//

import Foundation

public struct WorkBookEntry: IWorksheetRow {
    public static func getAndroidClassName() -> String {
        return "com.opencloudsheet.metadata.MetadataManager$WorkBookEntry"
    }

    public static func getTableColumnFieldsOrder() -> [String] {
        return ["description", "name", "provider", "providerMetadataInfo"]
    }

    public var _rowId: String = ""
    public var _createdAt: Int64 = 0
    public var _updatedAt: Int64 = 0
    public var _rowIndex: Int = -1

    public var name: String
    public let provider: String
    public var description: String
    public let providerMetadataInfo: String
}
