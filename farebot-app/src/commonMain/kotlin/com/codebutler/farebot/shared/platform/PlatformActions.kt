package com.codebutler.farebot.shared.platform

/**
 * Platform-specific actions that screens can request.
 * Each platform provides its own implementation.
 */
interface PlatformActions {
    fun openUrl(url: String)
    fun openNfcSettings()
    fun copyToClipboard(text: String)
    fun getClipboardText(): String?
    fun shareText(text: String)
    fun showToast(message: String)
    fun pickFileForImport(onResult: (String?) -> Unit)
    fun saveFileForExport(content: String, defaultFileName: String)
    fun updateAppTimestamp() {}
    fun pickFileForBytes(onResult: (ByteArray?) -> Unit) { onResult(null) }
}
