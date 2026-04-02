//
//  IWorkBook.swift
//  OpenCloudSheet
//
//  Interface for workbook operations supporting multiple sheet types
//

import Foundation

public protocol IWorkBook {
    func getId() -> String
    func getName() -> String
    func getWorkBookEntry() -> WorkBookEntry

    func initialize() async throws
    func createSheet<T: IWorksheetRow>(type: T.Type, name: String, description: String) async throws -> any IWorkSheet<T>
    func getSheets() async throws -> [SheetMetadata]
    func getSheet<T: IWorksheetRow>(name: String, type: T.Type) async throws -> (any IWorkSheet<T>)?
    func deleteSheet(name: String) async throws
}
