/*
 * WebUsbPN533Transport.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.codebutler.farebot.card.nfc.pn533

import com.codebutler.farebot.base.util.hex
import kotlinx.coroutines.delay
import kotlin.js.ExperimentalWasmJsInterop

/**
 * WebUSB transport layer for PN533 NFC reader communication.
 *
 * Implements the PN533 normal information frame format using the
 * WebUSB API (navigator.usb) via Kotlin/WasmJs interop.
 *
 * Note: This transport uses suspend functions instead of the synchronous
 * [PN533Transport] interface because WebUSB is Promise-based and
 * Kotlin/WasmJs cannot block on Promises.
 *
 * Frame format:
 * - Normal:   [00 00 FF] [LEN] [LCS] [TFI CMD DATA...] [DCS] [00]
 * - Extended: [00 00 FF FF FF] [LEN_H LEN_L] [LCS] [TFI CMD DATA...] [DCS] [00]
 *
 * Reference: NXP PN533 User Manual, libnfc pn53x_usb.c
 */
class WebUsbPN533Transport : PN533Transport {
    private var deviceOpened = false

    override suspend fun sendCommand(
        code: Byte,
        data: ByteArray,
        timeoutMs: Int,
    ): ByteArray = sendCommandAsync(code, data, timeoutMs)

    override suspend fun sendAck() = sendAckAsync()

    override fun flush() {
        // No-op: flush is handled during openAsync()
    }

    override fun close() {
        if (deviceOpened) {
            jsWebUsbClose()
            deviceOpened = false
        }
    }

    /**
     * Request a WebUSB device from the user and open it.
     * Must be called from a user gesture context (e.g., button click handler).
     */
    suspend fun openAsync(): Boolean {
        if (!jsHasWebUsb()) return false
        jsWebUsbRequestDevice()
        // Poll until the device request completes
        while (!jsWebUsbIsReady()) {
            delay(POLL_INTERVAL_MS)
        }
        if (!jsWebUsbHasDevice()) {
            return false
        }
        deviceOpened = true
        // No flush here — WebUSB transferIn cannot be cancelled, so rapid
        // reads with short timeouts leave dangling promises that consume
        // subsequent device responses. The poll loop sends an ACK first
        // to clear any stale PN533 command state.
        return true
    }

    suspend fun sendCommandAsync(
        code: Byte,
        data: ByteArray = byteArrayOf(),
        timeoutMs: Int = TIMEOUT_MS,
    ): ByteArray {
        val payload = byteArrayOf(TFI_HOST_TO_PN533, code) + data
        val frame = buildFrame(payload)

        bulkWrite(frame)

        // Read ACK or response
        val ackOrResponse =
            bulkRead(TIMEOUT_MS)
                ?: throw PN533TransportException("USB read ACK timed out")

        if (ackOrResponse.size >= ACK_FRAME.size &&
            ackOrResponse.copyOfRange(0, ACK_FRAME.size).contentEquals(ACK_FRAME)
        ) {
            // ACK received, now read the actual response
            val response =
                bulkRead(timeoutMs)
                    ?: throw PN533TransportException("USB read response timed out")
            return parseFrame(response)
        }

        // Not an ACK — likely a response frame (stale data or ACK was skipped)
        if (ackOrResponse.size >= 6 &&
            ackOrResponse[0] == 0x00.toByte() &&
            ackOrResponse[1] == 0x00.toByte() &&
            ackOrResponse[2] == 0xFF.toByte()
        ) {
            return parseFrame(ackOrResponse)
        }

        throw PN533Exception("Expected ACK, got: ${ackOrResponse.hex()}")
    }

    suspend fun sendAckAsync() {
        bulkWrite(ACK_FRAME)
    }

    private suspend fun bulkWrite(data: ByteArray) {
        val csv = data.joinToString(",") { (it.toInt() and 0xFF).toString() }
        jsWebUsbStartTransferOut(csv.toJsString())
        while (!jsWebUsbIsXferOutReady()) {
            delay(POLL_INTERVAL_MS)
        }
        val error = jsWebUsbGetXferOutError()?.toString()
        if (error != null) {
            throw PN533TransportException("USB write failed: $error")
        }
    }

    private suspend fun bulkRead(timeoutMs: Int): ByteArray? {
        jsWebUsbStartTransferIn(timeoutMs)
        while (!jsWebUsbIsXferInReady()) {
            delay(POLL_INTERVAL_MS)
        }
        val error = jsWebUsbGetXferInError()?.toString()
        if (error != null) {
            throw PN533TransportException("USB read failed: $error")
        }
        val csv = jsWebUsbGetXferInData()?.toString() ?: return null
        if (csv.isEmpty()) return null
        return csv.split(",").map { it.toInt().toByte() }.toByteArray()
    }

    companion object {
        const val TIMEOUT_MS = 5000
        const val POLL_INTERVAL_MS = 5L

        const val TFI_HOST_TO_PN533: Byte = 0xD4.toByte()
        val TFI_PN533_TO_HOST: Byte = 0xD5.toByte()

        val ACK_FRAME =
            byteArrayOf(
                0x00,
                0x00,
                0xFF.toByte(),
                0x00,
                0xFF.toByte(),
                0x00,
            )

        internal fun buildFrame(payload: ByteArray): ByteArray {
            val len = payload.size
            if (len <= 254) {
                val lcs = (256 - len) and 0xFF
                val dcs = (256 - payload.sumOf { it.toInt() and 0xFF }) and 0xFF
                return byteArrayOf(
                    0x00,
                    0x00,
                    0xFF.toByte(),
                    len.toByte(),
                    lcs.toByte(),
                ) + payload + byteArrayOf(dcs.toByte(), 0x00)
            } else {
                val lenH = (len shr 8) and 0xFF
                val lenL = len and 0xFF
                val lcs = (256 - ((lenH + lenL) and 0xFF)) and 0xFF
                val dcs = (256 - payload.sumOf { it.toInt() and 0xFF }) and 0xFF
                return byteArrayOf(
                    0x00,
                    0x00,
                    0xFF.toByte(),
                    0xFF.toByte(),
                    0xFF.toByte(),
                    lenH.toByte(),
                    lenL.toByte(),
                    lcs.toByte(),
                ) + payload + byteArrayOf(dcs.toByte(), 0x00)
            }
        }

        internal fun parseFrame(frame: ByteArray): ByteArray {
            if (frame.size < 6) {
                throw PN533Exception("Frame too short: ${frame.size} bytes")
            }
            if (frame[0] != 0x00.toByte() || frame[1] != 0x00.toByte() || frame[2] != 0xFF.toByte()) {
                throw PN533Exception("Invalid frame preamble: ${frame.hex()}")
            }

            val payload: ByteArray
            if (frame[3] == 0xFF.toByte() && frame[4] == 0xFF.toByte()) {
                if (frame.size < 10) {
                    throw PN533Exception("Extended frame too short: ${frame.size}")
                }
                val lenH = frame[5].toInt() and 0xFF
                val lenL = frame[6].toInt() and 0xFF
                val len = (lenH shl 8) or lenL
                payload = frame.copyOfRange(8, 8 + len)
            } else {
                val len = frame[3].toInt() and 0xFF
                payload = frame.copyOfRange(5, 5 + len)
            }

            if (payload.size == 1) {
                throw PN533CommandException(payload[0].toInt() and 0xFF)
            }

            if (payload.size < 2 || payload[0] != TFI_PN533_TO_HOST) {
                throw PN533Exception("Invalid TFI in response: ${payload.hex()}")
            }

            return payload.copyOfRange(2, payload.size)
        }
    }
}

// --- WebUSB JS interop ---
// All async WebUSB operations use window globals + coroutine polling.

private fun jsHasWebUsb(): Boolean = js("typeof navigator !== 'undefined' && typeof navigator.usb !== 'undefined'")

private fun jsWebUsbRequestDevice() {
    js(
        """
        (function() {
            window._fbUsb = { device: null, ready: false, error: null };
            navigator.usb.requestDevice({
                filters: [
                    { vendorId: 0x04CC, productId: 0x2533 },
                    { vendorId: 0x04E6, productId: 0x5591 },
                    { vendorId: 0x054C, productId: 0x02E1 }
                ]
            }).then(function(device) {
                return device.open().then(function() { return device; });
            }).then(function(device) {
                return device.selectConfiguration(1).then(function() { return device; });
            }).then(function(device) {
                return device.claimInterface(0).then(function() { return device; });
            }).then(function(device) {
                window._fbUsb.device = device;
                window._fbUsb.ready = true;
            }).catch(function(err) {
                window._fbUsb.error = err.message || 'Unknown error';
                window._fbUsb.ready = true;
            });
        })()
        """,
    )
}

private fun jsWebUsbIsReady(): Boolean = js("window._fbUsb && window._fbUsb.ready === true")

private fun jsWebUsbHasDevice(): Boolean = js("window._fbUsb && window._fbUsb.device !== null")

private fun jsWebUsbStartTransferOut(dataStr: JsString) {
    js(
        """
        (function() {
            window._fbUsbOut = { ready: false, error: null };
            if (!window._fbUsb || !window._fbUsb.device) {
                window._fbUsbOut.error = 'Device not connected';
                window._fbUsbOut.ready = true;
                return;
            }
            var parts = dataStr.split(',');
            var bytes = new Uint8Array(parts.length);
            for (var i = 0; i < parts.length; i++) bytes[i] = parseInt(parts[i]);
            window._fbUsb.device.transferOut(4, bytes).then(function() {
                window._fbUsbOut.ready = true;
            }).catch(function(err) {
                window._fbUsbOut.error = err.message;
                window._fbUsbOut.ready = true;
            });
        })()
        """,
    )
}

private fun jsWebUsbIsXferOutReady(): Boolean = js("window._fbUsbOut && window._fbUsbOut.ready === true")

private fun jsWebUsbGetXferOutError(): JsString? = js("(window._fbUsbOut && window._fbUsbOut.error) || null")

private fun jsWebUsbStartTransferIn(timeoutMs: Int) {
    js(
        """
        (function() {
            window._fbUsbIn = { data: null, ready: false, error: null };
            if (!window._fbUsb || !window._fbUsb.device) {
                window._fbUsbIn.error = 'Device not connected';
                window._fbUsbIn.ready = true;
                return;
            }
            var timer = setTimeout(function() {
                if (!window._fbUsbIn.ready) window._fbUsbIn.ready = true;
            }, timeoutMs);
            window._fbUsb.device.transferIn(4, 265).then(function(result) {
                clearTimeout(timer);
                if (result.data && result.data.byteLength > 0) {
                    var arr = new Uint8Array(result.data.buffer);
                    var parts = [];
                    for (var i = 0; i < arr.length; i++) parts.push(arr[i]);
                    window._fbUsbIn.data = parts.join(',');
                }
                window._fbUsbIn.ready = true;
            }).catch(function(err) {
                clearTimeout(timer);
                window._fbUsbIn.error = err.message;
                window._fbUsbIn.ready = true;
            });
        })()
        """,
    )
}

private fun jsWebUsbIsXferInReady(): Boolean = js("window._fbUsbIn && window._fbUsbIn.ready === true")

private fun jsWebUsbGetXferInError(): JsString? = js("(window._fbUsbIn && window._fbUsbIn.error) || null")

private fun jsWebUsbGetXferInData(): JsString? = js("(window._fbUsbIn && window._fbUsbIn.data) || null")

private fun jsWebUsbClose() {
    js(
        """
        (function() {
            if (window._fbUsb && window._fbUsb.device) {
                try { window._fbUsb.device.close(); } catch(e) {}
                window._fbUsb.device = null;
            }
        })()
        """,
    )
}
