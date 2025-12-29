//
//  ICreateFolder.swift
//  OpenCloudSheet
//
//  Interface for creating folders in cloud storage
//

import Foundation

protocol ICreateFolder {
    func createFolder(parentDirectory: ICloudFile?, folderName: String) async throws -> ICloudFile
}
