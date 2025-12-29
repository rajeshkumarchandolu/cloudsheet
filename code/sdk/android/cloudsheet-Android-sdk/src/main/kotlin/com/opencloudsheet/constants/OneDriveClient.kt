package com.opencloudsheet.constants

import com.opencloudsheet.interceptor.OneDriveRequestInterceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Configures OkHttpClient for OneDrive API calls with custom interceptors.
 * Provides centralized configuration for timeouts and interceptors.
 */
class OneDriveClient(
    private val requestInterceptor: OneDriveRequestInterceptor
) {
    val instance: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(requestInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
