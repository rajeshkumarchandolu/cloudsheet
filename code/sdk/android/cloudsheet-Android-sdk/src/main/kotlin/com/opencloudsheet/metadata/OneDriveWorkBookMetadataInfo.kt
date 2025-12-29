package com.opencloudsheet.metadata

/**
 * OneDrive-specific metadata for accessing workbook files.
 * This information is stored as JSON in WorkBookEntry.providerMetadataInfo.
 *
 * Enables accessing workbooks in any user's drive via:
 * /users/{ownerId}/drive/items/{fileId}
 */
data class OneDriveWorkBookMetadataInfo(
    val ownerId: String,
    val fileId: String,
    val iosClassName: String? = null,
    val androidClassName: String? = null
)
