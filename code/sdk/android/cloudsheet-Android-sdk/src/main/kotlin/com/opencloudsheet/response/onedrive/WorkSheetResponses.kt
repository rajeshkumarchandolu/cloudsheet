package com.opencloudsheet.response.onedrive

import com.google.gson.annotations.SerializedName

data class WorkSheetListResponse(
    @SerializedName("value")
    val value: List<WorkSheetData>
)

data class WorkSheetData(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("position")
    val position: Int? = null
)

data class WorkSheetResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("position")
    val position: Int? = null
)

data class CreateWorkSheetRequest(
    @SerializedName("name")
    val name: String
)
