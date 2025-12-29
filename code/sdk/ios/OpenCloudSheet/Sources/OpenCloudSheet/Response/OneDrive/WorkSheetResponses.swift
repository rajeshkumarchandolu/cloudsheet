//
//  WorkSheetResponses.swift
//  OpenCloudSheet
//

import Foundation

struct WorkSheetListResponse: Codable {
    let value: [WorkSheetData]
}

struct WorkSheetData: Codable {
    let id: String
    let name: String
    let position: Int?
}

struct WorkSheetResponse: Codable {
    let id: String
    let name: String
    let position: Int?
}

struct CreateWorkSheetRequest: Codable {
    let name: String
}
