package com.opencloudsheet.factory

import android.app.Activity
import android.util.Log
import com.opencloudsheet.Provider
import com.opencloudsheet.auth.IAuthenticator
import com.opencloudsheet.auth.OneDriveAuthenticator
import com.opencloudsheet.storage.folder.create.ICreateFolder
import com.opencloudsheet.storage.folder.create.OneDrivePersonalFolderCreator
import com.opencloudsheet.storage.folder.list.IListDirectoryContents
import com.opencloudsheet.storage.folder.list.OneDriveListDirectory
import com.opencloudsheet.model.file.ICloudFile
import com.opencloudsheet.model.workbook.IWorkBook
import com.opencloudsheet.model.workbook.OneDriveWorkBook
import com.opencloudsheet.storage.workbook.create.ICreateWorkBook
import com.opencloudsheet.storage.workbook.create.OneDrivePersonalWorkbookCreator
import com.opencloudsheet.storage.workbook.delete.IDeleteWorkBook
import com.opencloudsheet.storage.workbook.delete.OneDrivePersonalWorkbookDeleter
import com.opencloudsheet.config.OneDriveConfiguration
import com.opencloudsheet.metadata.MetadataManager
import com.opencloudsheet.metadata.OneDriveWorkBookMetadataInfo
import com.opencloudsheet.model.worksheet.IWorksheetRow
import com.opencloudsheet.constants.OneDriveClient
import com.opencloudsheet.interceptor.OneDriveRequestInterceptor
import com.opencloudsheet.utilities.OneDriveResponseHelper
import com.opencloudsheet.utilities.OneDriveWorkbookSessionHelper

/**
 * Microsoft OneDrive implementation of cloud storage factory.
 *
 * Provides Microsoft OneDrive-specific implementations:
 * - OneDriveListDirectory - List directory contents
 * - OneDrivePersonalFolderCreator - Create folders
 * - OneDrivePersonalWorkbookCreator - Create Excel workbooks
 * - OneDriveWorkBook - Workbook operations
 *
 * Dependency Graph (DAG - Directed Acyclic Graph):
 * ```
 * Authenticator (lazy created)
 *     ↓
 * SessionHelper (lazy, depends on Authenticator)
 *     ↓
 * RequestInterceptor (lazy, depends on SessionHelper)
 *     ↓
 * OneDriveClient (lazy, depends on RequestInterceptor)
 *     ↓
 * ListDirectory, CreateFolder, etc. (lazy, depends on Authenticator + Client)
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

    private val _authenticator: IAuthenticator by lazy {
        OneDriveAuthenticator(config)
    }

    private val _sessionHelper: OneDriveWorkbookSessionHelper by lazy {
        OneDriveWorkbookSessionHelper(_authenticator)
    }

    private val _requestInterceptor: OneDriveRequestInterceptor by lazy {
        OneDriveRequestInterceptor(_sessionHelper)
    }

    private val _oneDriveClient: OneDriveClient by lazy {
        OneDriveClient(_requestInterceptor)
    }

    private val _listDirectory: IListDirectoryContents by lazy {
        OneDriveListDirectory(_authenticator, _oneDriveClient)
    }

    private val _createFolder: ICreateFolder by lazy {
        OneDrivePersonalFolderCreator(_authenticator, getListDirectory(), _oneDriveClient)
    }

    private val _createWorkBook: ICreateWorkBook by lazy {
        OneDrivePersonalWorkbookCreator(_authenticator, _oneDriveClient)
    }

    private val _deleteWorkBook: IDeleteWorkBook by lazy {
        OneDrivePersonalWorkbookDeleter(_authenticator, _oneDriveClient)
    }

    suspend fun initialize(activity: Activity) {
        if (isInitialized) {
            Log.d(TAG, "Already initialized, skipping")
            return
        }

        try {
            Log.d(TAG, "Starting Microsoft OneDrive factory initialization")
            _authenticator.login(activity)
            Log.d(TAG, "User logged in successfully. Token: ${_authenticator.getAuthToken()}")
            initializeMetadataManager()
            Log.d(TAG, "Microsoft OneDrive factory initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Microsoft OneDrive factory", e)
            throw e
        }
    }

    fun getListDirectory(): IListDirectoryContents = _listDirectory
    override fun getCreateFolder(): ICreateFolder = _createFolder

    override fun getCreateWorkBook(): ICreateWorkBook = _createWorkBook

    override fun getAuthenticator(): IAuthenticator = _authenticator

    override fun getListDirectoryContents(): IListDirectoryContents = _listDirectory

    override fun getDeleteWorkBook(): IDeleteWorkBook = _deleteWorkBook

    override fun <T : IWorksheetRow> createWorkBookInstance(
        clazz: Class<T>,
        workBookEntry: MetadataManager.WorkBookEntry
    ): IWorkBook<T> {
        return OneDriveWorkBook(
            oneDriveWorkBookMetadataInfo(workBookEntry.providerMetadataInfo),
            clazz,
            _authenticator,
            workBookEntry,
            _oneDriveClient
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
            provider = Provider.OneDrive.name,
            description = "This is for the Metadata Information of the Users WorkBooks",
            providerMetadataInfo = OneDriveResponseHelper.toString(providerMetadataInfo)
        )
    }

    override suspend fun getProviderMetadataInfo(
        workbookFile: ICloudFile,
        iosClassName: String?,
        androidClassName: String
    ): String {
        val userId: String? = _authenticator.getUserDetails()?.id()
        return OneDriveResponseHelper.toString(
            OneDriveWorkBookMetadataInfo(
                ownerId = userId!!,
                fileId = workbookFile.getId(),
                iosClassName = iosClassName,
                androidClassName = androidClassName
            )
        )
    }

    override suspend fun deleteWorkBookFile(workbookEntry: MetadataManager.WorkBookEntry) {
        val metadataInfo = OneDriveResponseHelper.fromJson(
            workbookEntry.providerMetadataInfo,
            OneDriveWorkBookMetadataInfo::class.java
        )
        _deleteWorkBook.deleteWorkBook(metadataInfo.ownerId, metadataInfo.fileId)
        Log.d(TAG, "Deleted OneDrive file: ${workbookEntry.name}")
    }

    private fun oneDriveWorkBookMetadataInfo(providerMetadataInfo: String): OneDriveWorkBookMetadataInfo {
        return OneDriveResponseHelper.fromJson(
            providerMetadataInfo,
            OneDriveWorkBookMetadataInfo::class.java
        )
    }

}
