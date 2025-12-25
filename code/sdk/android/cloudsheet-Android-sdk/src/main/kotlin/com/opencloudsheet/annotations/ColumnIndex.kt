package com.opencloudsheet.annotations

/**
 * Annotation to explicitly specify the column index for a field in a worksheet.
 *
 * By default, fields are mapped to columns based on their declaration order in the class.
 * The first 3 columns are automatically reserved for metadata (_rowId, _createdAt, _updatedAt)
 * and are managed by the SDK internally.
 *
 * Use this annotation to override the default mapping when you need custom column ordering.
 * You only need to specify indices for your data fields using 0-based indexing (0, 1, 2, ...).
 * The SDK will automatically add the metadata column offset.
 *
 * Example without @ColumnIndex (default mapping):
 * ```
 * data class Employee(
 *     val name: String,       // User index 0 → Maps to worksheet column 3 (D)
 *     val department: String  // User index 1 → Maps to worksheet column 4 (E)
 * ) : IWorksheetRow()
 * ```
 * Worksheet columns: [_rowId, _createdAt, _updatedAt, name, department]
 *
 * Example with @ColumnIndex (custom mapping):
 * ```
 * data class Employee(
 *     @ColumnIndex(1) val name: String,       // User index 1 → Maps to worksheet column 4 (E)
 *     @ColumnIndex(0) val department: String  // User index 0 → Maps to worksheet column 3 (D)
 * ) : IWorksheetRow()
 * ```
 * Worksheet columns: [_rowId, _createdAt, _updatedAt, department, name]
 *
 * @param value The 0-based user field index (0, 1, 2, ...). The SDK automatically adds the metadata column offset.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ColumnIndex(val value: Int)
