package com.opencloudsheet.model.workbook

import com.opencloudsheet.metadata.MetadataManager
import com.opencloudsheet.model.metadata.SheetMetadata
import com.opencloudsheet.model.worksheet.IWorksheetRow
import com.opencloudsheet.model.worksheet.IWorkSheet

interface IWorkBook {
    fun getId(): String
    fun getName(): String
    fun getWorkBookEntry(): MetadataManager.WorkBookEntry

    suspend fun initialize()
    suspend fun <T : IWorksheetRow> createSheet(type: Class<T>, name: String, description: String): IWorkSheet<T>
    suspend fun getSheets(): List<SheetMetadata>
    suspend fun <T : IWorksheetRow> getSheet(name: String, type: Class<T>): IWorkSheet<T>?
    suspend fun deleteSheet(name: String)
}