/*
 * DesfireProtocol.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.card.desfire

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.card.desfire.raw.RawDesfireFileSettings
import com.codebutler.farebot.card.desfire.raw.RawDesfireManufacturingData
import com.codebutler.farebot.card.nfc.CardTransceiver

open class DesfireAccessControlException(
    message: String,
) : UnauthorizedException(message)

class DesfireNotFoundException(
    message: String,
) : NotFoundException(message)

internal class DesfireProtocol(
    private val mTransceiver: CardTransceiver,
) {
    @Throws(Exception::class)
    suspend fun getManufacturingData(): RawDesfireManufacturingData {
        val respBuffer = sendRequest(GET_MANUFACTURING_DATA)

        if (respBuffer.size != 28) {
            throw Exception("Invalid response")
        }

        return RawDesfireManufacturingData.create(respBuffer)
    }

    @Throws(Exception::class)
    suspend fun getAppList(): IntArray {
        val appDirBuf = sendRequest(GET_APPLICATION_DIRECTORY)

        val appIds = IntArray(appDirBuf.size / 3)

        var app = 0
        while (app < appDirBuf.size) {
            val appId = ByteArray(3)
            appDirBuf.copyInto(appId, 0, app, app + 3)

            appIds[app / 3] = ByteUtils.byteArrayToInt(appId)
            app += 3
        }

        return appIds
    }

    @Throws(Exception::class)
    suspend fun selectApp(appId: Int) {
        val appIdBuff = ByteArray(3)
        appIdBuff[0] = ((appId and 0xFF0000) shr 16).toByte()
        appIdBuff[1] = ((appId and 0xFF00) shr 8).toByte()
        appIdBuff[2] = (appId and 0xFF).toByte()

        sendRequest(SELECT_APPLICATION, appIdBuff)
    }

    @Throws(Exception::class)
    suspend fun getFileList(): IntArray {
        val buf = sendRequest(GET_FILES)
        val fileIds = IntArray(buf.size)
        for (x in buf.indices) {
            fileIds[x] = buf[x].toInt()
        }
        return fileIds
    }

    @Throws(Exception::class)
    suspend fun getFileSettings(fileNo: Int): RawDesfireFileSettings {
        val data = sendRequest(GET_FILE_SETTINGS, byteArrayOf(fileNo.toByte()))
        return RawDesfireFileSettings.create(data)
    }

    @Throws(Exception::class)
    suspend fun readFile(fileNo: Int): ByteArray =
        sendRequest(
            READ_DATA,
            byteArrayOf(
                fileNo.toByte(),
                0x0.toByte(),
                0x0.toByte(),
                0x0.toByte(),
                0x0.toByte(),
                0x0.toByte(),
                0x0.toByte(),
            ),
        )

    @Throws(Exception::class)
    suspend fun readRecord(fileNum: Int): ByteArray =
        sendRequest(
            READ_RECORD,
            byteArrayOf(
                fileNum.toByte(),
                0x0.toByte(),
                0x0.toByte(),
                0x0.toByte(),
                0x0.toByte(),
                0x0.toByte(),
                0x0.toByte(),
            ),
        )

    @Throws(Exception::class)
    suspend fun getValue(fileNum: Int): ByteArray =
        sendRequest(
            GET_VALUE,
            byteArrayOf(
                fileNum.toByte(),
            ),
        )

    @Throws(Exception::class)
    suspend fun sendUnlock(keyNum: Int): ByteArray =
        sendRequest(
            UNLOCK,
            byteArrayOf(
                keyNum.toByte(),
            ),
            getAdditionalFrame = false,
        )

    @Throws(Exception::class)
    suspend fun sendAdditionalFrame(bytes: ByteArray): ByteArray =
        sendRequest(
            ADDITIONAL_FRAME,
            bytes,
            getAdditionalFrame = false,
        )

    @Throws(Exception::class)
    private suspend fun sendRequest(command: Byte): ByteArray = sendRequest(command, null, getAdditionalFrame = true)

    @Throws(Exception::class)
    private suspend fun sendRequest(
        command: Byte,
        parameters: ByteArray?,
    ): ByteArray = sendRequest(command, parameters, getAdditionalFrame = true)

    @Throws(Exception::class)
    private suspend fun sendRequest(
        command: Byte,
        parameters: ByteArray?,
        getAdditionalFrame: Boolean,
    ): ByteArray {
        val outputChunks = mutableListOf<ByteArray>()

        var recvBuffer = mTransceiver.transceive(wrapMessage(command, parameters))

        while (true) {
            if (recvBuffer.size < 2 || recvBuffer[recvBuffer.size - 2] != 0x91.toByte()) {
                throw Exception("Invalid response")
            }

            outputChunks.add(recvBuffer.copyOfRange(0, recvBuffer.size - 2))

            val status = recvBuffer[recvBuffer.size - 1]
            when (status) {
                OPERATION_OK -> {
                    var totalSize = 0
                    for (chunk in outputChunks) totalSize += chunk.size
                    val result = ByteArray(totalSize)
                    var offset = 0
                    for (chunk in outputChunks) {
                        chunk.copyInto(result, offset)
                        offset += chunk.size
                    }
                    return result
                }
                ADDITIONAL_FRAME -> {
                    if (!getAdditionalFrame) {
                        var totalSize = 0
                        for (chunk in outputChunks) totalSize += chunk.size
                        val result = ByteArray(totalSize)
                        var offset = 0
                        for (chunk in outputChunks) {
                            chunk.copyInto(result, offset)
                            offset += chunk.size
                        }
                        return result
                    }
                    recvBuffer = mTransceiver.transceive(wrapMessage(GET_ADDITIONAL_FRAME, null))
                }
                PERMISSION_DENIED -> throw DesfireAccessControlException("Permission denied")
                AUTHENTICATION_ERROR -> throw DesfireAccessControlException("Authentication error")
                AID_NOT_FOUND -> throw DesfireNotFoundException("AID not found")
                FILE_NOT_FOUND -> throw DesfireNotFoundException("File not found")
                else -> throw Exception("Unknown status code: " + (status.toInt() and 0xFF).toString(16))
            }
        }
    }

    @Throws(Exception::class)
    private fun wrapMessage(
        command: Byte,
        parameters: ByteArray?,
    ): ByteArray {
        // APDU: CLA INS P1 P2 [Lc Data] Le
        val size = if (parameters != null) 6 + parameters.size else 5
        val result = ByteArray(size)
        var offset = 0

        result[offset++] = 0x90.toByte()
        result[offset++] = command
        result[offset++] = 0x00.toByte()
        result[offset++] = 0x00.toByte()
        if (parameters != null) {
            result[offset++] = parameters.size.toByte()
            parameters.copyInto(result, offset)
            offset += parameters.size
        }
        result[offset] = 0x00.toByte()

        return result
    }

    companion object {
        // Reference: http://neteril.org/files/M075031_desfire.pdf
        // Commands
        private const val UNLOCK: Byte = 0x0A.toByte()
        private const val GET_MANUFACTURING_DATA: Byte = 0x60.toByte()
        private const val GET_APPLICATION_DIRECTORY: Byte = 0x6A.toByte()
        private val GET_ADDITIONAL_FRAME: Byte = 0xAF.toByte()
        private const val SELECT_APPLICATION: Byte = 0x5A.toByte()
        private val READ_DATA: Byte = 0xBD.toByte()
        private val READ_RECORD: Byte = 0xBB.toByte()
        private const val GET_VALUE: Byte = 0x6C.toByte()
        private const val GET_FILES: Byte = 0x6F.toByte()
        private val GET_FILE_SETTINGS: Byte = 0xF5.toByte()

        // Status codes (Section 3.4)
        private const val OPERATION_OK: Byte = 0x00.toByte()
        private val PERMISSION_DENIED: Byte = 0x9D.toByte()
        private val AID_NOT_FOUND: Byte = 0xA0.toByte()
        private val AUTHENTICATION_ERROR: Byte = 0xAE.toByte()
        private val ADDITIONAL_FRAME: Byte = 0xAF.toByte()
        private val FILE_NOT_FOUND: Byte = 0xF0.toByte()
    }
}
