//
//  IDeleteWorkBook.swift
//  OpenCloudSheet
//
//  Interface for deleting workbook files from cloud storage
//

import Foundation

protocol IDeleteWorkBook {
    func deleteWorkBook(ownerId: String, fileId: String) async throws
}
