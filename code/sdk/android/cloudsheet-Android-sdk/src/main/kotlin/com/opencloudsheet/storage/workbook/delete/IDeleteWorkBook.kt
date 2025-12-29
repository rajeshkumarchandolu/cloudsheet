package com.opencloudsheet.storage.workbook.delete

/**
 * Interface for deleting workbook files from cloud storage.
 */
interface IDeleteWorkBook {
    /**
     * Delete a workbook file from cloud storage.
     *
     * @param ownerId The owner ID of the file
     * @param fileId The file ID to delete
     */
    suspend fun deleteWorkBook(ownerId: String, fileId: String)
}
