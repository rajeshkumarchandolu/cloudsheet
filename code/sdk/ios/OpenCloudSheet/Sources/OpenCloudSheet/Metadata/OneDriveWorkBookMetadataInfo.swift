//
//  OneDriveWorkBookMetadataInfo.swift
//  OpenCloudSheet
//
//  OneDrive-specific metadata for accessing workbook files
//

import Foundation

struct OneDriveWorkBookMetadataInfo: Codable {
    let ownerId: String
    let fileId: String
}
