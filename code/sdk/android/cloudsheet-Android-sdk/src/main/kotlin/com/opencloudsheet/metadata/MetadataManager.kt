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
    private val workbook: IWorkBook,  // Metadata.xlsx as IWorkBook
    private val createWorkBookInstance: (WorkBookEntry) -> IWorkBook
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
        val provider: String,               // Provider name (e.g., "OneDrive", "GoogleDrive")
        val description: String,            // User description
        val providerMetadataInfo: String    // JSON stringified provider-specific metadata (ownerId, fileId, etc.)
    ) : IWorksheetRow() {
        override fun getIosClassName() = "OpenCloudSheet.WorkBookEntry"
        override fun getTableColumnFieldsOrder() = listOf("description", "name", "provider", "providerMetadataInfo")
    }

    suspend fun initialize() {
        // Initialize the workbook first
        workbook.initialize()

        val existingSheet = workbook.getSheet(WORKBOOKS_SHEET_NAME, WorkBookEntry::class.java)
        if (existingSheet != null) {
            workBooksSheet = existingSheet
            return
        }
        Log.i(TAG, "creating the $WORKBOOKS_SHEET_NAME sheet in the ${workbook.getName()}")
        workBooksSheet = workbook.createSheet(WorkBookEntry::class.java, WORKBOOKS_SHEET_NAME, "Workbooks metadata")
    }

    suspend fun addWorkBook(
        name: String,
        provider: Provider,
        description: String,
        providerMetadataInfo: String
    ) {
        val entry = WorkBookEntry(
            name = name,
            provider = provider.name,
            description = description,
            providerMetadataInfo = providerMetadataInfo
        )
        // Use IWorkSheet.create() - standard CRUD
        workBooksSheet!!.create(entry)
    }

    suspend fun listWorkBooks(): List<IWorkBook> {
        val entries = workBooksSheet!!.get()

        return entries.map { entry ->
            val workbook = createWorkBookInstance(entry)
            workbook.initialize()
            workbook
        }
    }

    suspend fun updateWorkBook(workbookEntry: WorkBookEntry) {
        workBooksSheet!!.update(workbookEntry)
    }

    suspend fun deleteWorkBook(workbookEntry: WorkBookEntry) {
        workBooksSheet!!.delete(workbookEntry)
    }

}
