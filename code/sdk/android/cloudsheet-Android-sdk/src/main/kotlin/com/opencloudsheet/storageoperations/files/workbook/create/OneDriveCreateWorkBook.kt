package com.opencloudsheet.storageoperations.files.workbook.create

import android.util.Log
import com.google.gson.Gson
import com.opencloudsheet.auth.IAuthenticator
import com.opencloudsheet.model.file.ICloudFile
import com.opencloudsheet.model.file.OneDriveFile
import com.opencloudsheet.constants.OneDriveClient
import com.opencloudsheet.constants.OneDriveConstants
import com.opencloudsheet.response.onedrive.DriveItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class OneDriveCreateWorkBook(
    private val authProvider: IAuthenticator
) : ICreateWorkBook {

    companion object {
        private const val TAG = "OneDriveCreateWorkBook"
        private val gson = Gson()
    }

    override suspend fun createWorkbook(folder: ICloudFile?, name: String): ICloudFile {
        // Validate inputs
        if (name.isBlank()) {
            Log.e(TAG, "Sheet name cannot be blank")
            throw IllegalArgumentException("Sheet name cannot be blank")
        }

        // Validate folder is actually a folder
        if (folder != null && !folder.isFolder()) {
            Log.e(TAG, "Provided folder must be a folder, not a file: ${folder.getId()}")
            throw IllegalArgumentException("Provided folder must be a folder, not a file")
        }

        val token = authProvider.getAuthToken()
        if (token == null) {
            Log.e(TAG, "No authentication token available")
            throw IllegalStateException("No authentication token available")
        }

        // Ensure name has .xlsx extension
        val fileName = if (name.endsWith(".xlsx", ignoreCase = true)) name else "$name.xlsx"

        // Build URL
        val url = buildCreateFileUrl(folder)

        // Build request body
        val requestBody = buildExcelFileRequestBody(fileName)

        // Create HTTP request
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        // Execute request
        val response = withContext(Dispatchers.IO) {
            OneDriveClient.instance.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            Log.e(TAG, "HTTP request failed: ${response.code} - ${response.message}")
            throw Exception("Failed to create Excel file: HTTP ${response.code} - ${response.message}")
        }

        // Parse response
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            Log.e(TAG, "Empty response body received")
            throw Exception("Empty response body")
        }

        return parseCreatedFile(responseBody)
    }

    private fun buildCreateFileUrl(folder: ICloudFile?): String {
        val baseUrl = OneDriveConstants.BASE_MS_GRAPH_URL.toString()

        return if (folder == null) {
            // Create in root: POST /me/drive/root/children
            "$baseUrl/me/drive/root/children"
        } else {
            // Create in specific folder: POST /me/drive/items/{folder-id}/children
            "$baseUrl/me/drive/items/${folder.getId()}/children"
        }
    }

    private fun buildExcelFileRequestBody(fileName: String): RequestBody {
        val jsonBody = JSONObject().apply {
            put("name", fileName)
            put("file", JSONObject())
            put("@microsoft.graph.conflictBehavior", "fail")
        }

        return jsonBody.toString().toRequestBody("application/json".toMediaType())
    }

    private fun parseCreatedFile(responseBody: String): ICloudFile {
        val driveItem = gson.fromJson(responseBody, DriveItem::class.java)
        return OneDriveFile(driveItem)
    }
}
