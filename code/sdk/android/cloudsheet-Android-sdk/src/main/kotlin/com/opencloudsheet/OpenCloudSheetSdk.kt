package com.opencloudsheet

import android.app.Activity
import android.content.Context
import android.util.Log
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import com.opencloudsheet.auth.MicrosoftOneDriveAuthenticator
import com.opencloudsheet.config.MicrosoftOneDriveAuthConfiguration
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OpenCloudSheetSdk {
    private const val TAG = "OpenCloudSheetSdk"

    private val providerMap: MutableMap<Provider, IStorageProvider> = mutableMapOf()

    suspend fun initializeMicrosoftOnedriveProvider(
        context: Context,
        activity: Activity,
        config: MicrosoftOneDriveAuthConfiguration
    ) {
        val configFile = createTempMsAuthConfigFile(config, context)
        val msApplication = createSingleAccountPublicClientApplication(context, configFile)
        // Create and register provider
        val authenticator = MicrosoftOneDriveAuthenticator(config, msApplication)
        authenticator.login(activity)
        val storageProvider = MicrosoftOneDriveStorageProvider(authenticator)
        providerMap[Provider.MicrosoftOneDrive] = storageProvider
    }

    fun getInitializedProviders(): Set<Provider> {
        return providerMap.keys
    }

    fun getStorageProvider(provider: Provider): IStorageProvider? {
        return providerMap[provider]
    }

    private suspend fun createSingleAccountPublicClientApplication(
        context: Context,
        configFile: File
    ): ISingleAccountPublicClientApplication = suspendCancellableCoroutine { continuation ->
        Log.d(
            TAG,
            "Creating single account public client application with config file: ${configFile.absolutePath}"
        )
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            configFile,
            object :
                IPublicClientApplication.ISingleAccountApplicationCreatedListener {
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

    private fun createTempMsAuthConfigFile(
        config: MicrosoftOneDriveAuthConfiguration,
        context: Context
    ): File {
        val configJsonString = """
                    {
                      "client_id" : "${config.clientId}",
                      "redirect_uri" : "${config.redirectUri}",
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
        val configJson = JSONObject(configJsonString)
        val configFile = File(context.cacheDir, "msal_config.json")
        try {
            configFile.writeText(configJson.toString())
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create MSAL config file: ${e.message}", e)
        }
        return configFile
    }

}

