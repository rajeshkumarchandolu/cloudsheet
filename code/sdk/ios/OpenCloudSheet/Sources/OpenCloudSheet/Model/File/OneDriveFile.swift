//
//  OneDriveFile.swift
//  OpenCloudSheet
//

import Foundation

class OneDriveFile: ICloudFile {
    private let item: DriveItem

    init(item: DriveItem) {
        self.item = item
    }

    func getId() -> String {
        return item.id
    }

    func getName() -> String {
        return item.name
    }

    func isFolder() -> Bool {
        return item.folder != nil
    }

    func getPath() -> String? {
        return item.parentReference?.path
    }

    func getSize() -> Int64? {
        if let size = item.size, size >= 0 {
            return size
        }
        return nil
    }
}
