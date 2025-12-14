package com.opencloudsheet

import com.opencloudsheet.auth.MicrosoftOneDriveAuthenticator

class MicrosoftOneDriveStorageProvider(
    private val authenticator: MicrosoftOneDriveAuthenticator
) : IStorageProvider
