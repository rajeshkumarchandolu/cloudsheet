package com.opencloudsheet.metadata

import android.util.Log
import com.opencloudsheet.Provider
import com.opencloudsheet.model.workbook.IWorkBook
import com.opencloudsheet.model.worksheet.IWorkSheet
import com.opencloudsheet.model.worksheet.IWorksheetRow

/**
 * Manager for the Metadata.xlsx workbook that tracks all user workbooks.
 *
 * Key Insight: MetadataManager is just a wrapper over IWorkBook<WorkBookEntry>.
 * Metadata.xlsx is treated like any other workbook - it uses the standard
 * IWorkBook/IWorkSheet abstractions instead of direct API calls.
 *
 * This design:
 * - Reuses existing abstractions (no provider-specific code)
 * - Works with any provider (Microsoft, Google, etc.)
 * - Uses reflection for schema (same as other worksheets)
 */
class MetadataManager(
    private val workbook: IWorkBook<WorkBookEntry>,  // Metadata.xlsx as IWorkBook<WorkBookEntry>
    private val createWorkBookInstance: (Class<out IWorksheetRow>, WorkBookEntry) -> IWorkBook<*>
) {
    companion object {
        private const val WORKBOOKS_SHEET_NAME = "WorkBooks"
        private const val TAG = "MetadataManager"
    }

    private var workBooksSheet: IWorkSheet<WorkBookEntry>? = null

    /**
     * Data class for metadata schema.
     * Each workbook tracked in Metadata.xlsx has one row with these fields.
     * Schema auto-created via reflection when worksheet is initialized.
     */
    data class WorkBookEntry(
        val name: String,                   // Workbook name
        val className: String,              // Full class name (e.g., "com.example.Employee")
        val provider: String,               // Provider name (e.g., "OneDrive", "GoogleDrive")
        val description: String,            // User description
        val providerMetadataInfo: String    // JSON stringified provider-specific metadata (ownerId, fileId, etc.)
    ) : IWorksheetRow()

    suspend fun initialize() {
        val existingSheets = workbook.getWorkSheets()
        val existingWorkBooksSheet = existingSheets.find { it.getName() == WORKBOOKS_SHEET_NAME }

        if (existingWorkBooksSheet != null) {
            workBooksSheet = existingWorkBooksSheet
            return
        }
        Log.i(TAG, "creating the $WORKBOOKS_SHEET_NAME sheet in the ${workbook?.getName()}")
        workBooksSheet = workbook.createWorkSheet(WORKBOOKS_SHEET_NAME)
    }

    suspend fun addWorkBook(
        name: String,
        className: String,
        provider: Provider,
        description: String,
        providerMetadataInfo: String
    ) {
        val entry = WorkBookEntry(
            name = name,
            className = className,
            provider = provider.name,
            description = description,
            providerMetadataInfo = providerMetadataInfo
        )
        // Use IWorkSheet.create() - standard CRUD
        workBooksSheet!!.create(entry)
    }

    /**
     * List all tracked workbooks as IWorkBook instances.
     *
     * @return List of workbook instances
     */
    suspend fun listWorkBooks(): List<IWorkBook<*>> {
        val entries = workBooksSheet!!.get()

        return entries.mapNotNull { entry ->
            try {
                val clazz = Class.forName(entry.className) as Class<out IWorksheetRow>
                createWorkBookInstance(clazz, entry)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load workbook: ${entry.name}", e)
                null
            }
        }
    }

    /**
     * Delete a workbook from metadata tracking.
     * Note: This only removes the metadata entry, not the actual workbook file.
     */
    suspend fun deleteWorkBook(workbookEntry: WorkBookEntry) {
        // Use IWorkSheet.delete() - standard CRUD
        workBooksSheet!!.delete(workbookEntry)
    }

}
