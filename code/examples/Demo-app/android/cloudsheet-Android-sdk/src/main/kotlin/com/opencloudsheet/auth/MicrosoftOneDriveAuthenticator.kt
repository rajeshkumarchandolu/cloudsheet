package com.opencloudsheet.auth

import android.app.Activity
import android.util.Log
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MicrosoftOneDriveAuthenticator(
    val publicClientApplication: ISingleAccountPublicClientApplication
) : IAuthenticator {

    companion object {
        private const val TAG = "MSOneDriveAuth"
    }

    private var currentUser: IAuthenticationResult? = null

    override suspend fun login(activity: Activity): Boolean = suspendCancellableCoroutine { continuation ->
        Log.d(TAG, "Starting Microsoft OneDrive login process")

        val signInParameters = SignInParameters.builder()
            .withActivity(activity)
            .withScopes(listOf("user.read", "onedrive.readwrite"))
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    Log.d(TAG, "Login successful for user: ${authenticationResult.account.username}")
                    currentUser = authenticationResult
                    continuation.resume(true)
                }

                override fun onCancel() {
                    Log.d(TAG, "Login cancelled by user")
                    currentUser = null
                    continuation.resume(false)
                }

                override fun onError(exception: MsalException?) {
                    Log.e(TAG, "Login failed", exception)
                    currentUser = null
                    continuation.resume(false)
                }
            })
            .build()

        Log.d(TAG, "Initiating sign-in with scopes: user.read, onedrive.readwrite")
        publicClientApplication.signIn(signInParameters)

        continuation.invokeOnCancellation {
            Log.d(TAG, "Login operation was cancelled")
        }
    }

    override suspend fun getAuthToken(): String? = suspendCancellableCoroutine { continuation ->
        if (currentUser == null) {
            Log.w(TAG, "Attempting to get token but no user is logged in")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        Log.d(TAG, "Requesting silent token for user: ${currentUser?.account?.username}")

        val params = AcquireTokenSilentParameters.Builder()
            .withScopes(listOf("user.read", "onedrive.readwrite"))
            .withCallback(object : SilentAuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                    val tokenLength = authenticationResult?.accessToken?.length ?: 0
                    Log.d(TAG, "Token acquired successfully (length: $tokenLength)")
                    continuation.resume(authenticationResult?.accessToken)
                }

                override fun onError(exception: MsalException?) {
                    Log.e(TAG, "Silent token acquisition failed", exception)
                    continuation.resume(null)
                }
            })
            .build()

        publicClientApplication.acquireTokenSilent(params)

        continuation.invokeOnCancellation {
            Log.d(TAG, "Token acquisition was cancelled")
        }
    }

    override suspend fun logout(activity: Activity): Boolean {
        return try {
            Log.d(TAG, "Logging out user: ${currentUser?.account?.username}")
            publicClientApplication.signOut()
            currentUser = null
            Log.d(TAG, "Logout completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed", e)
            false
        }
    }
}
