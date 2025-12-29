//
//  IListDirectoryContents.swift
//  OpenCloudSheet
//
//  Interface for listing directory contents in cloud storage
//

import Foundation

protocol IListDirectoryContents {
    func listDirectoryContents(directory: ICloudFile?) async throws -> [ICloudFile]
}
