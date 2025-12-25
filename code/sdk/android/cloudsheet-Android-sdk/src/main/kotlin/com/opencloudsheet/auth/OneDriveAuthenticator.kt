package com.opencloudsheet.auth

import android.app.Activity
import android.util.Log
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import com.opencloudsheet.config.OneDriveConfiguration
import com.opencloudsheet.model.userdetails.IUserDetails
import com.opencloudsheet.model.userdetails.OneDriveUserDetails
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OneDriveAuthenticator(
    val authConfig: OneDriveConfiguration
) : IAuthenticator {

    private var msSingleAccountApp: ISingleAccountPublicClientApplication? = null

    private var currentUser: OneDriveUserDetails? = null

    companion object {
        private const val TAG = "MSOneDriveAuth"
    }

    override suspend fun login(activity: Activity): Boolean {
        // Lazy initialize MSAL app on first call
        if (msSingleAccountApp == null) {
            msSingleAccountApp = createSingleAccountPublicClientApplication(activity)
        }

        val account = getSignedInUserAccountDetails()
        if (account == null) {
            return signInUser(activity)
        } else {
            currentUser = OneDriveUserDetails(account)
            return true
        }
    }

    override suspend fun getAuthToken(): String? = suspendCancellableCoroutine { continuation ->
        if (msSingleAccountApp == null) {
            continuation.resumeWithException(IllegalStateException("Authenticator not initialized. Call login() first."))
            return@suspendCancellableCoroutine
        }
        if (currentUser == null) {
            Log.w(TAG, "Attempting to get token but no user is logged in")
            continuation.resumeWithException(Exception("No user is logged in"))
            return@suspendCancellableCoroutine
        }

        Log.d(TAG, "Requesting silent token for user: ${currentUser?.userName()}")

        if (currentUser?.getAccount() == null) {
            Log.e(TAG, "No account found in MSAL even though currentUser is set")
            continuation.resumeWithException(Exception("Account not found in MSAL"))
            return@suspendCancellableCoroutine
        }

        val silentParameters = AcquireTokenSilentParameters.Builder()
            .forAccount(currentUser?.getAccount())
            .withScopes(authConfig.scopes)
            .fromAuthority(currentUser?.getAccount()?.authority)
            .withCallback(object : SilentAuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                    val tokenLength = authenticationResult?.accessToken?.length ?: 0
                    Log.d(TAG, "Token acquired successfully (length: $tokenLength)")
                    continuation.resume(authenticationResult?.accessToken)
                }

                override fun onError(exception: MsalException?) {
                    Log.e(TAG, "Silent token acquisition failed", exception)
                    val error = exception ?: Exception("Failed to acquire token silently")
                    continuation.resumeWithException(error)
                }
            })
            .build()

        msSingleAccountApp!!.acquireTokenSilentAsync(silentParameters)

        continuation.invokeOnCancellation {
            Log.d(TAG, "Token acquisition was cancelled")
        }
    }

    override suspend fun logout(activity: Activity): Boolean =
        suspendCancellableCoroutine { continuation ->
            if (msSingleAccountApp == null) {
                throw IllegalStateException("Authenticator not initialized. Call login() first.")
            }
            Log.d(TAG, "Logging out user: ${currentUser?.userName()}")
            msSingleAccountApp!!.signOut(object :
                ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    currentUser = null
                    Log.d(TAG, "Logout completed successfully")
                    continuation.resume(true)
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "Logout failed", exception)
                    continuation.resume(false)
                }
            })
        }

    override suspend fun getUserDetails(): IUserDetails? {
        return currentUser
    }

    private suspend fun getSignedInUserAccountDetails(): IAccount? =
        suspendCancellableCoroutine { continuation ->
            if (msSingleAccountApp == null) {
                continuation.resumeWithException(IllegalStateException("Authenticator not initialized. Call login() first."))
                return@suspendCancellableCoroutine
            }
            msSingleAccountApp!!.getCurrentAccountAsync(object :
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
            if (msSingleAccountApp == null) {
                continuation.resumeWithException(IllegalStateException("Authenticator not initialized. Call login() first."))
                return@suspendCancellableCoroutine
            }
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
                        currentUser =
                            OneDriveUserDetails(authenticationResult.account)
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
            msSingleAccountApp!!.signIn(signInParameters)
        }

    private suspend fun createSingleAccountPublicClientApplication(
        activity: Activity
    ): ISingleAccountPublicClientApplication = suspendCancellableCoroutine { continuation ->
        Log.d(TAG, "Creating single account public client application")

        // Create temp config file (using activity's context)
        val configFile = creatMslConfigFile(activity, continuation)

        PublicClientApplication.createSingleAccountPublicClientApplication(
            activity.applicationContext,
            configFile,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication?) {
                    Log.d(TAG, "MSAL single account application created successfully")
                    if (application == null)
                        continuation.resumeWithException(IllegalStateException("MSAL single account application is null"))
                    else
                        continuation.resume(application)
                }

                override fun onError(exception: MsalException?) {
                    Log.e(TAG, "Failed to create MSAL single account application", exception)
                    continuation.resumeWithException(
                        IllegalStateException(
                            "Failed to create MSAL application",
                            exception
                        )
                    )
                }
            })
    }

    private fun creatMslConfigFile(
        activity: Activity,
        continuation: CancellableContinuation<ISingleAccountPublicClientApplication>
    ): File {
        val configJsonString = """
                {
                  "client_id" : "${authConfig.clientId}",
                  "redirect_uri" : "${authConfig.redirectUri}",
                  "account_mode" : "SINGLE",
                  "authorization_user_agent" : "DEFAULT",
                  "authorities": [
                    {
                      "type": "AAD",
                      "audience": {
                        "type": "AzureADandPersonalMicrosoftAccount",
                        "tenant_id": "common"
                      }
                    }
                  ]
                }
            """.trimIndent()

        val configFile = File(activity.cacheDir, "msal_config.json")
        try {
            configFile.writeText(configJsonString)
        } catch (e: Exception) {
            continuation.resumeWithException(
                IllegalStateException("Failed to create MSAL config file: ${e.message}", e)
            )
        }
        return configFile
    }

}
