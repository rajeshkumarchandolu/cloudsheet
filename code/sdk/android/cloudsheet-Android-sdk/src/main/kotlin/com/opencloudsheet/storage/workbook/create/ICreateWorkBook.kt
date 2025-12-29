package com.opencloudsheet.storage.workbook.create

import com.opencloudsheet.model.file.ICloudFile

interface ICreateWorkBook {
    suspend fun createWorkbook(folder: ICloudFile?, name: String): ICloudFile
}
