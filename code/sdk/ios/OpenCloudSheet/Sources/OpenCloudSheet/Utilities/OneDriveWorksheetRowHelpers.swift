//
//  OneDriveWorksheetRowHelpers.swift
//  OpenCloudSheet
//
//  Utility functions for worksheet row operations
//

import Foundation

class OneDriveWorksheetRowHelpers {
    static let METADATA_COLUMN_ROW_ID = "_rowId"
    static let METADATA_COLUMN_CREATED_AT = "_createdAt"
    static let METADATA_COLUMN_UPDATED_AT = "_updatedAt"

    private static let METADATA_COLUMN_COUNT = 3
    private static let METADATA_FIELDS: Set<String> = ["_rowId", "_createdAt", "_updatedAt", "_rowIndex"]

    private static let METADATA_COLUMNS: [Int: String] = [
        0: "_rowId",
        1: "_createdAt",
        2: "_updatedAt"
    ]

    static func generateUUID() -> String {
        return UUID().uuidString
    }

    static func getCurrentTimestamp() -> Int64 {
        return Int64(Date().timeIntervalSince1970 * 1000)
    }

    static func validateColumnIndexAnnotations<T: IWorksheetRow>(_ type: T.Type) throws {
        // In Swift with Codable, column order is determined by CodingKeys declaration order
        // No validation needed as Swift's type system enforces it
    }

    static func buildColumnMapping<T: IWorksheetRow>(_ type: T.Type) -> [Int: String] {
        let metadataColumns = METADATA_COLUMNS
        let userFieldNames = type.getTableColumnFieldsOrder()

        var userColumnsMap: [Int: String] = [:]
        for (index, fieldName) in userFieldNames.enumerated() {
            let actualColumnIndex = index + METADATA_COLUMN_COUNT
            userColumnsMap[actualColumnIndex] = fieldName
        }

        return metadataColumns.merging(userColumnsMap) { _, new in new }
    }

    static func validateColumnMapping(
        expectedColumns: [Int: String],
        worksheetColumns: [Int: String]
    ) throws {
        for (columnIndex, fieldName) in expectedColumns {
            guard let worksheetColumnName = worksheetColumns[columnIndex] else {
                throw NSError(
                    domain: "OneDriveWorksheetRowHelpers",
                    code: -1,
                    userInfo: [
                        NSLocalizedDescriptionKey: "Column at index \(columnIndex) for field '\(fieldName)' does not exist in worksheet. Available columns: \(worksheetColumns.values.joined(separator: ", "))"
                    ]
                )
            }

            if worksheetColumnName != fieldName {
                throw NSError(
                    domain: "OneDriveWorksheetRowHelpers",
                    code: -2,
                    userInfo: [
                        NSLocalizedDescriptionKey: "Column mismatch at index \(columnIndex): Expected '\(fieldName)' but found '\(worksheetColumnName)'"
                    ]
                )
            }
        }
    }

    static func getMissingColumns<T: IWorksheetRow>(
        _ type: T.Type,
        worksheetColumns: [Int: String]
    ) -> [Int: String] {
        let expectedColumns = buildColumnMapping(type)

        return expectedColumns.filter { index, _ in
            !worksheetColumns.keys.contains(index)
        }
    }
}
