package com.opencloudsheet.model.file

/**
 * Generic interface representing a file or folder in cloud storage.
 * This abstraction works across different cloud providers (OneDrive, Google Drive, Dropbox, etc.)
 */
interface ICloudFile {
    fun getId(): String
    fun getName(): String
    fun isFolder(): Boolean
    fun getPath(): String?
    fun getSize(): Long?
}