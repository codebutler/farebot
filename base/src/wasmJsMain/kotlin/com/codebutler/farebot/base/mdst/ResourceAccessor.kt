@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalEncodingApi::class)

package com.codebutler.farebot.base.mdst

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Load a compose resource file synchronously via XMLHttpRequest.
 * Returns base64-encoded content, or null on failure.
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
            xhr.responseType = 'arraybuffer';
            xhr.send();
            if (xhr.status !== 200 && xhr.status !== 0) return null;
            var bytes = new Uint8Array(xhr.response);
            if (bytes.length === 0) return null;
            var binary = '';
            bytes.forEach(function(b) { binary += String.fromCharCode(b); });
            return btoa(binary);
        } catch(e) {
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
