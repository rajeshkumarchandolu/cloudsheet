package com.opencloudsheet.config

data class MicrosoftOneDriveAuthConfiguration(
    val clientId: String,
    val authority: String,
    val redirectUri: String
) {
    init {
        require(clientId.isNotBlank()) { "Client ID cannot be blank" }
        require(authority.isNotBlank()) { "Authority cannot be blank" }
        require(redirectUri.isNotBlank()) { "Redirect URI cannot be blank" }
    }
}
