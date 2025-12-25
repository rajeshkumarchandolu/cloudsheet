package com.opencloudsheet.factory

import android.util.Log
import com.opencloudsheet.auth.IAuthenticator
import com.opencloudsheet.storageoperations.files.folder.create.ICreateFolder
import com.opencloudsheet.storageoperations.files.folder.list.IListDirectoryContents
import com.opencloudsheet.model.file.ICloudFile
import com.opencloudsheet.model.workbook.IWorkBook
import com.opencloudsheet.storageoperations.files.workbook.create.ICreateWorkBook
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

    abstract suspend fun getProviderMetadatInfo(workbookFile: ICloudFile): String
    protected suspend fun initializeMetadataManager() {
        if (!isInitialized) {
            Log.d(TAG, "Starting metadata manager initialization for app: $appName")

            // Step 1: Get or create cloudsheet/ folder
            val cloudsheetFolder = getOrCreateFolder(null, "cloudsheet")
            Log.d(TAG, "Got or created cloudsheet folder: ${cloudsheetFolder.getId()}")

            // Step 2: Get or create cloudsheet/{appName}/ folder
            val appFolder = getOrCreateFolder(cloudsheetFolder, appName)
            Log.d(TAG, "Got or created app folder: ${appFolder.getId()}")

            // Step 3: Get or create cloudsheet/{appName}/data/ folder
            _dataFolder = getOrCreateFolder(appFolder, "data")
            Log.d(TAG, "Got or created data folder: ${_dataFolder!!.getId()}")

            // Step 4: Get or create Metadata.xlsx workbook file
            val metadataFile = getOrCreateWorkbook(_dataFolder, "Metadata")
            Log.d(TAG, "Got or created Metadata workbook: ${metadataFile.getId()}")

            // Step 5: Create IWorkBook<WorkBookEntry> instance
            @Suppress("UNCHECKED_CAST")
            val metadataWorkbook = createWorkBookInstance(
                MetadataManager.WorkBookEntry::class.java,
                createMetadataFileWorkbookEntry(
                    metadataFile,
                    MetadataManager.WorkBookEntry::class.java
                )
            )

            // Step 6: Create and initialize MetadataManager
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
