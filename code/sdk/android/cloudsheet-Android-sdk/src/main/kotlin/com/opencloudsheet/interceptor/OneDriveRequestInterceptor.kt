package com.opencloudsheet.interceptor

import android.util.Log
import com.opencloudsheet.utilities.OneDriveWorkbookSessionHelper
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * OkHttp Interceptor for automatic Microsoft Excel Online session management.
 * Proactively adds session headers to all workbook requests for optimal performance.
 * Detects session expiry errors, invalidates cached session, creates new session, and retries request.
 */
class OneDriveRequestInterceptor(
    private val sessionHelper: OneDriveWorkbookSessionHelper
) : Interceptor {

    companion object {
        private const val TAG = "OneDriveRequestInterceptor"
        private const val SESSION_HEADER = "workbook-session-id"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (isWorkbookRequest(request) && !request.headers[SESSION_HEADER].isNullOrEmpty().not()) {
            val fileId = extractFileId(request.url.toString())
            val ownerId = extractOwnerId(request.url.toString())

            if (fileId != null && ownerId != null) {
                val sessionId = runBlocking {
                    sessionHelper.getSessionId(fileId, ownerId)
                }

                request = request.newBuilder()
                    .header(SESSION_HEADER, sessionId)
                    .build()

                Log.d(TAG, "Added session header to workbook request")
            }
        }

        var response = chain.proceed(request)

        if (isWorkbookRequest(request) && isSessionError(response)) {
            Log.i(TAG, "Session error detected, recreating session...")

            response.close()

            val fileId = extractFileId(request.url.toString())
            val ownerId = extractOwnerId(request.url.toString())

            if (fileId != null && ownerId != null) {
                sessionHelper.invalidateSession(fileId)

                val newSessionId = runBlocking {
                    sessionHelper.getSessionId(fileId, ownerId)
                }

                val newRequest = request.newBuilder()
                    .header(SESSION_HEADER, newSessionId)
                    .build()

                response = chain.proceed(newRequest)
                Log.d(TAG, "Request retried with new session")
            }
        }

        return response
    }

    private fun isWorkbookRequest(request: Request): Boolean {
        return request.url.toString().contains("/workbook")
    }

    private fun isSessionError(response: Response): Boolean {
        if (!response.isSuccessful) {
            when (response.code) {
                404, 400 -> {
                    val body = response.peekBody(Long.MAX_VALUE).string()
                    return body.contains("session", ignoreCase = true) ||
                           body.contains("sessionNotFound", ignoreCase = true) ||
                           body.contains("InvalidSessionId", ignoreCase = true)
                }
            }
        }
        return false
    }

    private fun extractFileId(url: String): String? {
        val fileIdRegex = """/drive/items\('([^']+)'\)""".toRegex()
        return fileIdRegex.find(url)?.groupValues?.get(1)
    }

    private fun extractOwnerId(url: String): String? {
        val ownerIdRegex = """/users\('([^']+)'\)""".toRegex()
        return ownerIdRegex.find(url)?.groupValues?.get(1)
    }
}
