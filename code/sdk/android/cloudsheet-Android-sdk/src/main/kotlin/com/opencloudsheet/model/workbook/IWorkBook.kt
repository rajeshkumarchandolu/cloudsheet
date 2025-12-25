package com.opencloudsheet.model.workbook

import com.opencloudsheet.metadata.MetadataManager
import com.opencloudsheet.model.worksheet.IWorksheetRow
import com.opencloudsheet.model.worksheet.IWorkSheet

interface IWorkBook<T : IWorksheetRow> {
    fun getId(): String
    fun getName(): String
    fun getWorkBookEntry(): MetadataManager.WorkBookEntry
    suspend fun getWorkSheets(): List<IWorkSheet<T>>
    suspend fun createWorkSheet(sheetName: String): IWorkSheet<T>
    suspend fun deleteWorkSheet(sheet: IWorkSheet<T>)
    suspend fun renameWorksheet(sheet: IWorkSheet<T>, newName: String)
}