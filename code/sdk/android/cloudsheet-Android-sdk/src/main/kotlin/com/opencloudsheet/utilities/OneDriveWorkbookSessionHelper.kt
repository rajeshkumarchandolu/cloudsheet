package com.opencloudsheet.utilities

import android.util.Log
import com.opencloudsheet.auth.IAuthenticator
import com.opencloudsheet.constants.OneDriveConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manages Microsoft Excel Online workbook sessions for performance optimization.
 * Sessions keep workbook state cached on Microsoft's servers, reducing latency.
 * Uses ConcurrentHashMap.computeIfAbsent for thread-safe session creation and caching.
 *
 * @see <a href="https://learn.microsoft.com/en-us/graph/api/workbook-createsession?view=graph-rest-1.0">Microsoft Graph: Create Session</a>
 */
class OneDriveWorkbookSessionHelper(
    private val authenticator: IAuthenticator
) {

    companion object {
        private const val TAG = "WorkbookSessionHelper"

        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

    private val sessionCache = ConcurrentHashMap<String, String>()

    suspend fun getSessionId(
        fileId: String,
        ownerId: String
    ): String {
        return sessionCache.computeIfAbsent(fileId) {
            runBlocking {
                val sessionId = createSession(fileId, ownerId)
                Log.d(TAG, "Created and cached session: $sessionId for workbook: $fileId")
                sessionId
            }
        }
    }

    suspend fun closeSession(
        fileId: String,
        ownerId: String
    ) {
        val sessionId = sessionCache.remove(fileId) ?: run {
            Log.d(TAG, "No session to close for workbook: $fileId")
            return
        }

        try {
            val url = "${OneDriveConstants.BASE_MS_GRAPH_URL}/users('$ownerId')/drive/items('$fileId')/workbook/closeSession"
            val authToken = authenticator.getAuthToken()
                ?: throw IllegalStateException("No auth token")

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("workbook-session-id", sessionId)
                .addHeader("Content-Type", "application/json")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()

            withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Failed to close session: ${response.code}")
                    } else {
                        Log.d(TAG, "Closed session: $sessionId")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing session", e)
        }
    }

    fun invalidateSession(fileId: String) {
        sessionCache.remove(fileId)
        Log.d(TAG, "Invalidated session for workbook: $fileId")
    }

    fun clearAllSessions() {
        sessionCache.clear()
        Log.d(TAG, "Cleared all session cache")
    }

    private suspend fun createSession(
        fileId: String,
        ownerId: String
    ): String {
        val url = "${OneDriveConstants.BASE_MS_GRAPH_URL}/users('$ownerId')/drive/items('$fileId')/workbook/createSession"
        val authToken = authenticator.getAuthToken()
            ?: throw IllegalStateException("No auth token")

        val requestBody = JSONObject().apply {
            put("persistChanges", true)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    throw Exception("Failed to create session: HTTP ${response.code} - $errorBody")
                }

                val responseBody = response.body.string()
                val json = JSONObject(responseBody)
                json.getString("id")
            }
        }
    }
}
