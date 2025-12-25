package com.opencloudsheet

import android.app.Activity
import android.util.Log
import com.opencloudsheet.model.workbook.IWorkBook
import com.opencloudsheet.config.OneDriveConfiguration
import com.opencloudsheet.factory.BaseCloudStorageFactory
import com.opencloudsheet.factory.OneDriveFactory
import com.opencloudsheet.metadata.MetadataManager.WorkBookEntry
import com.opencloudsheet.model.userdetails.IUserDetails
import com.opencloudsheet.model.worksheet.IWorksheetRow
import com.opencloudsheet.utilities.OneDriveResponseHelper
import java.util.concurrent.ConcurrentHashMap

/**
 * Main SDK entry point for CloudSheet.
 */
object OpenCloudSheetSdk {
    private const val TAG = "OpenCloudSheetSdk"

    private val factoryMap = ConcurrentHashMap<Provider, BaseCloudStorageFactory>()

    /**
     * Initialize OneDrive provider.
     *
     * @param context Android context
     * @param activity Activity for authentication UI
     * @param config OneDrive configuration (client ID, scopes, etc.)
     * @param appName Application name for folder structure (cloudsheet/{appName}/data/)
     */
    suspend fun initializeOneDriveProvider(
        activity: Activity,
        config: OneDriveConfiguration,
        appName: String
    ) {
        try {
            Log.d(TAG, "Initializing OneDrive provider")

            // 1. Create factory with config
            val factory = OneDriveFactory(config, appName)

            // 2. Initialize factory (handles auth + metadata manager setup)
            factory.initialize(activity)

            // 3. Store factory
            factoryMap[Provider.OneDrive] = factory

            Log.d(TAG, "OneDrive provider initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize OneDrive provider", e)
            throw e
        }
    }

    suspend fun getUserDetails(provider: Provider): IUserDetails? {
        val factory = factoryMap[provider]
            ?: throw IllegalStateException("Provider $provider not initialized")
        return factory.getAuthenticator().getUserDetails()
    }

    /**
     * Get list of all workbooks tracked in metadata.
     */
    suspend fun getWorkBooks(provider: Provider): List<IWorkBook<*>> {
        val factory = factoryMap[provider]
            ?: throw IllegalStateException("Provider $provider not initialized")

        return factory.getMetadataManager().listWorkBooks()
    }

    /**
     * Create a new workbook and track it in metadata.
     */
    suspend fun <T: IWorksheetRow> createWorkBook(
        provider: Provider,
        workbookName: String,
        description: String,
        clazz: Class<T>
    ): IWorkBook<T> {
        val factory = factoryMap[provider]
            ?: throw IllegalStateException("Provider $provider not initialized")

        val metadataManager = factory.getMetadataManager()
        val createWorkBook = factory.getCreateWorkBook()

        // Get data folder reference (cloudsheet/{appName}/data/)
        val dataFolder = factory.getDataFolder()

        // Create workbook file
        val workbookFile = createWorkBook.createWorkbook(dataFolder, workbookName)
        Log.d(TAG, "Created workbook file: ${workbookFile.getId()}")

        val providerMetadataInfo = factory.getProviderMetadatInfo(workbookFile);
        // Create WorkBookEntry
        val workBookEntry = WorkBookEntry(
            name = workbookName,
            className = clazz.name,
            provider = provider.name,
            description = description,
            providerMetadataInfo = providerMetadataInfo
        )

        // Create IWorkBook instance with metadata entry
        val workbook = factory.createWorkBookInstance(clazz, workBookEntry)

        // Add to metadata
        metadataManager.addWorkBook(
            name = workbookName,
            className = clazz.name,
            provider = provider,
            description = description,
            providerMetadataInfo = providerMetadataInfo
        )
        Log.d(TAG, "Added workbook to metadata: $workbookName")
        return workbook
    }

    /**
     * Delete a workbook from metadata tracking.
     * Note: This only removes the metadata entry, not the actual workbook file.
     */
    suspend fun deleteWorkBook(provider: Provider, workbookEntry: WorkBookEntry) {
        val factory = factoryMap[provider]
            ?: throw IllegalStateException("Provider $provider not initialized")

        factory.getMetadataManager().deleteWorkBook(workbookEntry)
        Log.d(TAG, "Deleted workbook from metadata: ${workbookEntry.name}")
    }

    /**
     * Get list of supported cloud storage providers.
     */
    fun supportedProviders(): List<Provider> = Provider.values().toList()

}
