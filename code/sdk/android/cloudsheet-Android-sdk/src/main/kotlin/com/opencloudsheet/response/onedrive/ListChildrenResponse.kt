package com.opencloudsheet.response.onedrive

import com.google.gson.annotations.SerializedName

data class ListChildrenResponse(
    @SerializedName("value")
    val value: List<DriveItem>,

    @SerializedName("@odata.nextLink")
    val odataNextLink: String? = null
)

data class DriveItem(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("size")
    val size: Long? = null,

    @SerializedName("folder")
    val folder: FolderFacet? = null,

    @SerializedName("parentReference")
    val parentReference: ItemReference? = null
)

data class FolderFacet(
    @SerializedName("childCount")
    val childCount: Int? = null
)

data class ItemReference(
    @SerializedName("path")
    val path: String? = null
)
