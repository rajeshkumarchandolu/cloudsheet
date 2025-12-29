//
//  IWorksheetRow.swift
//  OpenCloudSheet
//
//  Protocol for all data models used with IWorkSheet.
//  Provides automatic metadata management and defines column mapping schema.
//

import Foundation

public protocol IWorksheetRow: Codable, IPlatformTypeInfo,Sendable {
    var _rowId: String {get set}
    var _createdAt: Int64 {get set}
    var _updatedAt: Int64 {get set}
    var _rowIndex: Int {get set}

    /// Returns the field names in the exact order they map to worksheet table columns.
    ///
    /// This method defines the column mapping schema for worksheet storage.
    /// Field order directly maps to worksheet columns (after metadata columns 0-2).
    ///
    /// **CRITICAL: ORDER MATTERS!**
    /// - Field names and order MUST match exactly between iOS and Android implementations
    /// - Column 3 maps to first returned field, Column 4 to second, etc.
    /// - Mismatched order causes data corruption when sharing workbooks across platforms
    ///
    /// **Requirements:**
    /// - Return only user-defined fields (exclude rowId, createdAt, updatedAt, rowIndex)
    /// - Field names must be in exact declaration order
    /// - Field names must exactly match property names (case-sensitive)
    /// - Must match Android implementation's field order
    ///
    /// **Example:**
    /// ```swift
    /// struct Expense: IWorksheetRow {
    ///     var rowId: String = ""
    ///     var createdAt: Int64 = 0
    ///     var updatedAt: Int64 = 0
    ///     var rowIndex: Int = -1
    ///
    ///     var name: String        // Column 3
    ///     var amount: Double      // Column 4
    ///     var currency: String    // Column 5
    ///
    ///     static func getTableColumnFieldsOrder() -> [String] {
    ///         return ["name", "amount", "currency"]
    ///     }
    /// }
    /// ```
    ///
    /// **Android Equivalent Must Match:**
    /// ```kotlin
    /// data class Expense(
    ///     var name: String,      // Column 3
    ///     var amount: Double,    // Column 4
    ///     var currency: String   // Column 5
    /// ) : IWorksheetRow() {
    ///     override fun getTableColumnFieldsOrder() = listOf("name", "amount", "currency")
    /// }
    /// ```
    ///
    /// - Returns: Array of field names in exact column order
    static func getTableColumnFieldsOrder() -> [String]
}


public extension IWorksheetRow {
    static func getAllFieldNames() -> [String] {
        let metadataFields = ["_rowId", "_createdAt", "_updatedAt", "_rowIndex"]
        return metadataFields + getTableColumnFieldsOrder()
    }
}
