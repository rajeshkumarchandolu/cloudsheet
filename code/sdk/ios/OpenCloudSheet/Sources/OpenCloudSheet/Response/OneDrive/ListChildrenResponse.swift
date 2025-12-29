//
//  ListChildrenResponse.swift
//  OpenCloudSheet
//

import Foundation

struct ListChildrenResponse: Codable {
    let value: [DriveItem]
    let odataNextLink: String?

    enum CodingKeys: String, CodingKey {
        case value
        case odataNextLink = "@odata.nextLink"
    }
}

struct DriveItem: Codable {
    let id: String
    let name: String
    let size: Int64?
    let folder: FolderFacet?
    let parentReference: ItemReference?
}

struct FolderFacet: Codable {
    let childCount: Int?
}

struct ItemReference: Codable {
    let path: String?
}
