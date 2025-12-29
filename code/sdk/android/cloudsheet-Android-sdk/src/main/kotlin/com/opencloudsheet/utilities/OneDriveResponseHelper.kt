package com.opencloudsheet.utilities

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.opencloudsheet.annotations.ColumnIndex
import com.opencloudsheet.model.worksheet.IWorksheetRow

object OneDriveResponseHelper {
    private val _gson: Gson by lazy {
        Gson()
    }

    fun toString(obj: Any): String = _gson.toJson(obj)

    fun parse(response: String): JsonElement? {
        return JsonParser.parseString(response)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T = _gson.fromJson(json, clazz)

    fun <T : IWorksheetRow> parseTableRows(jsonArray: JsonArray?, clazz: Class<T>): List<T> {
        if (jsonArray == null) {
            return emptyList()
        }
        val result = mutableListOf<T>()
        for (i in 0 until jsonArray.size()) {
            val rowObject = jsonArray[i].asJsonObject
            val parsedRow = parseRow(rowObject, clazz, i)
            result.add(parsedRow)
        }
        return result
    }

    fun <T : IWorksheetRow> parseRow(rowObject: com.google.gson.JsonObject, clazz: Class<T>, fallbackIndex: Int = 0): T {
        val valuesArray = rowObject.getAsJsonArray("values")
        val rowValues = valuesArray[0].asJsonArray

        // Get the actual row index from rowObject
        val rowIndex = rowObject.get("index")?.asInt ?: fallbackIndex

        val jsonObject = buildJsonObjectForDeserialization(rowValues, clazz)
        val instance = _gson.fromJson(jsonObject, clazz)
        instance.setRowIndex(rowIndex)
        return instance
    }

    /**
     * Build a JsonObject from valuesArray for Gson deserialization.
     * Uses OneDriveWorksheetRowHelpers.buildColumnMapping to get field mapping.
     */
    private fun <T : IWorksheetRow> buildJsonObjectForDeserialization(
        valuesArray: JsonArray,
        clazz: Class<T>
    ): com.google.gson.JsonObject {
        // Get complete column mapping (index -> field name)
        val columnMapping = OneDriveWorksheetRowHelpers.buildColumnMapping(clazz)

        // Build JSON object from valuesArray using column mapping
        val jsonObject = com.google.gson.JsonObject()
        columnMapping.forEach { (columnIndex, fieldName) ->
            if (columnIndex < valuesArray.size()) {
                val value = valuesArray[columnIndex]
                jsonObject.add(fieldName, value)
            }
        }

        return jsonObject
    }

    fun <T : IWorksheetRow> toValueArray(obj: T): List<Any?> {
        val clazz = obj::class.java
        val metadataColumnCount = 3
        val userFieldNames = obj.getTableColumnFieldsOrder()

        val columnMap = mutableMapOf<Int, Any?>(
            0 to obj.getUniqueRowId(),
            1 to obj.getCreatedAt(),
            2 to obj.getUpdatedAt()
        )

        userFieldNames.forEachIndexed { index, fieldName ->
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            val actualColumnIndex = index + metadataColumnCount
            columnMap[actualColumnIndex] = field.get(obj)
        }

        val maxIndex = columnMap.keys.maxOrNull() ?: -1
        return (0..maxIndex).map { columnMap[it] }
    }
}

