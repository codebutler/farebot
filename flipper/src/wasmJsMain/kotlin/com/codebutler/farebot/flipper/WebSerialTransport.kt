@file:OptIn(ExperimentalWasmJsInterop::class)

package com.codebutler.farebot.flipper

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.js.ExperimentalWasmJsInterop

/**
 * FlipperTransport implementation using the Web Serial API.
 * Connects to Flipper Zero's CDC serial port via navigator.serial.
 *
 * Requires Chrome/Edge with Web Serial API support.
 * Must be initiated from a user gesture (button click).
 */
class WebSerialTransport : FlipperTransport {
    companion object {
        private const val POLL_INTERVAL_MS = 10L
        private const val READ_TIMEOUT_MS = 5000
        private const val CONNECT_TIMEOUT_MS = 15_000L
    }

    private var opened = false

    override val isConnected: Boolean
        get() = opened

    /**
     * Request a serial port from the user and open it.
     * Must be called from a user gesture context (button click).
     */
    override suspend fun connect() {
        if (!jsHasWebSerial()) {
            throw FlipperException("Web Serial API not available. Use Chrome or Edge.")
        }

        jsWebSerialRequestPort()

        withTimeout(CONNECT_TIMEOUT_MS) {
            while (!jsWebSerialIsReady()) {
                delay(POLL_INTERVAL_MS)
            }
        }

        if (!jsWebSerialHasPort()) {
            throw FlipperException("No serial port selected. Did you cancel the dialog?")
        }

        jsWebSerialOpen()

        withTimeout(CONNECT_TIMEOUT_MS) {
            while (!jsWebSerialIsOpen()) {
                val error = jsWebSerialGetOpenError()?.toString()
                if (error != null) {
                    throw FlipperException("Failed to open serial port: $error")
                }
                delay(POLL_INTERVAL_MS)
            }
        }

        opened = true
    }

    override suspend fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        jsWebSerialStartRead(length)

        var elapsed = 0L
        while (!jsWebSerialIsReadReady()) {
            delay(POLL_INTERVAL_MS)
            elapsed += POLL_INTERVAL_MS
            if (elapsed > READ_TIMEOUT_MS) {
                throw FlipperException("Serial read timed out")
            }
        }

        val csv = jsWebSerialGetReadData()?.toString() ?: throw FlipperException("Serial read returned no data")
        if (csv.isEmpty()) throw FlipperException("Serial read returned empty data")

        val bytes = csv.split(",").map { it.toInt().toByte() }.toByteArray()
        bytes.copyInto(buffer, offset, 0, bytes.size)
        return bytes.size
    }

    override suspend fun write(data: ByteArray) {
        val csv = data.joinToString(",") { (it.toInt() and 0xFF).toString() }
        jsWebSerialStartWrite(csv.toJsString())

        while (!jsWebSerialIsWriteReady()) {
            delay(POLL_INTERVAL_MS)
        }

        val error = jsWebSerialGetWriteError()?.toString()
        if (error != null) {
            throw FlipperException("Serial write failed: $error")
        }
    }

    override suspend fun close() {
        if (opened) {
            jsWebSerialClose()
            opened = false
        }
    }
}

// --- Web Serial JS interop ---

private fun jsHasWebSerial(): Boolean =
    js("typeof navigator !== 'undefined' && typeof navigator.serial !== 'undefined'")

private fun jsWebSerialRequestPort() {
    js(
        """
        (function() {
            window._fbSerial = { port: null, ready: false, open: false };
            navigator.serial.requestPort({
                filters: [{ usbVendorId: 0x0483, usbProductId: 0x5740 }]
            }).then(function(port) {
                window._fbSerial.port = port;
                window._fbSerial.ready = true;
            }).catch(function(err) {
                console.error('Web Serial requestPort failed:', err);
                window._fbSerial.ready = true;
            });
        })()
        """,
    )
}

private fun jsWebSerialIsReady(): Boolean = js("window._fbSerial && window._fbSerial.ready === true")

private fun jsWebSerialHasPort(): Boolean = js("window._fbSerial && window._fbSerial.port !== null")

private fun jsWebSerialOpen() {
    js(
        """
        (function() {
            window._fbSerial.openError = null;
            window._fbSerial.port.open({ baudRate: 230400 }).then(function() {
                window._fbSerial.reader = window._fbSerial.port.readable.getReader();
                window._fbSerial.open = true;
            }).catch(function(err) {
                console.error('Web Serial open failed:', err);
                window._fbSerial.openError = err.message || 'Unknown error';
            });
        })()
        """,
    )
}

private fun jsWebSerialIsOpen(): Boolean = js("window._fbSerial && window._fbSerial.open === true")

private fun jsWebSerialGetOpenError(): JsString? = js("(window._fbSerial && window._fbSerial.openError) || null")

private fun jsWebSerialStartRead(length: Int) {
    js(
        """
        (function() {
            window._fbSerialIn = { data: null, ready: false };
            window._fbSerial.reader.read().then(function(result) {
                if (result.value && result.value.length > 0) {
                    var arr = result.value;
                    var parts = [];
                    var len = Math.min(arr.length, length);
                    for (var i = 0; i < len; i++) parts.push(arr[i]);
                    window._fbSerialIn.data = parts.join(',');
                }
                window._fbSerialIn.ready = true;
            }).catch(function(err) {
                console.error('Web Serial read error:', err);
                window._fbSerialIn.ready = true;
            });
        })()
        """,
    )
}

private fun jsWebSerialIsReadReady(): Boolean = js("window._fbSerialIn && window._fbSerialIn.ready === true")

private fun jsWebSerialGetReadData(): JsString? = js("(window._fbSerialIn && window._fbSerialIn.data) || null")

private fun jsWebSerialStartWrite(dataStr: JsString) {
    js(
        """
        (function() {
            window._fbSerialOut = { ready: false, error: null };
            var parts = dataStr.split(',');
            var bytes = new Uint8Array(parts.length);
            for (var i = 0; i < parts.length; i++) bytes[i] = parseInt(parts[i]);
            var writer = window._fbSerial.port.writable.getWriter();
            writer.write(bytes).then(function() {
                writer.releaseLock();
                window._fbSerialOut.ready = true;
            }).catch(function(err) {
                writer.releaseLock();
                window._fbSerialOut.error = err.message;
                window._fbSerialOut.ready = true;
            });
        })()
        """,
    )
}

private fun jsWebSerialIsWriteReady(): Boolean = js("window._fbSerialOut && window._fbSerialOut.ready === true")

private fun jsWebSerialGetWriteError(): JsString? = js("(window._fbSerialOut && window._fbSerialOut.error) || null")

private fun jsWebSerialClose() {
    js(
        """
        (function() {
            try {
                if (window._fbSerial && window._fbSerial.reader) {
                    window._fbSerial.reader.cancel();
                    window._fbSerial.reader.releaseLock();
                }
                if (window._fbSerial && window._fbSerial.port) {
                    window._fbSerial.port.close();
                }
            } catch(e) {
                console.error('Web Serial close error:', e);
            }
            window._fbSerial = null;
            window._fbSerialIn = null;
            window._fbSerialOut = null;
        })()
        """,
    )
}
