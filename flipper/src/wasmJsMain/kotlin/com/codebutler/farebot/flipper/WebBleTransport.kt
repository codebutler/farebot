@file:OptIn(ExperimentalWasmJsInterop::class)

package com.codebutler.farebot.flipper

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.js.ExperimentalWasmJsInterop

/**
 * FlipperTransport implementation using the Web Bluetooth API.
 * Connects to Flipper Zero's BLE Serial service.
 *
 * Requires Chrome/Edge with Web Bluetooth API support.
 * Must be initiated from a user gesture (button click).
 */
class WebBleTransport : FlipperTransport {
    companion object {
        private const val POLL_INTERVAL_MS = 10L
        private const val READ_TIMEOUT_MS = 5000
        private const val CONNECT_TIMEOUT_MS = 30_000L
    }

    private var connected = false

    override val isConnected: Boolean
        get() = connected

    override suspend fun connect() {
        if (!jsHasWebBluetooth()) {
            throw FlipperException("Web Bluetooth API not available. Use Chrome or Edge.")
        }

        jsWebBleRequestDevice()

        withTimeout(CONNECT_TIMEOUT_MS) {
            while (!jsWebBleIsReady()) {
                delay(POLL_INTERVAL_MS)
            }
        }

        if (!jsWebBleHasDevice()) {
            throw FlipperException("No Flipper Zero device selected. Did you cancel the dialog?")
        }

        jsWebBleConnect()

        withTimeout(CONNECT_TIMEOUT_MS) {
            while (!jsWebBleIsConnected()) {
                delay(POLL_INTERVAL_MS)
            }
        }

        val error = jsWebBleGetConnectError()?.toString()
        if (error != null) {
            throw FlipperException("BLE connection failed: $error")
        }

        connected = true
    }

    override suspend fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        var elapsed = 0L
        while (jsWebBleAvailable() == 0) {
            delay(POLL_INTERVAL_MS)
            elapsed += POLL_INTERVAL_MS
            if (elapsed > READ_TIMEOUT_MS) {
                throw FlipperException("BLE read timed out")
            }
        }

        jsWebBleStartRead(length)
        val csv =
            jsWebBleGetReadResult()?.toString()
                ?: throw FlipperException("BLE read returned no data")
        if (csv.isEmpty()) throw FlipperException("BLE read returned empty data")

        val bytes = csv.split(",").map { it.toInt().toByte() }.toByteArray()
        bytes.copyInto(buffer, offset, 0, bytes.size)
        return bytes.size
    }

    override suspend fun write(data: ByteArray) {
        val csv = data.joinToString(",") { (it.toInt() and 0xFF).toString() }
        jsWebBleStartWrite(csv.toJsString())

        while (!jsWebBleIsWriteReady()) {
            delay(POLL_INTERVAL_MS)
        }

        val error = jsWebBleGetWriteError()?.toString()
        if (error != null) {
            throw FlipperException("BLE write failed: $error")
        }
    }

    override suspend fun close() {
        if (connected) {
            jsWebBleDisconnect()
            connected = false
        }
    }
}

// --- Web Bluetooth JS interop ---

private fun jsHasWebBluetooth(): Boolean =
    js("typeof navigator !== 'undefined' && typeof navigator.bluetooth !== 'undefined'")

private fun jsWebBleRequestDevice() {
    js(
        """
        (function() {
            console.log('[FareBot BLE] requestDevice: starting with namePrefix filter "Flipper"');
            console.log('[FareBot BLE] navigator.bluetooth available:', !!navigator.bluetooth);
            window._fbBle = { device: null, server: null, rxChar: null, txChar: null, ready: false, connected: false, connectError: null, buffer: [], writeReady: false, writeError: null };
            navigator.bluetooth.requestDevice({
                filters: [{ namePrefix: 'Flipper' }],
                optionalServices: ['8fe5b3d5-2e7f-4a98-2a48-7acc60fe0000']
            }).then(function(device) {
                console.log('[FareBot BLE] requestDevice: got device:', device.name, 'id:', device.id);
                window._fbBle.device = device;
                window._fbBle.ready = true;
            }).catch(function(err) {
                console.error('[FareBot BLE] requestDevice failed:', err.name, err.message);
                window._fbBle.ready = true;
            });
        })()
        """,
    )
}

private fun jsWebBleIsReady(): Boolean = js("window._fbBle && window._fbBle.ready === true")

private fun jsWebBleHasDevice(): Boolean = js("window._fbBle && window._fbBle.device !== null")

private fun jsWebBleConnect() {
    js(
        """
        (function() {
            var ble = window._fbBle;
            console.log('[FareBot BLE] connect: connecting to GATT server on device:', ble.device.name);
            ble.device.gatt.connect().then(function(server) {
                console.log('[FareBot BLE] connect: GATT server connected, getting primary service...');
                ble.server = server;
                return server.getPrimaryService('8fe5b3d5-2e7f-4a98-2a48-7acc60fe0000');
            }).then(function(service) {
                console.log('[FareBot BLE] connect: got service, getting characteristics...');
                return Promise.all([
                    service.getCharacteristic('19ed82ae-ed21-4c9d-4145-228e62fe0000'),
                    service.getCharacteristic('19ed82ae-ed21-4c9d-4145-228e61fe0000')
                ]);
            }).then(function(chars) {
                console.log('[FareBot BLE] connect: got characteristics, starting notifications...');
                ble.rxChar = chars[0];
                ble.txChar = chars[1];
                return ble.txChar.startNotifications();
            }).then(function() {
                console.log('[FareBot BLE] connect: notifications started, connection ready');
                ble.txChar.addEventListener('characteristicvaluechanged', function(event) {
                    var value = event.target.value;
                    var arr = new Uint8Array(value.buffer);
                    for (var i = 0; i < arr.length; i++) {
                        ble.buffer.push(arr[i]);
                    }
                });
                ble.connected = true;
            }).catch(function(err) {
                console.error('[FareBot BLE] connect failed:', err.name, err.message);
                ble.connectError = err.message || 'Unknown error';
                ble.connected = true;
            });
        })()
        """,
    )
}

private fun jsWebBleIsConnected(): Boolean = js("window._fbBle && window._fbBle.connected === true")

private fun jsWebBleGetConnectError(): JsString? = js("(window._fbBle && window._fbBle.connectError) || null")

private fun jsWebBleAvailable(): Int = js("(window._fbBle && window._fbBle.buffer) ? window._fbBle.buffer.length : 0")

private fun jsWebBleStartRead(length: Int) {
    js(
        """
        (function() {
            var buf = window._fbBle.buffer;
            var toRead = Math.min(buf.length, length);
            var parts = [];
            for (var i = 0; i < toRead; i++) parts.push(buf.shift());
            window._fbBleReadResult = parts.join(',');
        })()
        """,
    )
}

private fun jsWebBleGetReadResult(): JsString? = js("window._fbBleReadResult || null")

private fun jsWebBleStartWrite(dataStr: JsString) {
    js(
        """
        (function() {
            window._fbBle.writeReady = false;
            window._fbBle.writeError = null;
            var parts = dataStr.split(',');
            var bytes = new Uint8Array(parts.length);
            for (var i = 0; i < parts.length; i++) bytes[i] = parseInt(parts[i]);
            window._fbBle.rxChar.writeValue(bytes).then(function() {
                window._fbBle.writeReady = true;
            }).catch(function(err) {
                window._fbBle.writeError = err.message;
                window._fbBle.writeReady = true;
            });
        })()
        """,
    )
}

private fun jsWebBleIsWriteReady(): Boolean = js("window._fbBle && window._fbBle.writeReady === true")

private fun jsWebBleGetWriteError(): JsString? = js("(window._fbBle && window._fbBle.writeError) || null")

private fun jsWebBleDisconnect() {
    js(
        """
        (function() {
            try {
                if (window._fbBle && window._fbBle.server) {
                    window._fbBle.server.disconnect();
                }
            } catch(e) {
                console.error('Web Bluetooth disconnect error:', e);
            }
            window._fbBle = null;
        })()
        """,
    )
}
