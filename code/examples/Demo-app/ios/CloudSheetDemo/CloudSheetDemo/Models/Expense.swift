//
//  Expense.swift
//  CloudSheetDemo
//
//  Expense model for tracking expenses in a workbook
//

import Foundation
import OpenCloudSheet

struct Expense: IWorksheetRow {
    public var _rowId: String = ""
    public var _createdAt: Int64 = 0
    public var _updatedAt: Int64 = 0
    public var _rowIndex: Int = -1

    var name: String
    var amount: Double
    var currency: String

    static func getAndroidClassName() -> String {
        return "com.opencloudsheet.demo.model.Expense"
    }

    static func getTableColumnFieldsOrder() -> [String] {
        return ["name", "amount", "currency"]
    }

    init(name: String, amount: Double, currency: String) {
        self.name = name
        self.amount = amount
        self.currency = currency
    }
}
