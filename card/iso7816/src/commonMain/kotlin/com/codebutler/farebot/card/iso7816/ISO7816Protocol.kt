/*
 * ISO7816Protocol.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018-2019 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.iso7816

import com.codebutler.farebot.card.nfc.CardTransceiver

/**
 * Implements communication with cards that talk over ISO7816-4 APDUs.
 *
 * Android doesn't contain useful classes for interfacing with these APDUs, so this class implements
 * basic parts of the specification. In particular, this only supports open communication with the
 * card, and doesn't support writing data.
 *
 * This is used by Calypso and CEPAS cards, as well as China transit cards and KSX6924 (T-Money).
 *
 * References:
 * - EMV 4.3 Book 1 (s9, s11)
 * - https://en.wikipedia.org/wiki/Smart_card_application_protocol_data_unit
 */
class ISO7816Protocol(
    private val transceiver: CardTransceiver,
) {
    /**
     * Creates a C-APDU. (EMV 4.3 Book 1 s9.4.1)
     *
     * This always sends with Le (expected return length) of 0 (=256 bytes).
     *
     * @param cla Instruction class, may be any value but 0xFF.
     * @param ins Instruction code within the instruction class.
     * @param p1 Reference byte completing the INS.
     * @param p2 Reference byte completing the INS.
     * @param length Length of the expected return value, or 0 for no limit.
     * @param parameters Additional data to be sent in a command.
     * @return A wrapped command.
     */
    private fun wrapMessage(
        cla: Byte,
        ins: Byte,
        p1: Byte,
        p2: Byte,
        length: Byte,
        parameters: ByteArray,
    ): ByteArray {
        val hasParams = parameters.isNotEmpty()
        val size = 4 + (if (hasParams) 1 + parameters.size else 0) + 1
        val output = ByteArray(size)
        var offset = 0
        output[offset++] = cla
        output[offset++] = ins
        output[offset++] = p1
        output[offset++] = p2
        if (hasParams) {
            output[offset++] = parameters.size.toByte()
            parameters.copyInto(output, offset)
            offset += parameters.size
        }
        output[offset] = length
        return output
    }

    private suspend fun sendRequestReal(
        cla: Byte,
        ins: Byte,
        p1: Byte,
        p2: Byte,
        length: Byte,
        parameters: ByteArray,
    ): ByteArray {
        val sendBuffer = wrapMessage(cla, ins, p1, p2, length, parameters)
        val recvBuffer = transceiver.transceive(sendBuffer)

        if (recvBuffer.size == 1) {
            throw ISO7816Exception("Got 1-byte result: ${recvBuffer[0].toInt() and 0xFF}")
        }

        return recvBuffer
    }

    /**
     * Sends a command to the card and checks the response.
     *
     * @param cla Instruction class, may be any value but 0xFF.
     * @param ins Instruction code within the instruction class.
     * @param p1 Reference byte completing the INS.
     * @param p2 Reference byte completing the INS.
     * @param length Length of the expected return value, or 0 for no limit.
     * @param parameters Additional data to be sent in a command.
     * @return Response data (without status bytes).
     */
    suspend fun sendRequest(
        cla: Byte,
        ins: Byte,
        p1: Byte,
        p2: Byte,
        length: Byte,
        parameters: ByteArray = ByteArray(0),
    ): ByteArray {
        var recvBuffer = sendRequestReal(cla, ins, p1, p2, length, parameters)

        var sw1 = recvBuffer[recvBuffer.size - 2]
        var sw2 = recvBuffer[recvBuffer.size - 1]

        if (sw1 == ERROR_WRONG_LENGTH && sw2 != length) {
            recvBuffer = sendRequestReal(cla, ins, p1, p2, sw2, parameters)
            sw1 = recvBuffer[recvBuffer.size - 2]
            sw2 = recvBuffer[recvBuffer.size - 1]
        }

        if (sw1 != STATUS_OK) {
            when (sw1) {
                ERROR_COMMAND_NOT_ALLOWED ->
                    when (sw2) {
                        CNA_NO_CURRENT_EF -> throw ISONoCurrentEF()
                        CNA_SECURITY_STATUS_NOT_SATISFIED -> throw ISOSecurityStatusNotSatisfied()
                    }
                ERROR_WRONG_PARAMETERS ->
                    when (sw2) {
                        WP_FILE_NOT_FOUND -> throw ISOFileNotFoundException()
                        WP_RECORD_NOT_FOUND -> throw ISOEOFException()
                    }
                ERROR_INS_NOT_SUPPORTED_OR_INVALID ->
                    if (sw2 == 0.toByte()) throw ISOInstructionCodeNotSupported()
                ERROR_CLASS_NOT_SUPPORTED ->
                    if (sw2 == 0.toByte()) throw ISOClassNotSupported()
            }

            val sw1Hex = (sw1.toInt() and 0xFF).toString(16).padStart(2, '0')
            val sw2Hex = (sw2.toInt() and 0xFF).toString(16).padStart(2, '0')
            throw ISO7816Exception("Got unknown result: $sw1Hex$sw2Hex")
        }

        return recvBuffer.copyOfRange(0, recvBuffer.size - 2)
    }

    suspend fun selectByName(
        name: ByteArray,
        nextOccurrence: Boolean = false,
    ): ByteArray =
        sendRequest(
            CLASS_ISO7816,
            INSTRUCTION_ISO7816_SELECT,
            SELECT_BY_NAME,
            if (nextOccurrence) 0x02.toByte() else 0x00.toByte(),
            0.toByte(),
            name,
        )

    suspend fun selectByNameOrNull(name: ByteArray): ByteArray? =
        try {
            selectByName(name, false)
        } catch (e: ISO7816Exception) {
            null
        } catch (e: Exception) {
            null
        }

    suspend fun unselectFile() {
        sendRequest(CLASS_ISO7816, INSTRUCTION_ISO7816_SELECT, 0.toByte(), 0.toByte(), 0.toByte())
    }

    suspend fun selectById(fileId: Int): ByteArray {
        val file = byteArrayOf((fileId shr 8).toByte(), fileId.toByte())
        return sendRequest(
            CLASS_ISO7816,
            INSTRUCTION_ISO7816_SELECT,
            0.toByte(),
            0.toByte(),
            0.toByte(),
            file,
        )
    }

    suspend fun readRecord(
        recordNumber: Byte,
        length: Byte,
    ): ByteArray? =
        try {
            sendRequest(
                CLASS_ISO7816,
                INSTRUCTION_ISO7816_READ_RECORD,
                recordNumber,
                0x4.toByte(),
                length,
            )
        } catch (e: ISOEOFException) {
            throw e
        } catch (e: ISO7816Exception) {
            null
        }

    suspend fun readRecord(
        sfi: Int,
        recordNumber: Byte,
        length: Byte,
    ): ByteArray? =
        try {
            sendRequest(
                CLASS_ISO7816,
                INSTRUCTION_ISO7816_READ_RECORD,
                recordNumber,
                ((sfi shl 3) or 4).toByte(),
                length,
            )
        } catch (e: ISOEOFException) {
            throw e
        } catch (e: ISO7816Exception) {
            null
        }

    suspend fun readBinary(): ByteArray? =
        try {
            sendRequest(
                CLASS_ISO7816,
                INSTRUCTION_ISO7816_READ_BINARY,
                0.toByte(),
                0.toByte(),
                0.toByte(),
            )
        } catch (e: ISOEOFException) {
            throw e
        } catch (e: ISO7816Exception) {
            null
        }

    suspend fun readBinary(sfi: Int): ByteArray? =
        try {
            sendRequest(
                CLASS_ISO7816,
                INSTRUCTION_ISO7816_READ_BINARY,
                (0x80 or sfi).toByte(),
                0.toByte(),
                0.toByte(),
            )
        } catch (e: ISOEOFException) {
            throw e
        } catch (e: ISO7816Exception) {
            null
        }

    companion object {
        const val CLASS_ISO7816 = 0x00.toByte()
        const val CLASS_80 = 0x80.toByte()
        const val CLASS_90 = 0x90.toByte()

        const val INSTRUCTION_ISO7816_SELECT = 0xA4.toByte()
        const val INSTRUCTION_ISO7816_READ_BINARY = 0xB0.toByte()
        const val INSTRUCTION_ISO7816_READ_RECORD = 0xB2.toByte()

        const val ERROR_COMMAND_NOT_ALLOWED = 0x69.toByte()
        const val ERROR_WRONG_PARAMETERS = 0x6A.toByte()
        const val ERROR_WRONG_LENGTH = 0x6C.toByte()
        const val ERROR_INS_NOT_SUPPORTED_OR_INVALID = 0x6D.toByte()
        const val ERROR_CLASS_NOT_SUPPORTED = 0x6E.toByte()

        const val CNA_NO_CURRENT_EF = 0x86.toByte()
        const val CNA_SECURITY_STATUS_NOT_SATISFIED = 0x82.toByte()
        const val WP_FILE_NOT_FOUND = 0x82.toByte()
        const val WP_RECORD_NOT_FOUND = 0x83.toByte()

        const val SELECT_BY_NAME = 0x04.toByte()
        const val STATUS_OK = 0x90.toByte()
    }
}
