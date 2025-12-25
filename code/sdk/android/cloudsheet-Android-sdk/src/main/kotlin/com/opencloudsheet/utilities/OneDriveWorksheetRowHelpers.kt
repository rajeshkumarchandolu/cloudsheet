package com.opencloudsheet.utilities

import com.opencloudsheet.annotations.ColumnIndex
import com.opencloudsheet.model.worksheet.IWorksheetRow
import java.util.UUID

object OneDriveWorksheetRowHelpers {

    const val METADATA_COLUMN_ROW_ID = "_rowId"
    const val METADATA_COLUMN_CREATED_AT = "_createdAt"
    const val METADATA_COLUMN_UPDATED_AT = "_updatedAt"

    private const val METADATA_COLUMN_COUNT = 3
    private val METADATA_FIELDS = setOf("_rowId", "_createdAt", "_updatedAt", "_rowIndex")

    private val METADATA_COLUMNS = mapOf(
        0 to METADATA_COLUMN_ROW_ID,
        1 to METADATA_COLUMN_CREATED_AT,
        2 to METADATA_COLUMN_UPDATED_AT
    )

    fun generateUUID(): String {
        return UUID.randomUUID().toString()
    }

    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    fun <T : IWorksheetRow> validateColumnIndexAnnotations(clazz: Class<T>) {
        val userFields = clazz.declaredFields.filter { !METADATA_FIELDS.contains(it.name) }

        if (userFields.isEmpty()) {
            return
        }

        val fieldsWithAnnotation = userFields.filter { it.getAnnotation(ColumnIndex::class.java) != null }
        val fieldsWithoutAnnotation = userFields.filter { it.getAnnotation(ColumnIndex::class.java) == null }

        // If some fields have @ColumnIndex and some don't, throw an error
        if (fieldsWithAnnotation.isNotEmpty() && fieldsWithoutAnnotation.isNotEmpty()) {
            throw IllegalStateException(
                "Inconsistent @ColumnIndex usage in ${clazz.simpleName}. " +
                "Either all fields must have @ColumnIndex annotation or none should have it. " +
                "Fields with annotation: ${fieldsWithAnnotation.joinToString(", ") { it.name }}. " +
                "Fields without annotation: ${fieldsWithoutAnnotation.joinToString(", ") { it.name }}."
            )
        }

        // If all fields have @ColumnIndex, validate for gaps and duplicates
        if (fieldsWithAnnotation.size == userFields.size) {
            val indices = fieldsWithAnnotation.map { field ->
                field.name to field.getAnnotation(ColumnIndex::class.java).value
            }

            // Check for duplicate indices
            val duplicates = indices.groupBy { it.second }
                .filter { it.value.size > 1 }

            if (duplicates.isNotEmpty()) {
                val duplicateInfo = duplicates.map { (index, fields) ->
                    "index $index used by: ${fields.joinToString(", ") { it.first }}"
                }.joinToString("; ")

                throw IllegalStateException(
                    "Duplicate @ColumnIndex values found in ${clazz.simpleName}: $duplicateInfo"
                )
            }

            // Check for gaps (indices should be 0, 1, 2, ..., n-1)
            val sortedIndices = indices.map { it.second }.sorted()
            val expectedIndices = (0 until userFields.size).toList()

            if (sortedIndices != expectedIndices) {
                val missingIndices = expectedIndices.filterNot { sortedIndices.contains(it) }
                throw IllegalStateException(
                    "Gap in @ColumnIndex values in ${clazz.simpleName}. " +
                    "Expected indices: ${expectedIndices.joinToString(", ")}. " +
                    "Found indices: ${sortedIndices.joinToString(", ")}. " +
                    "Missing indices: ${missingIndices.joinToString(", ")}. " +
                    "Column indices must be sequential starting from 0 with no gaps."
                )
            }
        }
    }

    fun <T : IWorksheetRow> buildColumnMapping(clazz: Class<T>): Map<Int, String> {
        val metadataColumns = METADATA_COLUMNS

        val userFields = clazz.declaredFields.filter { !METADATA_FIELDS.contains(it.name) }

        val userColumnsMap = userFields.associate { field ->
            val userColumnIndex = field.getAnnotation(ColumnIndex::class.java)?.value
                ?: userFields.indexOf(field)

            val actualColumnIndex = userColumnIndex + METADATA_COLUMN_COUNT
            actualColumnIndex to field.name
        }

        return metadataColumns + userColumnsMap
    }

    fun validateColumnMapping(
        expectedColumns: Map<Int, String>,
        worksheetColumns: Map<Int, String>
    ) {
        expectedColumns.forEach { (columnIndex, fieldName) ->
            val worksheetColumnName = worksheetColumns[columnIndex]

            if (worksheetColumnName == null) {
                throw IllegalStateException(
                    "Column at index $columnIndex for field '$fieldName' does not exist in worksheet. " +
                    "Available columns: ${worksheetColumns.values.joinToString(", ")}. " +
                    "Verify your @ColumnIndex annotations."
                )
            }

            if (worksheetColumnName != fieldName) {
                throw IllegalStateException(
                    "Column mismatch at index $columnIndex: Expected '$fieldName' but found '$worksheetColumnName'. " +
                    "Either fix your @ColumnIndex annotations or update the worksheet structure."
                )
            }
        }
    }

    fun <T : IWorksheetRow> getMissingColumns(
        clazz: Class<T>,
        worksheetColumns: Map<Int, String>
    ): Map<Int, String> {
        val expectedColumns = buildColumnMapping(clazz)

        return expectedColumns.filterKeys { index ->
            !worksheetColumns.containsKey(index)
        }
    }
}
