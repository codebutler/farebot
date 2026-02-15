@file:OptIn(ExperimentalWasmJsInterop::class)

package com.codebutler.farebot.web

import com.codebutler.farebot.shared.platform.PlatformActions
import kotlin.js.ExperimentalWasmJsInterop

private fun jsOpenUrl(url: JsString) {
    js("window.open(url, '_blank')")
}

private fun jsCopyToClipboard(text: JsString) {
    js("navigator.clipboard.writeText(text)")
}

private fun jsAlert(message: JsString) {
    js("window.alert(message)")
}

private fun jsConsoleLog(message: JsString) {
    js("console.log(message)")
}

private fun jsSaveFile(content: JsString, fileName: JsString) {
    js(
        """
        (function() {
            var blob = new Blob([content], { type: 'application/json' });
            var a = document.createElement('a');
            a.href = URL.createObjectURL(blob);
            a.download = fileName;
            a.click();
            URL.revokeObjectURL(a.href);
        })()
        """,
    )
}

class WebPlatformActions : PlatformActions {
    override fun openUrl(url: String) {
        jsOpenUrl(url.toJsString())
    }

    override fun openNfcSettings() {
        // No NFC settings on web
    }

    override fun copyToClipboard(text: String) {
        jsCopyToClipboard(text.toJsString())
    }

    override fun getClipboardText(): String? {
        // Clipboard read requires async permission; return null for now
        return null
    }

    override fun shareText(text: String) {
        copyToClipboard(text)
        showToast("Copied to clipboard")
    }

    override fun showToast(message: String) {
        jsAlert(message.toJsString())
    }

    override fun pickFileForImport(onResult: (String?) -> Unit) {
        jsConsoleLog("pickFileForImport: not yet implemented for web".toJsString())
        onResult(null)
    }

    override fun saveFileForExport(
        content: String,
        defaultFileName: String,
    ) {
        jsSaveFile(content.toJsString(), defaultFileName.toJsString())
    }

    override fun pickFileForBytes(onResult: (ByteArray?) -> Unit) {
        jsConsoleLog("pickFileForBytes: not yet implemented for web".toJsString())
        onResult(null)
    }
}
