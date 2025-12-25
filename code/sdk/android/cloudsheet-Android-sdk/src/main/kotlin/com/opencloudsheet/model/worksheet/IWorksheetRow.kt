package com.opencloudsheet.model.worksheet

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
 * Metadata columns (_rowId, _createdAt, _updatedAt) are automatically added as the first 3 columns.
 * Use @ColumnIndex annotation if you need custom column mapping for your data fields.
 *
 * Example usage:
 * ```
 * data class Employee(
 *     val name: String,
 *     val department: String
 * ) : IWorksheetRow()
 * ```
 *
 * Worksheet columns will be:
 * [_rowId, _createdAt, _updatedAt, name, department]
 */
abstract class IWorksheetRow {
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
}
