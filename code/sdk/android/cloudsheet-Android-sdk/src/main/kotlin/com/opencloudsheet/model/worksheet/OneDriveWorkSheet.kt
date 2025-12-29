package com.opencloudsheet.model.worksheet

import android.util.Log
import com.opencloudsheet.auth.IAuthenticator
import com.opencloudsheet.constants.OneDriveClient
import com.opencloudsheet.constants.OneDriveConstants
import com.opencloudsheet.metadata.OneDriveWorkBookMetadataInfo
import com.opencloudsheet.response.onedrive.WorkSheetData
import com.opencloudsheet.utilities.OneDriveResponseHelper
import com.opencloudsheet.utilities.OneDriveWorksheetRowHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

/**
 * OneDrive implementation of worksheet operations using Microsoft Graph API.
 * Handles Excel table operations for CRUD on worksheet data.
 *
 * @see <a href="https://learn.microsoft.com/en-us/graph/api/resources/workbooktable?view=graph-rest-1.0">Microsoft Graph: Workbook Table</a>
 */
class OneDriveWorkSheet<T : IWorksheetRow>(
    private val metadataInfo: OneDriveWorkBookMetadataInfo,
    private val workSheetData: WorkSheetData,
    private val clazz: Class<T>,
    private val authenticator: IAuthenticator,
    private val oneDriveClient: OneDriveClient
) : IWorkSheet<T> {

    companion object {
        private const val TAG = "OneDriveWorkSheet"
    }

    private var isInitialized: Boolean = false
    private var tableId: String? = null

    override fun getId(): String = workSheetData.id
    override fun getName(): String = workSheetData.name

    override suspend fun get(): List<T> {
        ensureInitialized()

        val request = Request.Builder()
            .url(getTableRowsUrl())
            .addHeader("Authorization", "Bearer ${getAuthToken()}")
            .get()
            .build()

        val responseBody = executeRequest(request)
        val response = OneDriveResponseHelper.parse(responseBody)?.asJsonObject
        val rows = response?.getAsJsonArray("value")

        return OneDriveResponseHelper.parseTableRows(rows, clazz)
    }

    override suspend fun create(row: T): T {
        ensureInitialized()

        row.setUniqueRowId(OneDriveWorksheetRowHelpers.generateUUID())
        val now = OneDriveWorksheetRowHelpers.getCurrentTimestamp()
        row.setCreatedAt(now)
        row.setUpdatedAt(now)

        val rowValues = OneDriveResponseHelper.toValueArray(row)

        val requestJson = JSONObject().apply {
            put("values", org.json.JSONArray().apply {
                put(org.json.JSONArray().apply {
                    rowValues.forEach { put(it) }
                })
            })
        }.toString()

        val addRowUrl = "${getTableRowsUrl()}/add"

        val request = Request.Builder()
            .url(addRowUrl)
            .addHeader("Authorization", "Bearer ${getAuthToken()}")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        executeRequest(request)
        Log.d(TAG, "Row created successfully with ID: ${row.getUniqueRowId()}")
        return row
    }

    override suspend fun update(row: T): T {
        ensureInitialized()

        if (row.getUniqueRowId().isEmpty()) {
            throw IllegalArgumentException("Row ID not set. Ensure the row was fetched via get() before updating.")
        }

        val rowIndex = findRowIndexByUniqueId(row.getUniqueRowId())
        row.setUpdatedAt(OneDriveWorksheetRowHelpers.getCurrentTimestamp())

        val rowValues = OneDriveResponseHelper.toValueArray(row)

        val requestJson = JSONObject().apply {
            put("values", org.json.JSONArray().apply {
                put(org.json.JSONArray().apply {
                    rowValues.forEach { put(it) }
                })
            })
        }.toString()

        val updateRowUrl = "${getTableRowsUrl()}/itemAt(index=$rowIndex)"

        val request = Request.Builder()
            .url(updateRowUrl)
            .addHeader("Authorization", "Bearer ${getAuthToken()}")
            .addHeader("Content-Type", "application/json")
            .patch(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        executeRequest(request)
        Log.d(TAG, "Row updated successfully. ID: ${row.getUniqueRowId()}, Index: $rowIndex")
        return row
    }

    override suspend fun delete(row: T): T {
        ensureInitialized()

        if (row.getUniqueRowId().isEmpty()) {
            throw IllegalArgumentException("Row ID not set. Ensure the row was fetched via get() before deleting.")
        }

        val rowIndex = findRowIndexByUniqueId(row.getUniqueRowId())

        val deleteRowUrl = "${getTableRowsUrl()}/itemAt(index=$rowIndex)"

        val request = Request.Builder()
            .url(deleteRowUrl)
            .addHeader("Authorization", "Bearer ${getAuthToken()}")
            .delete()
            .build()

        executeRequest(request)
        Log.d(TAG, "Row deleted successfully. ID: ${row.getUniqueRowId()}, Index: $rowIndex")
        return row
    }

    private suspend fun initialize() {
        if (isInitialized) {
            return
        }

        OneDriveWorksheetRowHelpers.validateColumnIndexAnnotations(clazz)

        val tableExists = checkTableExists()

        if (!tableExists) {
            createTable()
        } else {
            val excelColumns = fetchTableColumns()
            val missingColumns = OneDriveWorksheetRowHelpers.getMissingColumns(clazz, excelColumns)

            if (missingColumns.isNotEmpty()) {
                Log.d(TAG, "Detected ${missingColumns.size} new columns in schema, adding them")
                missingColumns.forEach { (index, columnName) ->
                    createColumn(columnName, index)
                }
            }

            fixDefaultColumnNamesIfNeeded()
        }

        val excelColumns = fetchTableColumns()
        val expectedColumns = OneDriveWorksheetRowHelpers.buildColumnMapping(clazz)
        OneDriveWorksheetRowHelpers.validateColumnMapping(expectedColumns, excelColumns)

        Log.d(TAG, "Worksheet ${workSheetData.name} initialized successfully")
        isInitialized = true
    }

    private suspend fun checkTableExists(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(getTablesListUrl())
            .addHeader("Authorization", "Bearer ${getAuthToken()}")
            .get()
            .build()

        oneDriveClient.instance.newCall(request).execute().use { response ->
            when {
                !response.isSuccessful -> false
                else -> {
                    val responseBody = response.body.string()
                    val json = JSONObject(responseBody)
                    val tablesArray = json.optJSONArray("value")

                    if (tablesArray != null && tablesArray.length() > 0) {
                        val firstTable = tablesArray.getJSONObject(0)
                        if (!firstTable.has("id")) {
                            throw IllegalStateException("Table exists in worksheet but API response missing 'id' field. Cannot proceed without table ID.")
                        }
                        tableId = firstTable.getString("id")
                        Log.d(TAG, "Found existing table with ID: $tableId")
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    private suspend fun createTable() {
        Log.d(TAG, "Creating table")

        val columnMapping = OneDriveWorksheetRowHelpers.buildColumnMapping(clazz)
        val maxColumnIndex = columnMapping.keys.maxOrNull() ?: 0
        val endColumn = getExcelColumnName(maxColumnIndex)

        val tableAddress = "'${workSheetData.name}'!A1:${endColumn}1"

        Log.d(TAG, "Creating table with address: $tableAddress (${maxColumnIndex + 1} columns)")

        val createTableUrl = "${OneDriveConstants.BASE_MS_GRAPH_URL}/users('${metadataInfo.ownerId}')/drive/items('${metadataInfo.fileId}')/workbook/tables/add"

        val tableRequestBody = JSONObject().apply {
            put("address", tableAddress)
            put("hasHeaders", true)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(createTableUrl)
            .addHeader("Authorization", "Bearer ${getAuthToken()}")
            .addHeader("Content-Type", "application/json")
            .post(tableRequestBody)
            .build()

        tableId = withContext(Dispatchers.IO) {
            oneDriveClient.instance.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body.string()
                    Log.e(TAG, "Failed to create table: ${response.code} - ${response.message} - $errorBody")
                    throw Exception("Failed to create table: HTTP ${response.code}")
                }

                val responseBody = response.body.string()
                val json = JSONObject(responseBody)
                json.getString("id")
            }
        }

        Log.d(TAG, "Table created successfully with ID: $tableId")

        columnMapping.forEach { (index, columnName) ->
            renameColumn(index, columnName)
        }
    }

    /**
     * Converts a 0-based column index to Excel column name.
     * Examples: 0 -> A, 1 -> B, 25 -> Z, 26 -> AA, 27 -> AB
     */
    private fun getExcelColumnName(columnIndex: Int): String {
        var index = columnIndex
        val columnName = StringBuilder()

        while (index >= 0) {
            columnName.insert(0, ('A' + (index % 26)))
            index = (index / 26) - 1
        }

        return columnName.toString()
    }

    private suspend fun renameColumn(columnIndex: Int, newName: String) {
        Log.d(TAG, "Renaming column at index $columnIndex to '$newName'")

        val renameUrl = "${getTableUrl()}/columns/itemAt(index=$columnIndex)"

        val requestBody = JSONObject().apply {
            put("name", newName)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(renameUrl)
            .addHeader("Authorization", "Bearer ${getAuthToken()}")
            .addHeader("Content-Type", "application/json")
            .patch(requestBody)
            .build()

        withContext(Dispatchers.IO) {
            oneDriveClient.instance.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body.string()
                    Log.e(TAG, "Failed to rename column: ${response.code} - ${response.message} - $errorBody")
                    throw Exception("Failed to rename column at index $columnIndex: HTTP ${response.code}")
                }
            }
        }

        Log.d(TAG, "Column at index $columnIndex renamed to '$newName' successfully")
    }

    private suspend fun fetchTableColumns(): Map<Int, String> {
        val columnsUrl = "${getTableUrl()}/columns"

        val request = Request.Builder()
            .url(columnsUrl)
            .addHeader("Authorization", "Bearer ${getAuthToken()}")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            oneDriveClient.instance.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body.string()
                    Log.e(TAG, "Failed to fetch columns: ${response.code} - ${response.message} - $errorBody")
                    throw Exception("Failed to fetch columns: HTTP ${response.code}")
                }

                val responseBody = response.body.string()
                val json = JSONObject(responseBody)
                val columnsArray = json.getJSONArray("value")

                val columns = mutableMapOf<Int, String>()
                for (i in 0 until columnsArray.length()) {
                    val columnObj = columnsArray.getJSONObject(i)
                    val index = columnObj.getInt("index")
                    val name = columnObj.getString("name")
                    columns[index] = name
                }

                columns
            }
        }
    }

    private suspend fun createColumn(columnName: String, index: Int) {
        Log.d(TAG, "Creating column '$columnName' at index $index")

        val columnsUrl = "${getTableUrl()}/columns"

        val requestBody = JSONObject().apply {
            put("name", columnName)
            put("index", index)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(columnsUrl)
            .addHeader("Authorization", "Bearer ${getAuthToken()}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        withContext(Dispatchers.IO) {
            oneDriveClient.instance.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body.string()
                    Log.e(TAG, "Failed to create column: ${response.code} - ${response.message} - $errorBody")
                    throw Exception("Failed to create column '$columnName': HTTP ${response.code}")
                }
            }
        }

        Log.d(TAG, "Column '$columnName' created successfully")
    }

    private suspend fun fixDefaultColumnNamesIfNeeded() {
        val excelColumns = fetchTableColumns()
        val expectedColumns = OneDriveWorksheetRowHelpers.buildColumnMapping(clazz)

        val columnsToRename = mutableMapOf<Int, String>()

        expectedColumns.forEach { (index, expectedName) ->
            val currentName = excelColumns[index]
            val defaultName = "Column${index + 1}"

            if (currentName == defaultName && currentName != expectedName) {
                columnsToRename[index] = expectedName
            }
        }

        if (columnsToRename.isNotEmpty()) {
            Log.d(TAG, "Detected ${columnsToRename.size} columns with default names, renaming them")
            columnsToRename.forEach { (index, newName) ->
                try {
                    renameColumn(index, newName)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to rename column at index $index to '$newName'", e)
                }
            }
        }
    }

    private fun getTablesListUrl(): String {
        return "${OneDriveConstants.BASE_MS_GRAPH_URL}/users('${metadataInfo.ownerId}')/drive/items('${metadataInfo.fileId}')/workbook/worksheets('${workSheetData.id}')/tables"
    }

    private fun getTableUrl(): String {
        val id = tableId ?: throw IllegalStateException("Table ID not set. Table must be initialized first.")
        return "${getTablesListUrl()}/$id"
    }

    private suspend fun ensureInitialized() {
        if (!isInitialized) {
            initialize()
        }
    }

    private suspend fun findRowIndexByUniqueId(uniqueRowId: String): Int {
        val allRows = get()

        allRows.forEachIndexed { index, row ->
            if (row.getUniqueRowId() == uniqueRowId) {
                return index
            }
        }

        throw IllegalArgumentException("Row with ID '$uniqueRowId' not found in worksheet")
    }

    private suspend fun getAuthToken(): String {
        return authenticator.getAuthToken()
            ?: throw IllegalStateException("No authentication token available")
    }

    private fun getTableRowsUrl(): String {
        return "${getTableUrl()}/rows"
    }

    private suspend fun executeRequest(request: Request): String = withContext(Dispatchers.IO) {
        oneDriveClient.instance.newCall(request).execute().use { response ->
            parseResponse(response)
        }
    }

    private fun parseResponse(response: okhttp3.Response): String {
        val body = response.body.string()
        if (!response.isSuccessful) {
            Log.e(TAG, "Request failed: ${response.code} - ${response.message} - $body")
            throw Exception("Request failed: HTTP ${response.code}")
        }
        return body
    }

}
