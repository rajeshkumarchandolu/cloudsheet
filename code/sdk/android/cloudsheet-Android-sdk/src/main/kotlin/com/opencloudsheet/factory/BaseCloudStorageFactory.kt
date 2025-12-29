package com.opencloudsheet.factory

import android.util.Log
import com.opencloudsheet.auth.IAuthenticator
import com.opencloudsheet.storage.folder.create.ICreateFolder
import com.opencloudsheet.storage.folder.list.IListDirectoryContents
import com.opencloudsheet.model.file.ICloudFile
import com.opencloudsheet.model.workbook.IWorkBook
import com.opencloudsheet.storage.workbook.create.ICreateWorkBook
import com.opencloudsheet.storage.workbook.delete.IDeleteWorkBook
import com.opencloudsheet.metadata.MetadataManager
import com.opencloudsheet.model.worksheet.IWorksheetRow

/**
 * Base factory for cloud storage providers.
 *
 * Provides common initialization logic for all cloud storage providers
 * (Microsoft OneDrive, Google Drive, etc.).
 *
 * Uses Template Method pattern:
 * - Common logic: initialize() method creates folder structure and MetadataManager
 * - Provider-specific: Abstract methods implemented by subclasses
 *
 * @param appName Application name used for folder structure (cloudsheet/{appName}/data/)
 */
abstract class BaseCloudStorageFactory(
    protected val appName: String
) {
    companion object {
        private const val TAG = "BaseCloudStorageFactory"
    }

    protected var isInitialized: Boolean = false
    private var _metadataManager: MetadataManager? = null
    private var _dataFolder: ICloudFile? = null

    // Abstract methods - each provider must implement these
    abstract fun getCreateFolder(): ICreateFolder
    abstract fun getCreateWorkBook(): ICreateWorkBook
    abstract fun getDeleteWorkBook(): IDeleteWorkBook
    abstract fun <T : IWorksheetRow> createWorkBookInstance(
        clazz: Class<T>,
        workBookEntry: MetadataManager.WorkBookEntry
    ): IWorkBook<T>

    abstract fun getAuthenticator(): IAuthenticator
    abstract fun getListDirectoryContents(): IListDirectoryContents
    abstract suspend fun <T> createMetadataFileWorkbookEntry(
        file: ICloudFile,
        clazz: Class<T>
    ): MetadataManager.WorkBookEntry

    abstract suspend fun getProviderMetadataInfo(
        workbookFile: ICloudFile,
        iosClassName: String?,
        androidClassName: String
    ): String

    fun getMetadataManager(): MetadataManager {
        return _metadataManager ?: throw IllegalStateException(
            "Factory not initialized. Call initialize() first."
        )
    }

    fun getDataFolder(): ICloudFile {
        return _dataFolder ?: throw IllegalStateException(
            "Factory not initialized. Call initialize() first."
        )
    }

    suspend fun deleteWorkBook(workbookEntry: MetadataManager.WorkBookEntry) {
        deleteWorkBookFile(workbookEntry)
        _metadataManager?.deleteWorkBook(workbookEntry)
            ?: throw IllegalStateException("Factory not initialized")
    }

    protected suspend fun initializeMetadataManager() {
        if (!isInitialized) {
            Log.d(TAG, "Starting metadata manager initialization for app: $appName")
            val cloudsheetFolder = getOrCreateFolder(null, "cloudsheet")
            Log.d(TAG, "Got or created cloudsheet folder: ${cloudsheetFolder.getId()}")
            val appFolder = getOrCreateFolder(cloudsheetFolder, appName)
            Log.d(TAG, "Got or created app folder: ${appFolder.getId()}")
            _dataFolder = getOrCreateFolder(appFolder, "data")
            Log.d(TAG, "Got or created data folder: ${_dataFolder!!.getId()}")
            val metadataFile = getOrCreateWorkbook(_dataFolder, "Metadata")
            Log.d(TAG, "Got or created Metadata workbook: ${metadataFile.getId()}")
            @Suppress("UNCHECKED_CAST")
            val metadataWorkbook = createWorkBookInstance(
                MetadataManager.WorkBookEntry::class.java,
                createMetadataFileWorkbookEntry(
                    metadataFile,
                    MetadataManager.WorkBookEntry::class.java
                )
            )
            _metadataManager = MetadataManager(
                metadataWorkbook,
            ) { clazz, workBookEntry -> createWorkBookInstance(clazz, workBookEntry) }
            _metadataManager!!.initialize()
            isInitialized = true
            Log.d(TAG, "Metadata manager initialization completed successfully")
        } else {
            Log.d(TAG, "Already initialized, skipping")
        }
    }

    protected abstract suspend fun deleteWorkBookFile(workbookEntry: MetadataManager.WorkBookEntry)
    private suspend fun getOrCreateFolder(parent: ICloudFile?, name: String): ICloudFile {
        val contents = getListDirectoryContents().listDirectoryContents(parent)
        val existingFolder = contents.find { it.isFolder() && it.getName() == name }
        return existingFolder ?: getCreateFolder().createFolder(parent, name)
    }

    private suspend fun getOrCreateWorkbook(
        parentDirectory: ICloudFile?,
        name: String
    ): ICloudFile {
        val contents = getListDirectoryContents().listDirectoryContents(parentDirectory)
        val fileName = if (name.endsWith(".xlsx", ignoreCase = true)) name else "$name.xlsx"
        val existingFile = contents.find { !it.isFolder() && it.getName() == fileName }
        return existingFile ?: getCreateWorkBook().createWorkbook(parentDirectory, name)
    }

}
