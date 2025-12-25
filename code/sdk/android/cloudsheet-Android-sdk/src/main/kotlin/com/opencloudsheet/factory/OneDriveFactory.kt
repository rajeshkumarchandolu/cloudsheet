package com.opencloudsheet.factory

import android.app.Activity
import android.util.Log
import com.opencloudsheet.Provider
import com.opencloudsheet.auth.IAuthenticator
import com.opencloudsheet.auth.OneDriveAuthenticator
import com.opencloudsheet.storageoperations.files.folder.create.ICreateFolder
import com.opencloudsheet.storageoperations.files.folder.create.OneDriveCreateFolder
import com.opencloudsheet.storageoperations.files.folder.list.IListDirectoryContents
import com.opencloudsheet.storageoperations.files.folder.list.OneDriveListDirectory
import com.opencloudsheet.model.file.ICloudFile
import com.opencloudsheet.model.workbook.IWorkBook
import com.opencloudsheet.model.workbook.OneDriveWorkBook
import com.opencloudsheet.storageoperations.files.workbook.create.ICreateWorkBook
import com.opencloudsheet.storageoperations.files.workbook.create.OneDriveCreateWorkBook
import com.opencloudsheet.config.OneDriveConfiguration
import com.opencloudsheet.metadata.MetadataManager
import com.opencloudsheet.metadata.OneDriveWorkBookMetadataInfo
import com.opencloudsheet.model.worksheet.IWorksheetRow
import com.opencloudsheet.utilities.OneDriveResponseHelper

/**
 * Microsoft OneDrive implementation of cloud storage factory.
 *
 * Provides Microsoft OneDrive-specific implementations:
 * - OneDriveListDirectory - List directory contents
 * - OneDriveCreateFolder - Create folders
 * - OneDriveCreateWorkBook - Create Excel workbooks
 * - OneDriveWorkBook - Workbook operations
 *
 * Dependency Graph (DAG - Directed Acyclic Graph):
 * ```
 * Authenticator (lazy created)
 *     ↓
 * ListDirectory (lazy, depends on Authenticator)
 *     ↓
 * CreateFolder (lazy, depends on ListDirectory)
 *     ↓
 * BaseCloudStorageFactory.initializeMetadataManager() (uses CreateFolder + CreateWorkBook)
 * ```
 */
internal class OneDriveFactory(
    private val config: OneDriveConfiguration,
    appName: String
) : BaseCloudStorageFactory(appName) {

    companion object {
        private const val TAG = "OneDriveFactory"
    }

    // Lazy-initialized authenticator
    private val _authenticator: IAuthenticator by lazy {
        OneDriveAuthenticator(config)
    }

    // Lazy-initialized commands (depend on authenticator)
    private val _listDirectory: IListDirectoryContents by lazy {
        OneDriveListDirectory(_authenticator)
    }

    private val _createFolder: ICreateFolder by lazy {
        OneDriveCreateFolder(_authenticator, getListDirectory())
    }

    private val _createWorkBook: ICreateWorkBook by lazy {
        OneDriveCreateWorkBook(_authenticator)
    }

    /**
     * Initialize the factory with authentication and metadata manager setup.
     *
     * @param activity Activity for authentication UI
     */
    suspend fun initialize(activity: Activity) {
        if (isInitialized) {
            Log.d(TAG, "Already initialized, skipping")
            return
        }

        try {
            Log.d(TAG, "Starting Microsoft OneDrive factory initialization")

            // Step 1: Login user (authenticator will be lazily created and initialized in login())
            _authenticator.login(activity)
            Log.d(TAG, "User logged in successfully. Token: ${_authenticator.getAuthToken()}")

            // Step 2: Initialize metadata manager (creates folder structure + Metadata.xlsx)
            initializeMetadataManager()

            Log.d(TAG, "Microsoft OneDrive factory initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Microsoft OneDrive factory", e)
            throw e
        }
    }

    // Public accessor for ListDirectory (used by other components)
    fun getListDirectory(): IListDirectoryContents = _listDirectory

    // Override abstract methods from BaseCloudStorageFactory
    override fun getCreateFolder(): ICreateFolder = _createFolder

    override fun getCreateWorkBook(): ICreateWorkBook = _createWorkBook

    override fun getAuthenticator(): IAuthenticator = _authenticator

    override fun getListDirectoryContents(): IListDirectoryContents = _listDirectory

    override fun <T : IWorksheetRow> createWorkBookInstance(
        clazz: Class<T>,
        workBookEntry: MetadataManager.WorkBookEntry
    ): IWorkBook<T> {
        return OneDriveWorkBook(
            oneDriveWorkBookMetadataInfo(workBookEntry.providerMetadataInfo),
            clazz,
            _authenticator,
            workBookEntry
        )
    }

    override suspend fun <T> createMetadataFileWorkbookEntry(
        file: ICloudFile,
        clazz: Class<T>
    ): MetadataManager.WorkBookEntry {
        val ownerId = _authenticator.getUserDetails()?.id()
        val providerMetadataInfo = OneDriveWorkBookMetadataInfo(
            ownerId = ownerId ?: "me",
            fileId = file.getId()
        )
        return MetadataManager.WorkBookEntry(
            name = file.getName(),
            className = IWorksheetRow::class.java.name,
            provider = Provider.OneDrive.name,
            description = "This is for the Metadata Information of the Users WorkBooks",
            providerMetadataInfo = OneDriveResponseHelper.toString(providerMetadataInfo)
        )
    }

    override suspend fun getProviderMetadatInfo(workbookFile: ICloudFile): String {
        val userId: String? = _authenticator.getUserDetails()?.id()
        return OneDriveResponseHelper.toString(
            OneDriveWorkBookMetadataInfo(
                userId!!, workbookFile.getId()
            )
        )
    }

    private fun oneDriveWorkBookMetadataInfo(providerMetadataInfo: String): OneDriveWorkBookMetadataInfo {
        return OneDriveResponseHelper.fromJson(
            providerMetadataInfo,
            OneDriveWorkBookMetadataInfo::class.java
        )
    }

}
