@file:OptIn(ExperimentalWasmJsInterop::class)

package com.codebutler.farebot.web

import com.codebutler.farebot.shared.platform.PlatformActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
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

private fun jsSaveFile(
    content: JsString,
    fileName: JsString,
) {
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

private fun jsStartTextFilePicker(accept: JsString) {
    js(
        """
        (function() {
            window._fbResult = undefined;
            window._fbReady = false;
            var input = document.createElement('input');
            input.type = 'file';
            input.accept = accept;
            input.onchange = function() {
                var file = input.files[0];
                if (!file) { window._fbReady = true; return; }
                var reader = new FileReader();
                reader.onload = function() {
                    window._fbResult = reader.result;
                    window._fbReady = true;
                };
                reader.onerror = function() { window._fbReady = true; };
                reader.readAsText(file);
            };
            input.addEventListener('cancel', function() { window._fbReady = true; });
            window.addEventListener('focus', function onFocus() {
                setTimeout(function() { if (!window._fbReady) window._fbReady = true; }, 500);
                window.removeEventListener('focus', onFocus);
            });
            input.click();
        })()
        """,
    )
}

private fun jsStartBytesFilePicker() {
    js(
        """
        (function() {
            window._fbResult = undefined;
            window._fbReady = false;
            var input = document.createElement('input');
            input.type = 'file';
            input.onchange = function() {
                var file = input.files[0];
                if (!file) { window._fbReady = true; return; }
                var reader = new FileReader();
                reader.onload = function() {
                    window._fbResult = reader.result;
                    window._fbReady = true;
                };
                reader.onerror = function() { window._fbReady = true; };
                reader.readAsDataURL(file);
            };
            input.addEventListener('cancel', function() { window._fbReady = true; });
            window.addEventListener('focus', function onFocus() {
                setTimeout(function() { if (!window._fbReady) window._fbReady = true; }, 500);
                window.removeEventListener('focus', onFocus);
            });
            input.click();
        })()
        """,
    )
}

private fun jsIsFileResultReady(): Boolean = js("window._fbReady === true")

private fun jsGetFileResult(): JsString? = js("window._fbResult || null")

private fun jsResetFileResult() {
    js("window._fbResult = undefined; window._fbReady = false")
}

class WebPlatformActions : PlatformActions {
    private val scope = CoroutineScope(SupervisorJob())

    override fun openUrl(url: String) {
        jsOpenUrl(url.toJsString())
    }

    override fun openNfcSettings() {
        // No NFC settings on web
    }

    override fun copyToClipboard(text: String) {
        jsCopyToClipboard(text.toJsString())
    }

    override fun shareText(text: String) {
        copyToClipboard(text)
        showToast("Copied to clipboard")
    }

    override fun showToast(message: String) {
        jsAlert(message.toJsString())
    }

    override fun pickFileForImport(onResult: (String?) -> Unit) {
        jsStartTextFilePicker(".json,.txt".toJsString())
        scope.launch {
            val result = awaitFileResult()
            onResult(result)
        }
    }

    override fun saveFileForExport(
        content: String,
        defaultFileName: String,
    ) {
        jsSaveFile(content.toJsString(), defaultFileName.toJsString())
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun pickFileForBytes(onResult: (ByteArray?) -> Unit) {
        jsStartBytesFilePicker()
        scope.launch {
            val result = awaitFileResult()
            if (result == null) {
                onResult(null)
                return@launch
            }
            val base64Part = result.substringAfter(",", "")
            if (base64Part.isEmpty()) {
                onResult(null)
                return@launch
            }
            val bytes = Base64.decode(base64Part)
            onResult(bytes)
        }
    }

    private suspend fun awaitFileResult(): String? {
        // Poll every 50ms for up to 2 minutes
        repeat(2400) {
            if (jsIsFileResultReady()) {
                val result = jsGetFileResult()?.toString()
                jsResetFileResult()
                return result
            }
            delay(50)
        }
        jsResetFileResult()
        return null
    }
}
