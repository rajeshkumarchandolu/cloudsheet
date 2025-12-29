package com.opencloudsheet.storage.folder.list

import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.opencloudsheet.auth.IAuthenticator
import com.opencloudsheet.model.file.ICloudFile
import com.opencloudsheet.model.file.OneDriveFile
import com.opencloudsheet.constants.OneDriveClient
import com.opencloudsheet.constants.OneDriveConstants
import com.opencloudsheet.response.onedrive.ListChildrenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import java.net.URL

class OneDriveListDirectory(
    private val authProvider: IAuthenticator,
    private val oneDriveClient: OneDriveClient
) : IListDirectoryContents {

    companion object {
        private const val TAG = "OneDriveListDirectory"
        private val ROOT_CHILDREN_URI: Uri = OneDriveConstants.BASE_MS_GRAPH_URL.buildUpon()
            .appendPath("me")
            .appendPath("drive")
            .appendPath("root")
            .appendPath("children")
            .build()
        private val gson: Gson = Gson()
    }

    override suspend fun listDirectoryContents(directory: ICloudFile?): List<ICloudFile> {
        val accessToken = getAccessToken()
        val allItems = mutableListOf<ICloudFile>()
        var currentUrl: URL? = buildUrl(directory)

        // Handle pagination - fetch all pages until no more results
        while (currentUrl != null) {
            val pageResponse = fetchChildrenPage(currentUrl, accessToken)
            val pageItems = pageResponse.value.map { OneDriveFile(it) }

            allItems.addAll(pageItems)
            currentUrl = pageResponse.odataNextLink?.let { URL(it) }
        }

        return allItems
    }

    private suspend fun getAccessToken(): String {
        val token = authProvider.getAuthToken()
        if (token == null) {
            Log.e(TAG, "No authentication token available")
            throw IllegalStateException("No authentication token available")
        }
        return token
    }

    private suspend fun fetchChildrenPage(url: URL, token: String): ListChildrenResponse {
        val request = createAuthorizedRequest(url, token)
        val response = executeRequest(request)
        val responseBody = getResponseBody(response)

        return parseChildrenResponse(responseBody)
    }

    private fun createAuthorizedRequest(url: URL, token: String): Request {
        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
    }

    private suspend fun executeRequest(request: Request) = withContext(Dispatchers.IO) {
        oneDriveClient.instance.newCall(request).execute()
    }

    private fun getResponseBody(response: Response): String {
        if (!response.isSuccessful) {
            Log.e(TAG, "HTTP request failed: ${response.code} - ${response.message}")
            throw Exception("Failed to list items: HTTP ${response.code} - ${response.message}")
        }

        return response.body.string()
    }

    private fun parseChildrenResponse(responseBody: String): ListChildrenResponse {
        return gson.fromJson(responseBody, ListChildrenResponse::class.java)
    }

    private fun buildUrl(directory: ICloudFile?): URL {
        return if (directory == null) {
            URL(ROOT_CHILDREN_URI.toString())
        } else {
            val uri = OneDriveConstants.BASE_MS_GRAPH_URL.buildUpon()
                .appendPath("me")
                .appendPath("drive")
                .appendPath("items")
                .appendPath(directory.getId())
                .appendPath("children")
                .build()
            URL(uri.toString())
        }
    }

}
