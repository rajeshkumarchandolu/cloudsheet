package com.opencloudsheet.model.worksheet

/**
 * Interface for worksheet operations with type-safe CRUD support.
 *
 * All data classes used with IWorkSheet must implement IWorksheetRow to enable
 * efficient update and delete operations via row index tracking.
 *
 * @param T The data class type that represents a row in the worksheet.
 *          Must implement IWorksheetRow.
 */
interface IWorkSheet<T : IWorksheetRow> {
    fun getId(): String
    fun getName(): String

    suspend fun get(): List<T>
    suspend fun create(row: T): T
    suspend fun update(row: T): T
    suspend fun delete(row: T): T
}
