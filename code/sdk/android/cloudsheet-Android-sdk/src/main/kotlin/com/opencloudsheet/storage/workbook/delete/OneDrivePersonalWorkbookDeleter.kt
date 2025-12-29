package com.opencloudsheet.storage.workbook.delete

import android.util.Log
import com.opencloudsheet.auth.IAuthenticator
import com.opencloudsheet.constants.OneDriveClient
import com.opencloudsheet.constants.OneDriveConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Deletes workbooks from the current user's personal OneDrive.
 *
 * Uses the Microsoft Graph API to delete workbooks from the authenticated user's drive.
 * This differs from other implementations that may operate on different users' drives.
 *
 * @see <a href="https://learn.microsoft.com/en-us/graph/api/driveitem-delete?view=graph-rest-1.0&tabs=http">Microsoft Graph: Delete Drive Item</a>
 */
class OneDrivePersonalWorkbookDeleter(
    private val authenticator: IAuthenticator,
    private val oneDriveClient: OneDriveClient
) : IDeleteWorkBook {

    companion object {
        private const val TAG = "OneDrivePersonalWorkbookDeleter"
    }

    override suspend fun deleteWorkBook(ownerId: String, fileId: String) {
        val authToken = authenticator.getAuthToken()
            ?: throw IllegalStateException("No authentication token available")

        val deleteUrl = "${OneDriveConstants.BASE_MS_GRAPH_URL}/users('$ownerId')/drive/items('$fileId')"

        Log.d(TAG, "Deleting workbook file: $fileId")

        val request = Request.Builder()
            .url(deleteUrl)
            .addHeader("Authorization", "Bearer $authToken")
            .delete()
            .build()

        withContext(Dispatchers.IO) {
            oneDriveClient.instance.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Failed to delete workbook: ${response.code} - ${response.message} - $errorBody")
                    throw Exception("Failed to delete workbook file: HTTP ${response.code}")
                }
                Log.d(TAG, "Workbook file deleted successfully: $fileId")
            }
        }
    }
}
