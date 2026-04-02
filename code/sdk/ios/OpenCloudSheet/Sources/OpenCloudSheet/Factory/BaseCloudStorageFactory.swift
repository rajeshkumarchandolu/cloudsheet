//
//  BaseCloudStorageFactory.swift
//  OpenCloudSheet
//
//  Base factory protocol for cloud storage providers
//

import Foundation

protocol BaseCloudStorageFactory: Sendable {
    var appName: String { get }

    func initialize(from viewController: PlatformViewController) async throws
    func getAuthenticator() -> IAuthenticator
    func getCreateFolder() -> ICreateFolder
    func getCreateWorkBook() -> ICreateWorkBook
    func getDeleteWorkBook() -> IDeleteWorkBook
    func getListDirectoryContents() -> IListDirectoryContents
    func getMetadataManager() throws -> MetadataManager
    func getDataFolder() throws -> ICloudFile

    func createWorkBookInstance(
        workBookEntry: WorkBookEntry
    ) throws -> (any IWorkBook)?

    func createMetadataFileWorkbookEntry(
        file: ICloudFile
    ) async throws -> WorkBookEntry

    func getProviderMetadataInfo(
        workbookFile: ICloudFile
    ) async throws -> String
    func deleteWorkBook(workbookEntry: WorkBookEntry) async throws
    func deleteWorkBookFile(workbookEntry: WorkBookEntry) async throws
}
