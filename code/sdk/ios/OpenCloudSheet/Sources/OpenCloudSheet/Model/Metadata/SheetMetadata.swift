//
//  SheetMetadata.swift
//  OpenCloudSheet
//
//  Model representing a sheet within a workbook.
//  Stored in the _Metadata sheet of each workbook to track all sheets.
//

import Foundation

public struct SheetMetadata: IWorksheetRow {
    public var _rowId: String = ""
    public var _createdAt: Int64 = 0
    public var _updatedAt: Int64 = 0
    public var _rowIndex: Int = -1

    public var sheetName: String
    public var worksheetId: String
    public var description: String
    public var providerMetadataInfo: String

    public init(
        sheetName: String,
        worksheetId: String,
        description: String,
        providerMetadataInfo: String
    ) {
        self.sheetName = sheetName
        self.worksheetId = worksheetId
        self.description = description
        self.providerMetadataInfo = providerMetadataInfo
    }

    public static func getTableColumnFieldsOrder() -> [String] {
        return ["sheetName", "worksheetId", "description", "providerMetadataInfo"]
    }

    public static func getIosClassName() -> String {
        return String(reflecting: Self.self)
    }

    public static func getAndroidClassName() -> String {
        return "com.opencloudsheet.model.metadata.SheetMetadata"
    }
}
