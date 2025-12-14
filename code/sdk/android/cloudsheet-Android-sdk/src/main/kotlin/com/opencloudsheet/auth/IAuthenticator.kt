package com.opencloudsheet.auth

import android.app.Activity

interface IAuthenticator {

    suspend fun login(activity: Activity): Boolean
    suspend fun getAuthToken(): String?
    suspend fun logout(activity: Activity): Boolean
}
