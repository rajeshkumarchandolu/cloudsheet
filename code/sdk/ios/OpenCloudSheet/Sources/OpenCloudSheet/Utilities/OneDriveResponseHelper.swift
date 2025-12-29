//
//  OneDriveResponseHelper.swift
//  OpenCloudSheet
//
//  Helper for parsing OneDrive API responses
//

import Foundation

class OneDriveResponseHelper {
    static func parse(_ response: String) -> [String: Any]? {
        guard let data = response.data(using: .utf8) else { return nil }
        return try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    }

    static func parseTableRows<T: IWorksheetRow>(
        _ jsonArray: [[String: Any]]?,
        _ type: T.Type
    ) -> [T] {
        guard let jsonArray = jsonArray else { return [] }

        var result: [T] = []
        for (index, rowObject) in jsonArray.enumerated() {
            if let parsedRow = parseRow(rowObject, type, fallbackIndex: index) {
                result.append(parsedRow)
            }
        }
        return result
    }

    static func parseRow<T: IWorksheetRow>(
        _ rowObject: [String: Any],
        _ type: T.Type,
        fallbackIndex: Int = 0
    ) -> T? {
        guard let valuesArray = rowObject["values"] as? [[Any]] else { return nil }
        guard let rowValues = valuesArray.first else { return nil }

        let rowIndex = rowObject["index"] as? Int ?? fallbackIndex

        var instance = buildInstanceFromValues(rowValues, type)
        instance._rowIndex = rowIndex
        return instance
    }

    private static func buildInstanceFromValues<T: IWorksheetRow>(
        _ valuesArray: [Any],
        _ type: T.Type
    ) -> T {
        let columnMapping = OneDriveWorksheetRowHelpers.buildColumnMapping(type)
                        
        var jsonDict: [String: Any] = [:]
        jsonDict["_rowIndex"] = -1
        for (columnIndex, fieldName) in columnMapping {
            guard columnIndex < valuesArray.count else { continue }
            let value = valuesArray[columnIndex]
            jsonDict[fieldName] = value
        }

        do {
            let jsonData = try JSONSerialization.data(withJSONObject: jsonDict)
            let instance = try JSONDecoder().decode(type, from: jsonData)
            return instance
        } catch {
            print("Failed to deserialize row: \(error)")
            fatalError("Failed to deserialize row of type \(type)")
        }
    }


    static func toValueArray<T: IWorksheetRow>(_ obj: T) -> [Any?] {
        let metadataColumnCount = 3
        let userFieldNames = type(of: obj).getTableColumnFieldsOrder()

        var columnMap: [Int: Any?] = [
            0: obj._rowId,
            1: obj._createdAt,
            2: obj._updatedAt
        ]

        let mirror = Mirror(reflecting: obj)
        for (index, fieldName) in userFieldNames.enumerated() {
            let actualColumnIndex = index + metadataColumnCount
            if let child = mirror.children.first(where: { $0.label == fieldName }) {
                columnMap[actualColumnIndex] = child.value
            }
        }

        let maxIndex = columnMap.keys.max() ?? -1
        return (0...maxIndex).map { columnMap[$0] ?? NSNull() }
    }
}
