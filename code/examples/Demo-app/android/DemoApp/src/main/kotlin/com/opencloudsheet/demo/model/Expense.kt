package com.opencloudsheet.demo.model

import com.opencloudsheet.model.worksheet.IWorksheetRow

/**
 * Expense model for tracking expenses in a workbook.
 *
 * Fields are mapped to worksheet columns in declaration order:
 * - Column 0 (rowId) - Auto-managed UUID
 * - Column 1 (createdAt) - Auto-managed timestamp
 * - Column 2 (updatedAt) - Auto-managed timestamp
 * - Column 3 (name) - Expense name/description
 * - Column 4 (amount) - Expense amount
 * - Column 5 (currency) - Currency code (e.g., USD, EUR, INR)
 */
data class Expense(
    var name: String = "",
    var amount: Double = 0.0,
    var currency: String = "USD"
) : IWorksheetRow() {
    override fun getIosClassName() = "CloudSheetDemo.Expense"
    override fun getTableColumnFieldsOrder() = listOf("name", "amount", "currency")
}
