package com.codebutler.farebot.shared.platform

/**
 * Platform-specific actions that screens can request.
 * Each platform provides its own implementation.
 */
interface PlatformActions {
    fun openUrl(url: String)

    val openNfcSettings: (() -> Unit)? get() = null

    fun copyToClipboard(text: String)

    fun shareFile(
        content: String,
        fileName: String,
        mimeType: String,
    )

    fun showToast(message: String)

    fun pickFileForImport(onResult: (String?) -> Unit)

    fun updateAppTimestamp() {}

    fun pickFileForBytes(onResult: (ByteArray?) -> Unit) {
        onResult(null)
    }
}
