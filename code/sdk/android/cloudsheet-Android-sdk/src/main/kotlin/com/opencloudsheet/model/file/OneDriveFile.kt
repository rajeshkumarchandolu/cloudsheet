package com.opencloudsheet.model.file

import com.opencloudsheet.response.onedrive.DriveItem

class OneDriveFile(
    private val item: DriveItem
) : ICloudFile {
    override fun getId(): String = item.id
    override fun getName(): String = item.name
    override fun isFolder(): Boolean = item.folder != null
    override fun getPath(): String? = item.parentReference?.path
    override fun getSize(): Long? = item.size?.takeIf { it >= 0 }
}