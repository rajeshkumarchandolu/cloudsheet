package com.opencloudsheet

import android.app.Activity
import android.content.Context
import com.microsoft.identity.client.PublicClientApplication
import com.opencloudsheet.auth.MicrosoftOneDriveAuthenticator
import com.opencloudsheet.config.MicrosoftOneDriveAuthConfiguration
import org.json.JSONObject
import java.io.File

class OpenCloudSheetSdk {
    val providerMap: MutableMap<Provider, IStorageProvider> = mutableMapOf()

    suspend fun initializeMicrosoftOnedriveProvider(
        context: Context,
        activity: Activity,
        config: MicrosoftOneDriveAuthConfiguration
    ) {
        try {
            // Create MSAL config JSON
            val configJsonString = """
            {
                "client_id": "${config.clientId}",
                "authorization_user_agent": "DEFAULT",
                "redirect_uri": "${config.redirectUri}",
                "authorities": [
                    {
                        "type": "AAD",
                        "authority_url": "${config.authority}"
                    }
                ]
            }
            """.trimIndent()

            val configJson = JSONObject(configJsonString)

            // Create temp config file
            val configFile = File(context.cacheDir, "msal_config.json")
            try {
                configFile.writeText(configJson.toString())
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create MSAL config file: ${e.message}", e)
            }

            // Create PublicClientApplication
            val publicClientApplication = try {
                PublicClientApplication.createSingleAccountPublicClientApplication(
                    context,
                    configFile
                )
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Failed to create PublicClientApplication: ${e.message}",
                    e
                )
            }

            // Create and register provider
            val authenticator = MicrosoftOneDriveAuthenticator(publicClientApplication)
            authenticator.login(activity)
            val storageProvider = MicrosoftOneDriveStorageProvider(authenticator)
            providerMap[Provider.MicrosoftOneDrive] = storageProvider

        } catch (e: IllegalArgumentException) {
            throw e // Re-throw validation errors
        } catch (e: Exception) {
            throw RuntimeException(
                "Failed to initialize Microsoft OneDrive provider: ${e.message}",
                e
            )
        }
    }


    fun getInitializedProviders(): Set<Provider> {
        return providerMap.keys
    }

    fun getStorageProvider(provider: Provider): IStorageProvider? {
        return providerMap[provider]
    }

}
