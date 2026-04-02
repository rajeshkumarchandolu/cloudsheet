//
//  OneDriveFactory.swift
//  OpenCloudSheet
//
//  OneDrive implementation of cloud storage factory
//

import Foundation

final class OneDriveFactory: BaseCloudStorageFactory, @unchecked Sendable {
    let appName: String
    private let config: OneDriveConfiguration

    private var isInitialized: Bool = false
    private var _metadataManager: MetadataManager?
    private var _dataFolder: ICloudFile?

    private lazy var _authenticator: IAuthenticator = {
        return OneDriveAuthenticator(config: config)
    }()

    private lazy var _sessionHelper: OneDriveWorkbookSessionHelper = {
        return OneDriveWorkbookSessionHelper(authenticator: _authenticator)
    }()

    private lazy var _oneDriveClient: OneDriveClient = {
        return OneDriveClient(sessionHelper: _sessionHelper)
    }()

    private lazy var _listDirectory: IListDirectoryContents = {
        return OneDriveListDirectory(authProvider: _authenticator, oneDriveClient: _oneDriveClient)
    }()

    private lazy var _createFolder: ICreateFolder = {
        return OneDrivePersonalFolderCreator(
            authProvider: _authenticator,
            listDirectoryContents: _listDirectory,
            oneDriveClient: _oneDriveClient
        )
    }()

    private lazy var _createWorkBook: ICreateWorkBook = {
        return OneDrivePersonalWorkbookCreator(authProvider: _authenticator, oneDriveClient: _oneDriveClient)
    }()

    private lazy var _deleteWorkBook: IDeleteWorkBook = {
        return OneDrivePersonalWorkbookDeleter(authenticator: _authenticator, oneDriveClient: _oneDriveClient)
    }()

    init(config: OneDriveConfiguration, appName: String) {
        self.config = config
        self.appName = appName
    }

    func initialize(from viewController: PlatformViewController) async throws {
        if isInitialized {
            print("Already initialized, skipping")
            return
        }

        do {
            print("Starting Microsoft OneDrive factory initialization")
            try await _authenticator.login(from: viewController)
            if let token = try await _authenticator.getAuthToken() {
                print("User logged in successfully. Token: \(token)")
            }
            try await initializeMetadataManager()
            print("Microsoft OneDrive factory initialized successfully")
        } catch {
            print("Failed to initialize Microsoft OneDrive factory: \(error)")
            throw error
        }
    }

    func getAuthenticator() -> IAuthenticator {
        return _authenticator
    }

    func getCreateFolder() -> ICreateFolder {
        return _createFolder
    }

    func getCreateWorkBook() -> ICreateWorkBook {
        return _createWorkBook
    }

    func getDeleteWorkBook() -> IDeleteWorkBook {
        return _deleteWorkBook
    }

    func getListDirectoryContents() -> IListDirectoryContents {
        return _listDirectory
    }

    func getMetadataManager() throws -> MetadataManager {
        guard let manager = _metadataManager else {
            throw NSError(domain: "OneDriveFactory", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Factory not initialized. Call initialize() first."])
        }
        return manager
    }

    func getDataFolder() throws -> ICloudFile {
        guard let folder = _dataFolder else {
            throw NSError(domain: "OneDriveFactory", code: -2,
                         userInfo: [NSLocalizedDescriptionKey: "Factory not initialized. Call initialize() first."])
        }
        return folder
    }

    func createWorkBookInstance(workBookEntry: WorkBookEntry) throws -> (any IWorkBook)? {
        let metadataInfo = oneDriveWorkBookMetadataInfo(providerMetadataInfo: workBookEntry.providerMetadataInfo)
        return OneDriveWorkBook(
            metadataInfo: metadataInfo,
            authenticator: _authenticator,
            workBookEntry: workBookEntry,
            oneDriveClient: _oneDriveClient
        )
    }

    func createMetadataFileWorkbookEntry(file: ICloudFile) async throws -> WorkBookEntry {
        let ownerId = try await _authenticator.getUserDetails()?.id() ?? "me"
        let providerMetadataInfo = OneDriveWorkBookMetadataInfo(
            ownerId: ownerId,
            fileId: file.getId()
        )

        let encoder = JSONEncoder()
        let data = try encoder.encode(providerMetadataInfo)
        let jsonString = String(data: data, encoding: .utf8) ?? "{}"

        return WorkBookEntry(
            name: file.getName(),
            provider: Provider.OneDrive.rawValue,
            description: "This is for the Metadata Information of the Users WorkBooks",
            providerMetadataInfo: jsonString
        )
    }

    func getProviderMetadataInfo(workbookFile: ICloudFile) async throws -> String {
        let userId = try await _authenticator.getUserDetails()?.id() ?? "me"
        let metadataInfo = OneDriveWorkBookMetadataInfo(
            ownerId: userId,
            fileId: workbookFile.getId()
        )

        let encoder = JSONEncoder()
        let data = try encoder.encode(metadataInfo)
        return String(data: data, encoding: .utf8) ?? "{}"
    }

    func deleteWorkBook(workbookEntry: WorkBookEntry) async throws {
        try await deleteWorkBookFile(workbookEntry: workbookEntry)
        guard let manager = _metadataManager else {
            throw NSError(domain: "OneDriveFactory", code: -3,
                         userInfo: [NSLocalizedDescriptionKey: "Factory not initialized"])
        }
        try await manager.deleteWorkBook(workbookEntry: workbookEntry)
    }

    func deleteWorkBookFile(workbookEntry: WorkBookEntry) async throws {
        let metadataInfo = oneDriveWorkBookMetadataInfo(providerMetadataInfo: workbookEntry.providerMetadataInfo)
        try await _deleteWorkBook.deleteWorkBook(ownerId: metadataInfo.ownerId, fileId: metadataInfo.fileId)
        print("Deleted OneDrive file: \(workbookEntry.name)")
    }

    private func initializeMetadataManager() async throws {
        if isInitialized {
            print("Already initialized, skipping")
            return
        }

        print("Starting metadata manager initialization for app: \(appName)")
        let cloudsheetFolder = try await getOrCreateFolder(parent: nil, name: "cloudsheet")
        print("Got or created cloudsheet folder: \(cloudsheetFolder.getId())")
        let appFolder = try await getOrCreateFolder(parent: cloudsheetFolder, name: appName)
        print("Got or created app folder: \(appFolder.getId())")
        _dataFolder = try await getOrCreateFolder(parent: appFolder, name: "data")
        print("Got or created data folder: \(_dataFolder!.getId())")
        let metadataFile = try await getOrCreateWorkbook(parentDirectory: _dataFolder, name: "Metadata")
        print("Got or created Metadata workbook: \(metadataFile.getId())")

        guard let metadataWorkbook = try createWorkBookInstance(
            workBookEntry: try await createMetadataFileWorkbookEntry(file: metadataFile)
        ) else {
            throw NSError(domain: "OneDriveFactory", code: -4,
                         userInfo: [NSLocalizedDescriptionKey: "Failed to create metadata workbook instance"])
        }
        try await metadataWorkbook.initialize()

        _metadataManager = MetadataManager(
            workbook: metadataWorkbook,
            createWorkBookInstance: { [weak self] entry in
                return try self?.createWorkBookInstance(workBookEntry: entry)
            }
        )

        try await _metadataManager!.initialize()
        isInitialized = true
        print("Metadata manager initialization completed successfully")
    }

    private func getOrCreateFolder(parent: ICloudFile?, name: String) async throws -> ICloudFile {
        let contents = try await _listDirectory.listDirectoryContents(directory: parent)
        if let existingFolder = contents.first(where: { $0.isFolder() && $0.getName() == name }) {
            return existingFolder
        }
        return try await _createFolder.createFolder(parentDirectory: parent, folderName: name)
    }

    private func getOrCreateWorkbook(parentDirectory: ICloudFile?, name: String) async throws -> ICloudFile {
        let contents = try await _listDirectory.listDirectoryContents(directory: parentDirectory)
        let fileName = name.hasSuffix(".xlsx") ? name : "\(name).xlsx"
        if let existingFile = contents.first(where: { !$0.isFolder() && $0.getName() == fileName }) {
            return existingFile
        }
        return try await _createWorkBook.createWorkbook(folder: parentDirectory, name: name)
    }

    private func oneDriveWorkBookMetadataInfo(providerMetadataInfo: String) -> OneDriveWorkBookMetadataInfo {
        guard let data = providerMetadataInfo.data(using: .utf8),
              let metadataInfo = try? JSONDecoder().decode(OneDriveWorkBookMetadataInfo.self, from: data) else {
            fatalError("Failed to parse OneDriveWorkBookMetadataInfo from JSON")
        }
        return metadataInfo
    }
}
