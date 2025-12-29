//
//  ICreateWorkBook.swift
//  OpenCloudSheet
//
//  Interface for creating workbooks in cloud storage
//

import Foundation

protocol ICreateWorkBook {
    func createWorkbook(folder: ICloudFile?, name: String) async throws -> ICloudFile
}
