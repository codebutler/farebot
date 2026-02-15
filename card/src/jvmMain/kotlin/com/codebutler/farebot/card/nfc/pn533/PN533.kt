/*
 * PN533.kt
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

/**
 * High-level PN533 NFC controller protocol.
 *
 * Wraps [PN533Transport] to provide typed command methods for
 * card detection, activation, and data exchange.
 *
 * Reference: NXP PN533 User Manual, libnfc pn53x.c
 */
class PN533(
    private val transport: PN533Transport,
) {
    fun getFirmwareVersion(): FirmwareVersion {
        val resp = transport.sendCommand(CMD_GET_FIRMWARE_VERSION)
        // Response: [IC] [Ver] [Rev] [Support]
        if (resp.size < 4) throw PN533Exception("GetFirmwareVersion: short response")
        return FirmwareVersion(
            ic = resp[0].toInt() and 0xFF,
            version = resp[1].toInt() and 0xFF,
            revision = resp[2].toInt() and 0xFF,
            support = resp[3].toInt() and 0xFF,
        )
    }

    fun samConfiguration(
        mode: Byte = SAM_MODE_NORMAL,
        timeout: Byte = 0x00,
    ) {
        transport.sendCommand(
            CMD_SAM_CONFIGURATION,
            byteArrayOf(mode, timeout, 0x01),
        )
    }

    fun setParameters(flags: Int) {
        transport.sendCommand(CMD_SET_PARAMETERS, byteArrayOf(flags.toByte()))
    }

    fun resetMode() {
        transport.sendCommand(CMD_RESET_MODE, byteArrayOf(0x01))
        transport.sendAck()
        Thread.sleep(10)
    }

    fun writeRegister(
        address: Int,
        value: Int,
    ) {
        transport.sendCommand(
            CMD_WRITE_REGISTER,
            byteArrayOf(
                ((address shr 8) and 0xFF).toByte(),
                (address and 0xFF).toByte(),
                value.toByte(),
            ),
        )
    }

    fun rfConfiguration(
        item: Byte,
        data: ByteArray,
    ) {
        transport.sendCommand(
            CMD_RF_CONFIGURATION,
            byteArrayOf(item) + data,
        )
    }

    fun setMaxRetries(
        atrRetries: Byte = 0xFF.toByte(),
        pslRetries: Byte = 0x01,
        passiveActivation: Byte = 0x02,
    ) {
        rfConfiguration(
            RF_CONFIG_MAX_RETRIES,
            byteArrayOf(atrRetries, pslRetries, passiveActivation),
        )
    }

    fun rfFieldOff() {
        rfConfiguration(RF_CONFIG_RF_FIELD, byteArrayOf(0x00))
    }

    fun rfFieldOn() {
        rfConfiguration(RF_CONFIG_RF_FIELD, byteArrayOf(0x01))
    }

    fun inListPassiveTarget(
        maxTargets: Byte = 0x01,
        baudRate: Byte,
        initiatorData: ByteArray = byteArrayOf(),
    ): TargetInfo? {
        val resp =
            try {
                transport.sendCommand(
                    CMD_IN_LIST_PASSIVE_TARGET,
                    byteArrayOf(maxTargets, baudRate) + initiatorData,
                    timeoutMs = POLL_TIMEOUT_MS,
                )
            } catch (e: PN533CommandException) {
                // RC-S956 returns error frame 0x7F for unsupported baud rates
                return null
            } catch (e: PN533Exception) {
                if (e.message?.contains("TIMEOUT") == true) return null
                throw e
            }
        // Response: [NbTg] [Tg] [target data...]
        if (resp.isEmpty()) return null
        val nbTg = resp[0].toInt() and 0xFF
        if (nbTg == 0) return null

        val tg = resp[1].toInt() and 0xFF
        return when (baudRate) {
            BAUD_RATE_106_ISO14443A -> parseTypeATarget(resp, 2, tg)
            BAUD_RATE_212_FELICA, BAUD_RATE_424_FELICA -> parseFeliCaTarget(resp, 2, tg)
            else -> null
        }
    }

    fun inDataExchange(
        tg: Int,
        data: ByteArray,
    ): ByteArray {
        val resp =
            transport.sendCommand(
                CMD_IN_DATA_EXCHANGE,
                byteArrayOf(tg.toByte()) + data,
            )
        // Response: [Status] [Data...]
        if (resp.isEmpty()) throw PN533Exception("InDataExchange: empty response")
        val status = resp[0].toInt() and 0xFF
        if (status != 0x00) {
            throw PN533Exception("InDataExchange error: status=0x%02X".format(status))
        }
        return resp.copyOfRange(1, resp.size)
    }

    fun inCommunicateThru(data: ByteArray): ByteArray {
        val resp = transport.sendCommand(CMD_IN_COMMUNICATE_THRU, data)
        // Response: [Status] [Data...]
        if (resp.isEmpty()) throw PN533Exception("InCommunicateThru: empty response")
        val status = resp[0].toInt() and 0xFF
        if (status != 0x00) {
            throw PN533Exception("InCommunicateThru error: status=0x%02X".format(status))
        }
        return resp.copyOfRange(1, resp.size)
    }

    fun inRelease(tg: Int) {
        transport.sendCommand(CMD_IN_RELEASE, byteArrayOf(tg.toByte()))
    }

    fun sendAck() {
        transport.sendAck()
    }

    fun close() {
        transport.close()
    }

    private fun parseTypeATarget(
        resp: ByteArray,
        offset: Int,
        tg: Int,
    ): TargetInfo.TypeA {
        // [ATQA(2)] [SAK(1)] [NFCIDLength(1)] [NFCID(N)]
        var pos = offset
        val atqa = resp.copyOfRange(pos, pos + 2)
        pos += 2
        val sak = resp[pos]
        pos += 1
        val nfcIdLen = resp[pos].toInt() and 0xFF
        pos += 1
        val uid = resp.copyOfRange(pos, pos + nfcIdLen)
        return TargetInfo.TypeA(tg = tg, atqa = atqa, sak = sak, uid = uid)
    }

    private fun parseFeliCaTarget(
        resp: ByteArray,
        offset: Int,
        tg: Int,
    ): TargetInfo.FeliCa {
        // [POL_RES_LEN(1)] [RESPONSE_CODE(1)] [NFCID2/IDm(8)] [Pad/PMm(8)] [RD(2)?]
        var pos = offset
        val polResLen = resp[pos].toInt() and 0xFF
        pos += 1
        // Skip response code (0x01 = POLLING response)
        pos += 1
        val idm = resp.copyOfRange(pos, pos + 8)
        pos += 8
        val pmm = resp.copyOfRange(pos, pos + 8)
        return TargetInfo.FeliCa(tg = tg, idm = idm, pmm = pmm)
    }

    data class FirmwareVersion(
        val ic: Int,
        val version: Int,
        val revision: Int,
        val support: Int,
    ) {
        override fun toString(): String = "IC=0x%02X v%d.%d support=0x%02X".format(ic, version, revision, support)
    }

    sealed class TargetInfo(
        val tg: Int,
    ) {
        class TypeA(
            tg: Int,
            val atqa: ByteArray,
            val sak: Byte,
            val uid: ByteArray,
        ) : TargetInfo(tg)

        class FeliCa(
            tg: Int,
            val idm: ByteArray,
            val pmm: ByteArray,
        ) : TargetInfo(tg)
    }

    companion object {
        // PN533 command codes (host-to-PN533, TFI=0xD4)
        const val CMD_GET_FIRMWARE_VERSION: Byte = 0x02
        const val CMD_WRITE_REGISTER: Byte = 0x08
        const val CMD_SET_PARAMETERS: Byte = 0x12
        const val CMD_SAM_CONFIGURATION: Byte = 0x14
        const val CMD_RESET_MODE: Byte = 0x18
        const val CMD_RF_CONFIGURATION: Byte = 0x32
        const val CMD_IN_LIST_PASSIVE_TARGET: Byte = 0x4A
        const val CMD_IN_DATA_EXCHANGE: Byte = 0x40
        const val CMD_IN_COMMUNICATE_THRU: Byte = 0x42
        const val CMD_IN_RELEASE: Byte = 0x44

        // SAM configuration modes
        const val SAM_MODE_NORMAL: Byte = 0x01

        // RF configuration items
        const val RF_CONFIG_RF_FIELD: Byte = 0x01
        const val RF_CONFIG_MAX_RETRIES: Byte = 0x05

        // Baud rates for InListPassiveTarget
        const val BAUD_RATE_106_ISO14443A: Byte = 0x00
        const val BAUD_RATE_212_FELICA: Byte = 0x01
        const val BAUD_RATE_424_FELICA: Byte = 0x02

        // Timeout for InListPassiveTarget polling (ms)
        const val POLL_TIMEOUT_MS = 2000
    }
}
