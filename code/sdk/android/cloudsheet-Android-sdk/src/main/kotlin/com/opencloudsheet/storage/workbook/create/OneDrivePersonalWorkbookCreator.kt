package com.opencloudsheet.storage.workbook.create

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

/**
 * Creates Excel workbooks in the current user's personal OneDrive.
 *
 * Uses the Microsoft Graph API /me endpoint to create workbooks in the authenticated user's drive.
 * This differs from other implementations that may operate on different users' drives.
 *
 * @see <a href="https://learn.microsoft.com/en-us/graph/api/driveitem-post-children?view=graph-rest-1.0">Microsoft Graph: Create Drive Item</a>
 */
class OneDrivePersonalWorkbookCreator(
    private val authProvider: IAuthenticator,
    private val oneDriveClient: OneDriveClient
) : ICreateWorkBook {

    companion object {
        private const val TAG = "OneDrivePersonalWorkbookCreator"
        private val gson = Gson()
    }

    override suspend fun createWorkbook(folder: ICloudFile?, name: String): ICloudFile {
        if (name.isBlank()) {
            Log.e(TAG, "Sheet name cannot be blank")
            throw IllegalArgumentException("Sheet name cannot be blank")
        }

        if (folder != null && !folder.isFolder()) {
            Log.e(TAG, "Provided folder must be a folder, not a file: ${folder.getId()}")
            throw IllegalArgumentException("Provided folder must be a folder, not a file")
        }

        val token = authProvider.getAuthToken()
        if (token == null) {
            Log.e(TAG, "No authentication token available")
            throw IllegalStateException("No authentication token available")
        }

        val fileName = if (name.endsWith(".xlsx", ignoreCase = true)) name else "$name.xlsx"

        val url = buildCreateFileUrl(folder)

        val requestBody = buildExcelFileRequestBody(fileName)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            oneDriveClient.instance.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP request failed: ${response.code} - ${response.message}")
                    throw Exception("Failed to create Excel file: HTTP ${response.code} - ${response.message}")
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    Log.e(TAG, "Empty response body received")
                    throw Exception("Empty response body")
                }

                parseCreatedFile(responseBody)
            }
        }
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
