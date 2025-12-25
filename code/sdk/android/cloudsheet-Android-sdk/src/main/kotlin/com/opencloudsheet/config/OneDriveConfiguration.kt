package com.opencloudsheet.config

data class OneDriveConfiguration(
    val clientId: String,
    val redirectUri: String,
    val scopes: List<String>
) {
    init {
        require(clientId.isNotBlank()) { "Client ID cannot be blank" }
        require(redirectUri.isNotBlank()) { "Redirect URI cannot be blank" }
    }
}
