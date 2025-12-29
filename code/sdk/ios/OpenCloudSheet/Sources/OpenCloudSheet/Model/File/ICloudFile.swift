//
//  ICloudFile.swift
//  OpenCloudSheet
//
//  Generic interface representing a file or folder in cloud storage
//

import Foundation

protocol ICloudFile {
    func getId() -> String
    func getName() -> String
    func isFolder() -> Bool
    func getPath() -> String?
    func getSize() -> Int64?
}
