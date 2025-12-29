//
//  OpenCloudSheetSdk.swift
//  Cloudsheet-ios-sdk
//

import Foundation

actor FactoryStorage {
    private var factoryMap: [Provider: BaseCloudStorageFactory] = [:]

    func set(provider: Provider, factory: BaseCloudStorageFactory) {
        factoryMap[provider] = factory
    }

    func get(provider: Provider) -> BaseCloudStorageFactory? {
        return factoryMap[provider]
    }
}

public class OpenCloudSheetSdk {
    private static let storage = FactoryStorage()

    public static func registerModels(_ types: [IWorksheetRow.Type]) async {
        await TypeRegistry.shared.register(types: types)
    }

    public static func initializeOneDriveProvider(
        from viewController: PlatformViewController,
        config: OneDriveConfiguration,
        appName: String
        ) async throws {
        let factory = OneDriveFactory(config: config, appName: appName)
        try await factory.initialize(from: viewController)
        await storage.set(provider: .OneDrive, factory: factory)
    }

    public static func getUserDetails(_ provider: Provider) async throws -> IUserDetails? {
        guard let factory = await storage.get(provider: provider) else {
            throw NSError(domain: "OpenCloudSheetSdk", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Provider \(provider) not initialized"])
        }
        return try await factory.getAuthenticator().getUserDetails()
    }

    public static func getWorkBooks(_ provider: Provider) async throws -> [any IWorkBook] {
        guard let factory = await storage.get(provider: provider) else {
            throw NSError(domain: "OpenCloudSheetSdk", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Provider \(provider) not initialized"])
        }
        return try await factory.getMetadataManager().listWorkBooks()
    }

    public static func createWorkBook<T: IWorksheetRow>(
        provider: Provider,
        workbookName: String,
        description: String,
        type: T.Type
    ) async throws -> (any IWorkBook)? {
        guard let factory = await storage.get(provider: provider) else {
            throw NSError(domain: "OpenCloudSheetSdk", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Provider \(provider) not initialized"])
        }

        let metadataManager = try factory.getMetadataManager()
        let createWorkBook = factory.getCreateWorkBook()
        let dataFolder = try factory.getDataFolder()

        let workbookFile = try await createWorkBook.createWorkbook(folder: dataFolder, name: workbookName)
        print("Created workbook file: \(workbookFile.getId())")

        let iosClassName = T.getIosClassName()
        let androidClassName = T.getAndroidClassName()
        let providerMetadataInfo = try await factory.getProviderMetadataInfo(
            workbookFile: workbookFile,
            iosClassName: iosClassName,
            androidClassName: androidClassName
        )

        let workBookEntry = WorkBookEntry(
            name: workbookName,
            provider: provider.rawValue,
            description: description,
            providerMetadataInfo: providerMetadataInfo
        )

        let workbook = factory.createWorkBookInstance(type: type, workBookEntry: workBookEntry)

        try await metadataManager.addWorkBook(
            name: workbookName,
            className: iosClassName,
            provider: provider,
            description: description,
            providerMetadataInfo: providerMetadataInfo
        )

        print("Added workbook to metadata: \(workbookName)")
        return workbook
    }

    public static func updateWorkBook(provider: Provider, workbookEntry: WorkBookEntry) async throws {
        guard let factory = await storage.get(provider: provider) else {
            throw NSError(domain: "OpenCloudSheetSdk", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Provider \(provider) not initialized"])
        }

        try await factory.getMetadataManager().updateWorkBook(workbookEntry: workbookEntry)
        print("Updated workbook metadata: \(workbookEntry.name)")
    }

    public static func deleteWorkBook(provider: Provider, workbookEntry: WorkBookEntry) async throws {
        guard let factory = await storage.get(provider: provider) else {
            throw NSError(domain: "OpenCloudSheetSdk", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Provider \(provider) not initialized"])
        }

        try await factory.deleteWorkBook(workbookEntry: workbookEntry)
        print("Deleted workbook: \(workbookEntry.name)")
    }

    public static func supportedProviders() -> [Provider] {
        return [.OneDrive]
    }
}
