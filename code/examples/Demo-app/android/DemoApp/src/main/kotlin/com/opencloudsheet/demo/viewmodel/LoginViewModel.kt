package com.opencloudsheet.demo.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencloudsheet.OpenCloudSheetSdk
import com.opencloudsheet.Provider
import com.opencloudsheet.config.OneDriveConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Checking)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val availableProviders = listOf(Provider.OneDrive)
    private val providerInfoList = mutableListOf<ProviderInfo>()

    init {
        viewModelScope.launch {
            checkProviders()
        }
    }

    suspend fun checkProviders() {
        val details = try {
            OpenCloudSheetSdk.getUserDetails(Provider.OneDrive)
        } catch (e: IllegalStateException) {
            null
        }
        val userDetails = mapOf(Provider.OneDrive to details)

        // Initialize provider info list with available providers
        providerInfoList.clear()
        availableProviders.forEach { provider ->
            val details = userDetails[provider]
            providerInfoList.add(
                ProviderInfo(
                    provider = provider,
                    isSignedIn = details != null,
                    userName = details?.userName(),
                    userEmail = details?.email()
                )
            )
        }

        _uiState.value = if (providerInfoList.any { it.isSignedIn }) {
            LoginUiState.ProviderSignedIn(providerInfoList.toList())
        } else {
            LoginUiState.Ready(providerInfoList.toList())
        }
    }

    fun loginWithOneDrive(context: Context, activity: Activity) {
        _uiState.value = LoginUiState.LoggingIn(Provider.OneDrive)

        val config = OneDriveConfiguration(
            clientId = "8e09fe9e-83f2-40ab-b31a-73f09416c7bc",
            scopes = listOf("Files.ReadWrite.All"),
            redirectUri = "msauth://com.opencloudsheet.cloudsheetDemo/GTBu1vGn59kjUJkE9GXAcd8ANUE%3D"
        )

        viewModelScope.launch {
            try {
                OpenCloudSheetSdk.initializeOneDriveProvider(activity, config, "cloudsheetDemo")

                // Refresh provider info with updated user details
                checkProviders()

            } catch (e: Exception) {
                _uiState.value = LoginUiState.LoginFailed(
                    error = e.message ?: "Unknown error",
                    providers = providerInfoList.toList()
                )
            }
        }
    }

    fun retryLogin() {
        _uiState.value = LoginUiState.Ready(providerInfoList.toList())
    }
}

data class ProviderInfo(
    val provider: Provider,
    val userName: String? = null,
    val userEmail: String? = null,
    val isSignedIn: Boolean = false
)

sealed class LoginUiState {
    object Checking : LoginUiState()
    data class Ready(val providers: List<ProviderInfo>) : LoginUiState()
    data class LoggingIn(val provider: Provider) : LoginUiState()
    data class ProviderSignedIn(val providers: List<ProviderInfo>) : LoginUiState()
    data class LoginFailed(val error: String, val providers: List<ProviderInfo>) : LoginUiState()
}
