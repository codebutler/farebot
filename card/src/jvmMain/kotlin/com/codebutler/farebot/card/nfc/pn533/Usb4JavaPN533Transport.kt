/*
 * Usb4JavaPN533Transport.kt
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

package com.codebutler.farebot.card.nfc.pn533

import com.codebutler.farebot.base.util.hex
import org.usb4java.DeviceHandle
import org.usb4java.LibUsb
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * Raw USB frame I/O for the PN533 NFC controller via usb4java.
 *
 * Implements the PN533 normal information frame format:
 * - Normal:   [00 00 FF] [LEN] [LCS] [TFI CMD DATA...] [DCS] [00]
 * - Extended: [00 00 FF FF FF] [LEN_H LEN_L] [LCS] [TFI CMD DATA...] [DCS] [00]
 *
 * Reference: NXP PN533 User Manual, libnfc pn53x_usb.c, nfcpy pn533.py
 */
class Usb4JavaPN533Transport(
    private val handle: DeviceHandle,
) : PN533Transport {
    /**
     * Drain any stale data from the USB read buffer.
     * Call after opening the device to clear leftovers from previous sessions.
     */
    override fun flush() {
        val buf = ByteBuffer.allocateDirect(64)
        val transferred = IntBuffer.allocate(1)
        repeat(MAX_FLUSH_READS) {
            buf.clear()
            transferred.clear()
            val result = LibUsb.bulkTransfer(handle, ENDPOINT_IN, buf, transferred, FLUSH_TIMEOUT_MS.toLong())
            if (result == LibUsb.ERROR_TIMEOUT || transferred.get(0) == 0) return
        }
    }

    override suspend fun sendCommand(
        code: Byte,
        data: ByteArray,
        timeoutMs: Int,
    ): ByteArray {
        val payload = byteArrayOf(TFI_HOST_TO_PN533, code) + data
        val frame = buildFrame(payload)

        if (DEBUG) println("[PN53x TX] cmd=0x%02X frame=${frame.hex()}".format(code))
        bulkWrite(frame)
        val staleResponse = readAck()
        // If readAck got a response frame instead of an ACK, use it directly
        if (staleResponse != null) {
            if (DEBUG) println("[PN53x RX] (stale) ${staleResponse.hex()}")
            return parseFrame(staleResponse)
        }
        return readResponse(timeoutMs)
    }

    override suspend fun sendAck() {
        bulkWrite(ACK_FRAME)
    }

    override fun close() {
        LibUsb.releaseInterface(handle, 0)
        LibUsb.close(handle)
    }

    private fun buildFrame(payload: ByteArray): ByteArray {
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
            // Extended frame
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

    /**
     * Read and validate the ACK frame. Returns null if ACK was received normally.
     * If a response frame arrives instead of ACK (stale data race), returns the raw
     * frame bytes so the caller can parse them directly.
     */
    private fun readAck(): ByteArray? {
        val buf = ByteBuffer.allocateDirect(MAX_FRAME_SIZE)
        val transferred = IntBuffer.allocate(1)
        val result = LibUsb.bulkTransfer(handle, ENDPOINT_IN, buf, transferred, TIMEOUT_MS.toLong())
        if (result != LibUsb.SUCCESS && result != LibUsb.ERROR_TIMEOUT) {
            throw PN533TransportException("USB read ACK failed: ${LibUsb.errorName(result)}")
        }
        val count = transferred.get(0)
        val bytes = ByteArray(count)
        buf.rewind()
        buf.get(bytes)

        if (bytes.size >= ACK_FRAME.size && bytes.copyOfRange(0, ACK_FRAME.size).contentEquals(ACK_FRAME)) {
            return null // ACK received
        }

        // Not an ACK — likely a response frame (stale data or ACK was skipped)
        if (bytes.size >= 6 && bytes[0] == 0x00.toByte() && bytes[1] == 0x00.toByte() && bytes[2] == 0xFF.toByte()) {
            return bytes // Return as response frame
        }

        throw PN533Exception("Expected ACK, got: ${bytes.hex()}")
    }

    private fun readResponse(timeoutMs: Int = TIMEOUT_MS): ByteArray {
        val buf = ByteBuffer.allocateDirect(MAX_FRAME_SIZE)
        val transferred = IntBuffer.allocate(1)
        val result = LibUsb.bulkTransfer(handle, ENDPOINT_IN, buf, transferred, timeoutMs.toLong())
        if (result != LibUsb.SUCCESS) {
            throw PN533TransportException("USB read response failed: ${LibUsb.errorName(result)}")
        }
        val count = transferred.get(0)
        val bytes = ByteArray(count)
        buf.rewind()
        buf.get(bytes)
        if (DEBUG) println("[PN53x RX] ${bytes.hex()}")
        return parseFrame(bytes)
    }

    private fun parseFrame(frame: ByteArray): ByteArray {
        if (frame.size < 6) {
            throw PN533Exception("Frame too short: ${frame.size} bytes")
        }
        // Verify preamble + start code
        if (frame[0] != 0x00.toByte() || frame[1] != 0x00.toByte() || frame[2] != 0xFF.toByte()) {
            throw PN533Exception("Invalid frame preamble: ${frame.hex()}")
        }

        val payload: ByteArray
        if (frame[3] == 0xFF.toByte() && frame[4] == 0xFF.toByte()) {
            // Extended frame
            if (frame.size < 10) {
                throw PN533Exception("Extended frame too short: ${frame.size}")
            }
            val lenH = frame[5].toInt() and 0xFF
            val lenL = frame[6].toInt() and 0xFF
            val len = (lenH shl 8) or lenL
            // LCS at frame[7]
            payload = frame.copyOfRange(8, 8 + len)
        } else {
            // Normal frame
            val len = frame[3].toInt() and 0xFF
            // LCS at frame[4]
            payload = frame.copyOfRange(5, 5 + len)
        }

        // Error frame: 1-byte payload with error code (e.g., 0x7F = command not supported)
        if (payload.size == 1) {
            throw PN533CommandException(payload[0].toInt() and 0xFF)
        }

        // payload[0] = TFI (0xD5 for PN533-to-host), payload[1] = response code
        if (payload.size < 2 || payload[0] != TFI_PN533_TO_HOST) {
            throw PN533Exception("Invalid TFI in response: ${payload.hex()}")
        }

        // Return everything after TFI and response code (data only)
        return payload.copyOfRange(2, payload.size)
    }

    private fun bulkWrite(data: ByteArray) {
        val buf = ByteBuffer.allocateDirect(data.size)
        buf.put(data)
        buf.rewind()
        val transferred = IntBuffer.allocate(1)
        val result = LibUsb.bulkTransfer(handle, ENDPOINT_OUT, buf, transferred, TIMEOUT_MS.toLong())
        if (result != LibUsb.SUCCESS) {
            throw PN533TransportException("USB write failed: ${LibUsb.errorName(result)}")
        }
    }

    companion object {
        const val ENDPOINT_OUT: Byte = 0x04
        val ENDPOINT_IN: Byte = 0x84.toByte()
        const val TIMEOUT_MS = 5000
        const val FLUSH_TIMEOUT_MS = 100
        const val MAX_FRAME_SIZE = 265
        const val MAX_FLUSH_READS = 10

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

        const val DEBUG = false
    }
}
