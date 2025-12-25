package com.opencloudsheet.model.workbook

import android.util.Log
import com.google.gson.Gson
import com.opencloudsheet.auth.IAuthenticator
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
import org.json.JSONObject

/**
 * Microsoft OneDrive implementation of IWorkBook<T>.
 *
 * Provides operations on an Excel workbook stored in OneDrive:
 * - List worksheets
 * - Create worksheet (with automatic schema generation via reflection)
 * - Delete worksheet
 * - Rename worksheet
 */
class OneDriveWorkBook<T : IWorksheetRow>(
    private val metadataInfo: OneDriveWorkBookMetadataInfo,
    private val clazz: Class<T>,
    private val authenticator: IAuthenticator,
    private val workBookEntry: MetadataManager.WorkBookEntry
) : IWorkBook<T> {

    companion object {
        private const val TAG = "OneDriveWorkBook"
        private val gson = Gson()
    }

    override fun getId(): String = metadataInfo.fileId
    override fun getName(): String = workBookEntry.name
    override fun getWorkBookEntry(): MetadataManager.WorkBookEntry = workBookEntry

    override suspend fun getWorkSheets(): List<IWorkSheet<T>> {
        val token = authenticator.getAuthToken()
            ?: throw IllegalStateException("No authentication token available")

        val url = getWorksheetOperationUrl()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        val response = withContext(Dispatchers.IO) {
            OneDriveClient.instance.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            Log.e(TAG, "Failed to get worksheets: ${response.code} - ${response.message}")
            throw Exception("Failed to get worksheets: HTTP ${response.code}")
        }

        val responseBody = response.body.string()

        val workSheetListResponse = gson.fromJson(responseBody, WorkSheetListResponse::class.java)

        // Always filter out "Sheet1" - Excel's default worksheet
        // This handles all scenarios including when rename fails during creation
        return workSheetListResponse.value
            .filter { it.name != "Sheet1" }
            .map { workSheetData ->
                OneDriveWorkSheet(metadataInfo, workSheetData, clazz, authenticator)
            }
    }

    override suspend fun createWorkSheet(sheetName: String): IWorkSheet<T> {
        // Validate input
        if (sheetName.isBlank()) {
            Log.e(TAG, "Sheet name cannot be blank")
            throw IllegalArgumentException("Sheet name cannot be blank")
        }

        val token = authenticator.getAuthToken()
            ?: throw IllegalStateException("No authentication token available")

        // Check if this is the first worksheet being created
        val existingSheets = getWorkSheets()

        // If there's only one sheet named "Sheet1", rename it instead of creating a new one
        if (existingSheets.size == 1 && existingSheets[0].getName() == "Sheet1") {
            Log.d(TAG, "Found default Sheet1, renaming it to: $sheetName")
            val defaultSheet = existingSheets[0]
            renameWorksheet(defaultSheet, sheetName)

            // Return the renamed sheet with updated data
            val workSheetData = WorkSheetData(
                id = defaultSheet.getId(),
                name = sheetName,
                position = 0
            )
            return OneDriveWorkSheet(metadataInfo, workSheetData, clazz, authenticator)
        }

        // Step 1: Create worksheet via Microsoft Graph API
        val createUrl = getWorksheetOperationUrl()

        val createRequestBody = CreateWorkSheetRequest(sheetName)
        val createRequestJson = gson.toJson(createRequestBody).toRequestBody("application/json".toMediaType())

        val createRequest = Request.Builder()
            .url(createUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(createRequestJson)
            .build()

        val createResponse = withContext(Dispatchers.IO) {
            OneDriveClient.instance.newCall(createRequest).execute()
        }

        if (!createResponse.isSuccessful) {
            Log.e(TAG, "Failed to create worksheet: ${createResponse.code} - ${createResponse.message}")
            throw Exception("Failed to create worksheet: HTTP ${createResponse.code}")
        }

        val createResponseBody = createResponse.body.string()

        val workSheetResponse = gson.fromJson(createResponseBody, WorkSheetResponse::class.java)

        // Step 2: Return OneDriveWorkSheet instance
        val workSheetData = WorkSheetData(
            id = workSheetResponse.id,
            name = workSheetResponse.name,
            position = workSheetResponse.position
        )

        return OneDriveWorkSheet(metadataInfo, workSheetData, clazz, authenticator)
    }

    override suspend fun deleteWorkSheet(sheet: IWorkSheet<T>) {
        val token = authenticator.getAuthToken()
            ?: throw IllegalStateException("No authentication token available")

        val url = "${getWorksheetOperationUrl()}/${sheet.getId()}"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .delete()
            .build()

        val response = withContext(Dispatchers.IO) {
            OneDriveClient.instance.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            Log.e(TAG, "Failed to delete worksheet: ${response.code} - ${response.message}")
            throw Exception("Failed to delete worksheet: HTTP ${response.code}")
        }

        Log.d(TAG, "Worksheet ${sheet.getName()} deleted successfully")
    }

    override suspend fun renameWorksheet(sheet: IWorkSheet<T>, newName: String) {
        // Validate input
        if (newName.isBlank()) {
            Log.e(TAG, "New sheet name cannot be blank")
            throw IllegalArgumentException("New sheet name cannot be blank")
        }

        val token = authenticator.getAuthToken()
            ?: throw IllegalStateException("No authentication token available")

        val url = "${getWorksheetOperationUrl()}/${sheet.getId()}"

        val requestBody = JSONObject().apply {
            put("name", newName)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .patch(requestBody)
            .build()

        val response = withContext(Dispatchers.IO) {
            OneDriveClient.instance.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            Log.e(TAG, "Failed to rename worksheet: ${response.code} - ${response.message}")
            throw Exception("Failed to rename worksheet: HTTP ${response.code}")
        }

        Log.d(TAG, "Worksheet renamed from ${sheet.getName()} to $newName successfully")
    }

    private fun getBaseWorkbookUrl(): String {
        return "${OneDriveConstants.BASE_MS_GRAPH_URL}/users('${metadataInfo.ownerId}')/drive/items('${metadataInfo.fileId}')/workbook"
    }

    private fun getWorksheetOperationUrl(): String {
        return "${getBaseWorkbookUrl()}/worksheets"
    }
}
