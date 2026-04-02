package com.opencloudsheet.model.metadata

import com.opencloudsheet.model.worksheet.IWorksheetRow

/**
 * Model representing a sheet within a workbook.
 * Stored in the _Metadata sheet of each workbook to track all sheets.
 */
data class SheetMetadata(
    val sheetName: String,
    val worksheetId: String,
    val description: String,
    val providerMetadataInfo: String
) : IWorksheetRow() {

    override fun getTableColumnFieldsOrder(): List<String> {
        return listOf("sheetName", "worksheetId", "description", "providerMetadataInfo")
    }

    override fun getAndroidClassName(): String {
        return this::class.java.name
    }

    override fun getIosClassName(): String {
        return "OpenCloudSheet.SheetMetadata"
    }
}
