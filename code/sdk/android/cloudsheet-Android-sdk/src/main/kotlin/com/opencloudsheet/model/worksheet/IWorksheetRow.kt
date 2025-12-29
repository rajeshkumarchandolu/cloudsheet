package com.opencloudsheet.model.worksheet

import com.opencloudsheet.protocols.IPlatformTypeInfo

/**
 * Abstract base class for all data classes used with IWorkSheet.
 *
 * This class provides automatic metadata management:
 * - Row index (physical position in worksheet)
 * - Unique row ID (logical identifier using UUIDv7)
 * - Timestamps (created/updated in Unix milliseconds)
 *
 * All metadata fields are automatically managed by the SDK and stored as the first 3 columns.
 *
 * **Field Declaration Order:**
 * By default, fields in your data class should match the column order in the worksheet.
 * Metadata columns (rowId, createdAt, updatedAt) are automatically added as the first 3 columns.
 * Use @ColumnIndex annotation if you need custom column mapping for your data fields.
 *
 * **Cross-Platform Support:**
 * Implementing classes must provide companion object with IPlatformTypeInfo to enable
 * cross-platform workbook compatibility between iOS and Android.
 *
 * Example usage:
 * ```
 * data class Employee(
 *     val name: String,
 *     val department: String
 * ) : IWorksheetRow() {
 *     companion object : IPlatformTypeInfo {
 *         override fun getIosClassName() = "MyApp.Employee"
 *         override fun getAndroidClassName() = Employee::class.java.name
 *     }
 * }
 * ```
 *
 * Worksheet columns will be:
 * [rowId, createdAt, updatedAt, name, department]
 */
abstract class IWorksheetRow: IPlatformTypeInfo {
    private var _rowId: String = ""
    private var _createdAt: Long = 0L
    private var _updatedAt: Long = 0L
    private var _rowIndex: Int = -1

    fun getUniqueRowId(): String = _rowId
    fun setUniqueRowId(id: String) {
        _rowId = id
    }

    fun getCreatedAt(): Long = _createdAt
    fun setCreatedAt(timestamp: Long) {
        _createdAt = timestamp
    }

    fun getUpdatedAt(): Long = _updatedAt
    fun setUpdatedAt(timestamp: Long) {
        _updatedAt = timestamp
    }

    fun getRowIndex(): Int = _rowIndex
    fun setRowIndex(index: Int) {
        _rowIndex = index
    }

    abstract fun getTableColumnFieldsOrder(): List<String>
}
