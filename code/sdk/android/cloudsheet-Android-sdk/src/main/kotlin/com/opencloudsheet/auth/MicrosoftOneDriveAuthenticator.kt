package com.opencloudsheet.auth

import android.app.Activity
import android.util.Log
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import com.opencloudsheet.config.MicrosoftOneDriveAuthConfiguration
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MicrosoftOneDriveAuthenticator(
    val authConfig: MicrosoftOneDriveAuthConfiguration,
    val msSingleAccountApp: ISingleAccountPublicClientApplication
) : IAuthenticator {

    private var currentUser: IAccount? = null

    companion object {
        private const val TAG = "MSOneDriveAuth"
    }


    override suspend fun login(activity: Activity): Boolean {
        val account = getSignedInUserAccountDetails()
        if (account == null) {
            return signInUser(activity)
        } else {
            currentUser = account
            return true
        }
    }

    override suspend fun getAuthToken(): String? = suspendCancellableCoroutine { continuation ->
        if (currentUser == null) {
            Log.w(TAG, "Attempting to get token but no user is logged in")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        Log.d(TAG, "Requesting silent token for user: ${currentUser?.username}")

        val params = AcquireTokenSilentParameters.Builder()
            .withScopes(authConfig.scopes)
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

        msSingleAccountApp.acquireTokenSilent(params)

        continuation.invokeOnCancellation {
            Log.d(TAG, "Token acquisition was cancelled")
        }
    }

    override suspend fun logout(activity: Activity): Boolean {
        return try {
            Log.d(TAG, "Logging out user: ${currentUser?.username}")
            msSingleAccountApp.signOut()
            currentUser = null
            Log.d(TAG, "Logout completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed", e)
            false
        }
    }

    private suspend fun getSignedInUserAccountDetails(): IAccount? =
        suspendCancellableCoroutine { continuation ->
            msSingleAccountApp.getCurrentAccountAsync(object :
                ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(activeAccount: IAccount?) {
                    continuation.resume(activeAccount)
                }

                override fun onAccountChanged(
                    priorAccount: IAccount?,
                    currentAccount: IAccount?
                ) {
                    continuation.resume(currentAccount)
                }

                override fun onError(exception: MsalException) {
                    continuation.resumeWithException(
                        RuntimeException(
                            "Error checking if user is already signed in",
                            exception
                        )
                    )
                }
            })
        }

    private suspend fun signInUser(activity: Activity): Boolean =
        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Starting Microsoft OneDrive login process")

            val signInParameters = SignInParameters.builder()
                .withActivity(activity)
                .withScopes(authConfig.scopes)
                .withCallback(object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        Log.d(
                            TAG,
                            "Login successful for user: ${authenticationResult.account.username}"
                        )
                        continuation.resume(true)
                    }

                    override fun onCancel() {
                        Log.d(TAG, "Login cancelled by user")
                        continuation.resumeWithException(Exception("Login cancelled"))
                    }

                    override fun onError(exception: MsalException?) {
                        Log.e(TAG, "Login failed", exception)
                        continuation.resumeWithException(
                            RuntimeException(
                                "Login failed with Exception: ${exception?.message}",
                                exception
                            )
                        )
                    }
                })
                .build()
            msSingleAccountApp.signIn(signInParameters)
        }

}
