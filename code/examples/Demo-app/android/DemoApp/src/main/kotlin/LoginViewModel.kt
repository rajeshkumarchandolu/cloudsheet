package com.opencloudsheet.demo

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencloudsheet.OpenCloudSheetSdk
import com.opencloudsheet.Provider
import com.opencloudsheet.config.MicrosoftOneDriveAuthConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Checking)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkProviders()
    }

    fun checkProviders() {
        val initializedProviders = OpenCloudSheetSdk.getInitializedProviders()
        _uiState.value = if (initializedProviders.isEmpty()) {
            LoginUiState.NeedsLogin
        } else {
            LoginUiState.LoggedIn(initializedProviders)
        }
    }

    fun loginWithOneDrive(context: Context, activity: Activity) {
        _uiState.value = LoginUiState.LoggingIn

        val config = MicrosoftOneDriveAuthConfiguration(
            clientId = "8e09fe9e-83f2-40ab-b31a-73f09416c7bc",
            scopes = listOf("Files.ReadWrite.All"),
            redirectUri = "msauth://com.opencloudsheet.cloudsheetDemo/VzSiQcXRmi2kyjzcA%2BmYLEtbGVs%3D"
        )

        viewModelScope.launch {
            try {
                OpenCloudSheetSdk.initializeMicrosoftOnedriveProvider(context, activity, config)
                _uiState.value = LoginUiState.LoggedIn(setOf(Provider.MicrosoftOneDrive))
            } catch (e: Exception) {
                _uiState.value = LoginUiState.LoginFailed(e.message ?: "Unknown error")
            }
        }
    }

    fun retryLogin() {
        _uiState.value = LoginUiState.NeedsLogin
    }
}

sealed class LoginUiState {
    object Checking : LoginUiState()
    object NeedsLogin : LoginUiState()
    object LoggingIn : LoginUiState()
    data class LoggedIn(val providers: Set<Provider>) : LoginUiState()
    data class LoginFailed(val error: String) : LoginUiState()
}
