package com.opencloudsheet.metadata

/**
 * OneDrive-specific metadata for accessing workbook files.
 * This information is stored as JSON in WorkBookEntry.providerMetadataInfo.
 *
 * Enables accessing workbooks in any user's drive via:
 * /users/{ownerId}/drive/items/{fileId}
 */
data class OneDriveWorkBookMetadataInfo(
    val ownerId: String,  // Microsoft user ID who owns the file
    val fileId: String    // OneDrive file/item ID
)
