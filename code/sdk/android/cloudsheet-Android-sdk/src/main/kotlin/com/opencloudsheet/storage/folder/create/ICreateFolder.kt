package com.opencloudsheet.storage.folder.create

import com.opencloudsheet.model.file.ICloudFile

interface ICreateFolder {
    suspend fun createFolder(parentDirectory: ICloudFile?, folderName: String): ICloudFile
}
