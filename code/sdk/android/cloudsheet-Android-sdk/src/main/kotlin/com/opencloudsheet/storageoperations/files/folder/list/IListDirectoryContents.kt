package com.opencloudsheet.storageoperations.files.folder.list

import com.opencloudsheet.model.file.ICloudFile

interface IListDirectoryContents {
    suspend fun listDirectoryContents(directory: ICloudFile?): List<ICloudFile>
}
