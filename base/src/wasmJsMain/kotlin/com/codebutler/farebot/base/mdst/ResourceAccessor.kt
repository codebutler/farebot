@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalEncodingApi::class)

package com.codebutler.farebot.base.mdst

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Load a compose resource file synchronously via XMLHttpRequest.
 * Returns base64-encoded content, or null on failure.
 *
 * Uses overrideMimeType('text/plain; charset=x-user-defined') because browsers
 * throw InvalidAccessError when setting responseType on synchronous XHR.
 * The x-user-defined charset maps each byte to a Unicode code point, which
 * we convert back to bytes via charCodeAt(i) & 0xFF.
 *
 * Compose Resources for wasmJs serves files at:
 *   composeResources/{package}/files/{filename}
 */
private fun jsLoadResourceBase64(url: JsString): JsString? =
    js(
        """
    (function() {
        try {
            var xhr = new XMLHttpRequest();
            xhr.open('GET', url, false);
            xhr.overrideMimeType('text/plain; charset=x-user-defined');
            xhr.send();
            if (xhr.status !== 200 && xhr.status !== 0) return null;
            var text = xhr.responseText;
            if (text.length === 0) return null;
            var binary = '';
            for (var i = 0; i < text.length; i++) {
                binary += String.fromCharCode(text.charCodeAt(i) & 0xFF);
            }
            return btoa(binary);
        } catch(e) {
            console.error('Failed to load resource: ' + url, e);
            return null;
        }
    })()
    """,
    )

actual object ResourceAccessor {
    actual fun openMdstFile(dbName: String): ByteArray? {
        val base64 =
            jsLoadResourceBase64(
                "composeResources/farebot.base.generated.resources/files/$dbName.mdst".toJsString(),
            )?.toString() ?: return null
        if (base64.isEmpty()) return null
        return try {
            Base64.decode(base64)
        } catch (_: Exception) {
            null
        }
    }
}
