//
//  IWorkBook.swift
//  OpenCloudSheet
//
//  Interface for workbook operations
//

import Foundation

public protocol IWorkBook {
    associatedtype T: IWorksheetRow
    func getId() -> String
    func getName() -> String
    func getWorkBookEntry() -> WorkBookEntry

    func getWorkSheets() async throws -> [any IWorkSheet]
    func createWorkSheet(sheetName: String) async throws -> any IWorkSheet
    func deleteWorkSheet(sheet: any IWorkSheet) async throws
    func renameWorksheet(sheet: any IWorkSheet, newName: String) async throws
}
