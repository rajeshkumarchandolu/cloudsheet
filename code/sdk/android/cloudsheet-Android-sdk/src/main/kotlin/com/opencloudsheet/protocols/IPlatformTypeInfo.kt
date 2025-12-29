package com.opencloudsheet.protocols

/**
 * Interface for providing platform-specific type information to enable cross-platform workbook compatibility.
 *
 * Implementing classes should provide both iOS and Android class names to ensure workbooks
 * can be accessed from both platforms.
 */
interface IPlatformTypeInfo {
    fun getIosClassName(): String
    fun getAndroidClassName(): String = this::class.java.name
}
