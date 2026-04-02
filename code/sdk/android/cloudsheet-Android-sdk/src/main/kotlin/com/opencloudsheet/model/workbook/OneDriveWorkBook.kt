package com.opencloudsheet.model.workbook

import android.util.Log
import com.google.gson.Gson
import com.opencloudsheet.auth.IAuthenticator
import com.opencloudsheet.model.metadata.SheetMetadata
import com.opencloudsheet.model.worksheet.IWorksheetRow
import com.opencloudsheet.model.worksheet.IWorkSheet
import com.opencloudsheet.model.worksheet.OneDriveWorkSheet
import com.opencloudsheet.constants.OneDriveClient
import com.opencloudsheet.constants.OneDriveConstants
import com.opencloudsheet.metadata.MetadataManager
import com.opencloudsheet.metadata.OneDriveWorkBookMetadataInfo
import com.opencloudsheet.response.onedrive.WorkSheetListResponse
import com.opencloudsheet.response.onedrive.CreateWorkSheetRequest
import com.opencloudsheet.response.onedrive.WorkSheetData
import com.opencloudsheet.response.onedrive.WorkSheetResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

/**
 * Microsoft OneDrive implementation of IWorkBook supporting multiple sheet types.
 *
 * Provides operations on an Excel workbook stored in OneDrive:
 * - Create typed sheets with automatic schema generation
 * - List all sheets via metadata
 * - Get specific sheet by name and type
 * - Delete sheets
 *
 * @see <a href="https://learn.microsoft.com/en-us/graph/api/resources/workbookworksheet?view=graph-rest-1.0">Microsoft Graph: Worksheet</a>
 */
class OneDriveWorkBook(
    private val metadataInfo: OneDriveWorkBookMetadataInfo,
    private val authenticator: IAuthenticator,
    private val workBookEntry: MetadataManager.WorkBookEntry,
    private val oneDriveClient: OneDriveClient
) : IWorkBook {

    companion object {
        private const val TAG = "OneDriveWorkBook"
        private const val METADATA_SHEET_NAME = "_Metadata"
        private val gson = Gson()
    }

    private var metadataSheet: OneDriveWorkSheet<SheetMetadata>? = null

    override fun getId(): String = metadataInfo.fileId
    override fun getName(): String = workBookEntry.name
    override fun getWorkBookEntry(): MetadataManager.WorkBookEntry = workBookEntry

    override suspend fun initialize() {
        val worksheets = getAllWorkSheetsFromExcel()

        val existingMetadataSheet = worksheets.find { it.name == METADATA_SHEET_NAME }
        if (existingMetadataSheet != null) {
            Log.i(TAG, "Found existing _Metadata sheet")
            metadataSheet = OneDriveWorkSheet(
                metadataInfo,
                existingMetadataSheet,
                SheetMetadata::class.java,
                authenticator,
                oneDriveClient
            )
        } else {
            Log.i(TAG, "Creating new _Metadata sheet")
            val createdWorksheet = createWorkSheetInExcel(METADATA_SHEET_NAME)
            metadataSheet = OneDriveWorkSheet(
                metadataInfo,
                createdWorksheet,
                SheetMetadata::class.java,
                authenticator,
                oneDriveClient
            )
            // Make the metadata sheet very hidden
            setWorkSheetVisibility(createdWorksheet.id, "VeryHidden")
        }
    }

    // MARK: - IWorkBook Protocol Methods

    override suspend fun <T : IWorksheetRow> createSheet(
        type: Class<T>,
        name: String,
        description: String
    ): IWorkSheet<T> {
        require(name.isNotBlank()) { "Sheet name cannot be blank" }

        val metadataSheet = this.metadataSheet
            ?: throw IllegalStateException("Workbook not initialized. Call initialize() first.")

        val existingSheets = metadataSheet.get()
        if (existingSheets.any { it.sheetName == name }) {
            throw IllegalArgumentException("Sheet with name '$name' already exists")
        }

        val workSheetData = createWorkSheetInExcel(name)

        val worksheet = OneDriveWorkSheet(
            metadataInfo,
            workSheetData,
            type,
            authenticator,
            oneDriveClient
        )

        val tempInstance = gson.fromJson("{}", type)
        val providerMetadataInfo = """{"iosClassName":"${tempInstance.getIosClassName()}","androidClassName":"${tempInstance.getAndroidClassName()}"}"""

        val sheetMetadata = SheetMetadata(
            sheetName = name,
            worksheetId = workSheetData.id,
            description = description,
            providerMetadataInfo = providerMetadataInfo
        )
        metadataSheet.create(sheetMetadata)

        Log.i(TAG, "Created sheet '$name' with type ${type.simpleName}")
        return worksheet
    }

    override suspend fun getSheets(): List<SheetMetadata> {
        val metadataSheet = this.metadataSheet
            ?: throw IllegalStateException("Workbook not initialized. Call initialize() first.")

        return metadataSheet.get()
    }

    override suspend fun <T : IWorksheetRow> getSheet(name: String, type: Class<T>): IWorkSheet<T>? {
        val metadataSheet = this.metadataSheet
            ?: throw IllegalStateException("Workbook not initialized. Call initialize() first.")

        val sheets = metadataSheet.get()
        val sheetMetadata = sheets.find { it.sheetName == name } ?: return null

        val workSheetData = WorkSheetData(
            id = sheetMetadata.worksheetId,
            name = sheetMetadata.sheetName,
            position = 0
        )

        return OneDriveWorkSheet(
            metadataInfo,
            workSheetData,
            type,
            authenticator,
            oneDriveClient
        )
    }

    override suspend fun deleteSheet(name: String) {
        val metadataSheet = this.metadataSheet
            ?: throw IllegalStateException("Workbook not initialized. Call initialize() first.")

        val sheets = metadataSheet.get()
        val sheetMetadata = sheets.find { it.sheetName == name }
            ?: throw IllegalArgumentException("Sheet with name '$name' not found")

        deleteWorkSheetInExcel(sheetMetadata.worksheetId)
        metadataSheet.delete(sheetMetadata)

        Log.i(TAG, "Deleted sheet '$name'")
    }

    // MARK: - Internal Excel Operations

    private suspend fun getAllWorkSheetsFromExcel(): List<WorkSheetData> {
        val token = authenticator.getAuthToken()
            ?: throw IllegalStateException("No authentication token available")

        val url = getWorksheetOperationUrl()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            oneDriveClient.instance.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to get worksheets: ${response.code} - ${response.message}")
                    throw Exception("Failed to get worksheets: HTTP ${response.code}")
                }

                val responseBody = response.body.string()
                gson.fromJson(responseBody, WorkSheetListResponse::class.java).value
            }
        }
    }

    private suspend fun createWorkSheetInExcel(sheetName: String): WorkSheetData {
        val token = authenticator.getAuthToken()
            ?: throw IllegalStateException("No authentication token available")

        val createUrl = getWorksheetOperationUrl()

        val createRequestBody = CreateWorkSheetRequest(sheetName)
        val createRequestJson = gson.toJson(createRequestBody).toRequestBody("application/json".toMediaType())

        val createRequest = Request.Builder()
            .url(createUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(createRequestJson)
            .build()

        return withContext(Dispatchers.IO) {
            oneDriveClient.instance.newCall(createRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to create worksheet: ${response.code} - ${response.message}")
                    throw Exception("Failed to create worksheet: HTTP ${response.code}")
                }

                val responseBody = response.body.string()
                val workSheetResponse = gson.fromJson(responseBody, WorkSheetResponse::class.java)

                WorkSheetData(
                    id = workSheetResponse.id,
                    name = workSheetResponse.name,
                    position = workSheetResponse.position
                )
            }
        }
    }

    private suspend fun deleteWorkSheetInExcel(worksheetId: String) {
        val token = authenticator.getAuthToken()
            ?: throw IllegalStateException("No authentication token available")

        val url = "${getWorksheetOperationUrl()}/$worksheetId"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .delete()
            .build()

        withContext(Dispatchers.IO) {
            oneDriveClient.instance.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to delete worksheet: ${response.code} - ${response.message}")
                    throw Exception("Failed to delete worksheet: HTTP ${response.code}")
                }
            }
        }
    }

    private fun getBaseWorkbookUrl(): String {
        return "${OneDriveConstants.BASE_MS_GRAPH_URL}/users('${metadataInfo.ownerId}')/drive/items('${metadataInfo.fileId}')/workbook"
    }

    private fun getWorksheetOperationUrl(): String {
        return "${getBaseWorkbookUrl()}/worksheets"
    }

    private suspend fun setWorkSheetVisibility(worksheetId: String, visibility: String) {
        val token = authenticator.getAuthToken()
            ?: throw IllegalStateException("No authentication token available")

        val url = "${getWorksheetOperationUrl()}/$worksheetId"
        val requestBody = """{"visibility":"$visibility"}""".toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .patch(requestBody)
            .build()

        withContext(Dispatchers.IO) {
            oneDriveClient.instance.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to set worksheet visibility: ${response.code} - ${response.message}")
                    throw Exception("Failed to set worksheet visibility: HTTP ${response.code}")
                }
                Log.i(TAG, "Set worksheet $worksheetId visibility to $visibility")
            }
        }
    }
}
