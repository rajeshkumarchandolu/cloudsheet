package com.opencloudsheet.constants

import android.net.Uri


object OneDriveConstants {
    val builder: Uri.Builder = Uri.Builder()
        .scheme("https")
        .authority("graph.microsoft.com")
        .appendPath("v1.0")

    val BASE_MS_GRAPH_URL: Uri = builder.build()

    const val DEFAULT_TABLE_NAME = "Table1"
}
