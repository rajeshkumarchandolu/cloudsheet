package com.opencloudsheet.auth

import android.app.Activity
import com.opencloudsheet.model.userdetails.IUserDetails

interface IAuthenticator {
    suspend fun login(activity: Activity): Boolean
    suspend fun getAuthToken(): String?
    suspend fun logout(activity: Activity): Boolean
    suspend fun getUserDetails(): IUserDetails?
}
