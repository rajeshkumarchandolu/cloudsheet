//
//  IWorkSheet.swift
//  OpenCloudSheet
//
//  Interface for worksheet operations with type-safe CRUD support
//

import Foundation

public protocol IWorkSheet<T> {
    associatedtype T: IWorksheetRow

    func getId() -> String
    func getName() -> String

    func get() async throws -> [T]
    func create(row: T) async throws -> T
    func update(row: T) async throws -> T
    func delete(row: T) async throws -> T
}
