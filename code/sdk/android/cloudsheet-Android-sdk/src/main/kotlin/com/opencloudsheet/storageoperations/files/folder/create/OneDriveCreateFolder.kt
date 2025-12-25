package com.opencloudsheet.storageoperations.files.folder.create

import android.util.Log
import com.google.gson.Gson
import com.opencloudsheet.auth.IAuthenticator
import com.opencloudsheet.storageoperations.files.folder.list.IListDirectoryContents
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

class OneDriveCreateFolder(
    private val authProvider: IAuthenticator,
    private val listDirectoryContents: IListDirectoryContents
) : ICreateFolder {

    companion object {
        private const val TAG = "OneDriveCreateFolder"
    }

    override suspend fun createFolder(parentDirectory: ICloudFile?, folderName: String): ICloudFile {
        if (folderName.isBlank()) {
            Log.e(TAG, "Folder name cannot be blank")
            throw IllegalArgumentException("Folder name cannot be blank")
        }

        // Check if parent directory exists and is actually a folder
        if (parentDirectory != null && !parentDirectory.isFolder()) {
            Log.e(TAG, "Parent directory is not a folder: ${parentDirectory.getId()}")
            throw IllegalArgumentException("Parent must be a directory")
        }


        // Check if folder already exists
        val existingFolder = checkFolderExists(parentDirectory, folderName)
        if (existingFolder != null) {
            Log.d(TAG, "Folder already exists: $folderName")
            return existingFolder
        }

        // Create the new folder
        return createFolderAtLocation(parentDirectory, folderName)
    }

    private suspend fun checkFolderExists(parentDirectory: ICloudFile?, folderName: String): ICloudFile? {
        return try {
            val items = listDirectoryContents.listDirectoryContents(parentDirectory)
            items.find { it.isFolder() && it.getName() == folderName }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if folder exists: $folderName", e)
            null
        }
    }

    private suspend fun createFolderAtLocation(
        parentDirectory: ICloudFile?,
        folderName: String
    ): ICloudFile {
        val token = authProvider.getAuthToken()
            ?: throw IllegalStateException("No authentication token available")

        val url = buildCreateFolderUrl(parentDirectory)
        val requestBody = buildFolderRequestBody(folderName)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = withContext(Dispatchers.IO) {
            OneDriveClient.instance.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            throw Exception("Failed to create folder '$folderName': HTTP ${response.code} - ${response.message}")
        }

        val responseBody = response.body.string()

        return parseFolder(responseBody)
    }



    private fun buildCreateFolderUrl(parentDirectory: ICloudFile?): String {
        val baseUrl = OneDriveConstants.BASE_MS_GRAPH_URL.toString()

        return if (parentDirectory == null) {
            "$baseUrl/me/drive/root/children"
        } else {
            "$baseUrl/me/drive/items/${parentDirectory.getId()}/children"
        }
    }

    private fun buildFolderRequestBody(folderName: String): RequestBody {
        val jsonBody = JSONObject().apply {
            put("name", folderName)
            put("folder", JSONObject())
            put("@microsoft.graph.conflictBehavior", "fail")
        }

        return jsonBody.toString().toRequestBody("application/json".toMediaType())
    }

    private fun parseFolder(responseBody: String): ICloudFile {
        val driveItem = Gson().fromJson(responseBody, DriveItem::class.java)
        return OneDriveFile(driveItem)
    }
}
