/*
 * ISO7816CardReader.kt
 *
 * Copyright 2018 Google
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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

import com.codebutler.farebot.card.iso7816.raw.RawISO7816Card
import com.codebutler.farebot.card.nfc.CardTransceiver
import kotlin.time.Clock

/**
 * Reads ISO 7816 cards by trying to SELECT BY NAME known application identifiers,
 * then reading their files, records, and balance data.
 */
object ISO7816CardReader {
    /**
     * Configuration for reading a specific ISO 7816 application type.
     *
     * @param appNames List of AIDs to try for this application type.
     * @param type Application type identifier (e.g., "china", "ksx6924").
     * @param readBalances Optional function to read balances after application selection.
     * @param sfiRange Range of SFI values to scan for records.
     * @param fileSelectors Additional file selectors to try reading.
     */
    data class AppConfig(
        val appNames: List<ByteArray>,
        val type: String,
        val readBalances: (suspend (ISO7816Protocol) -> Map<Int, ByteArray>)? = null,
        val readExtraData: (suspend (ISO7816Protocol) -> Map<String, ByteArray>)? = null,
        val sfiRange: IntRange = 0..31,
        val fileSelectors: List<FileSelector> = emptyList(),
    )

    data class FileSelector(
        val parentDf: Int? = null,
        val fileId: Int,
    )

    /**
     * Attempts to read an ISO7816 card using the given transceiver.
     *
     * @param tagId The NFC tag identifier.
     * @param transceiver The card transceiver for sending APDUs.
     * @param appConfigs List of application configurations to try.
     * @return A [RawISO7816Card] if any application was successfully selected, null otherwise.
     */
    suspend fun readCard(
        tagId: ByteArray,
        transceiver: CardTransceiver,
        appConfigs: List<AppConfig>,
        onProgress: (suspend (current: Int, total: Int) -> Unit)? = null,
    ): RawISO7816Card? {
        val protocol = ISO7816Protocol(transceiver)
        val applications = mutableListOf<ISO7816Application>()

        for ((configIndex, config) in appConfigs.withIndex()) {
            onProgress?.invoke(configIndex, appConfigs.size)
            val app = tryReadApplication(protocol, config) ?: continue
            applications.add(app)
        }

        if (applications.isEmpty()) {
            return null
        }

        return RawISO7816Card.create(tagId, Clock.System.now(), applications)
    }

    private suspend fun tryReadApplication(
        protocol: ISO7816Protocol,
        config: AppConfig,
    ): ISO7816Application? {
        // Try each AID for this application type
        var fci: ByteArray? = null
        var matchedAppName: ByteArray? = null

        for (appName in config.appNames) {
            fci = protocol.selectByNameOrNull(appName)
            if (fci != null) {
                matchedAppName = appName
                break
            }
        }

        if (fci == null || matchedAppName == null) {
            return null
        }

        // Read SFI files
        val sfiFiles = mutableMapOf<Int, ISO7816File>()
        for (sfi in config.sfiRange) {
            val file = readSfiFile(protocol, sfi)
            if (file != null) {
                sfiFiles[sfi] = file
            }
        }

        // Read additional file selectors
        val files = mutableMapOf<String, ISO7816File>()
        for (selector in config.fileSelectors) {
            val file = readFileSelector(protocol, selector, matchedAppName)
            if (file != null) {
                @OptIn(ExperimentalStdlibApi::class)
                val key =
                    if (selector.parentDf != null) {
                        "${selector.parentDf.toString(16)}/${selector.fileId.toString(16)}"
                    } else {
                        selector.fileId.toString(16)
                    }
                files[key] = file
            }
        }

        // Read balances if configured
        val balances = config.readBalances?.invoke(protocol)

        // Read extra data if configured
        val extraData = config.readExtraData?.invoke(protocol)

        // Merge all data into files map
        val allFiles = files.toMutableMap()
        if (balances != null) {
            for ((idx, data) in balances) {
                allFiles["balance/$idx"] = ISO7816File(binaryData = data)
            }
        }
        if (extraData != null) {
            for ((key, data) in extraData) {
                allFiles[key] = ISO7816File(binaryData = data)
            }
        }

        return ISO7816Application(
            appName = matchedAppName,
            appFci = fci,
            files = allFiles,
            sfiFiles = sfiFiles,
            type = config.type,
        )
    }

    private suspend fun readSfiFile(
        protocol: ISO7816Protocol,
        sfi: Int,
    ): ISO7816File? {
        val records = mutableMapOf<Int, ByteArray>()
        var binaryData: ByteArray? = null

        // Try reading records
        for (recordNum in 1..255) {
            try {
                val record = protocol.readRecord(sfi, recordNum.toByte(), 0) ?: break
                records[recordNum] = record
            } catch (e: ISOEOFException) {
                break
            } catch (e: ISO7816Exception) {
                break
            } catch (e: Exception) {
                println("[ISO7816] Record read failed at SFI $sfi, record $recordNum: $e")
                break
            }
        }

        // Try reading binary data
        try {
            binaryData = protocol.readBinary(sfi)
        } catch (e: Exception) {
            println("[ISO7816] Binary read failed for SFI $sfi: $e")
        }

        return if (records.isNotEmpty() || binaryData != null) {
            ISO7816File(binaryData = binaryData, records = records)
        } else {
            null
        }
    }

    private suspend fun readFileSelector(
        protocol: ISO7816Protocol,
        selector: FileSelector,
        appName: ByteArray,
    ): ISO7816File? {
        try {
            // If there's a parent DF, select it first
            if (selector.parentDf != null) {
                // Re-select the application to reset state
                protocol.selectByName(appName)
                protocol.selectById(selector.parentDf)
            }

            // Unselect before selecting to avoid stale state
            try {
                protocol.unselectFile()
            } catch (e: ISO7816Exception) {
                // Unselect failed, continue with select
            }

            val fci = protocol.selectById(selector.fileId)
            val records = mutableMapOf<Int, ByteArray>()

            // Try reading records
            for (recordNum in 1..255) {
                try {
                    val record = protocol.readRecord(recordNum.toByte(), 0) ?: break
                    records[recordNum] = record
                } catch (e: ISOEOFException) {
                    break
                } catch (e: Exception) {
                    println("[ISO7816] File record read failed: $e")
                    break
                }
            }

            // Try reading binary
            val binaryData =
                try {
                    protocol.readBinary()
                } catch (e: Exception) {
                    println("[ISO7816] File binary read failed: $e")
                    null
                }

            if (records.isEmpty() && binaryData == null) return null

            return ISO7816File(binaryData = binaryData, records = records, fci = fci)
        } catch (e: Exception) {
            println("[ISO7816] File read failed for app: $e")
            return null
        }
    }

    /**
     * Read China card balances using the proprietary GET BALANCE command.
     * CLA=0x80, INS=0x5c, P1=balance_index, P2=0x02, Le=4
     */
    suspend fun readChinaBalances(protocol: ISO7816Protocol): Map<Int, ByteArray> {
        val balances = mutableMapOf<Int, ByteArray>()
        for (i in 0..3) {
            try {
                val balance =
                    protocol.sendRequest(
                        ISO7816Protocol.CLASS_80,
                        0x5c.toByte(), // INS_GET_BALANCE
                        i.toByte(),
                        0x02.toByte(),
                        4, // BALANCE_RESP_LEN
                    )
                balances[i] = balance
            } catch (e: Exception) {
                println("[ISO7816] Balance read failed: $e")
            }
        }
        return balances
    }

    /**
     * Read KSX6924 balance using the proprietary GET BALANCE command.
     * CLA=0x90, INS=0x4c, P1=0, P2=0, Le=4
     */
    suspend fun readKSX6924Balance(protocol: ISO7816Protocol): ByteArray? =
        try {
            protocol.sendRequest(
                ISO7816Protocol.CLASS_90,
                0x4c.toByte(), // INS_GET_BALANCE
                0.toByte(),
                0.toByte(),
                4, // BALANCE_RESP_LEN
            )
        } catch (e: Exception) {
            println("[ISO7816] KSX6924 purse info failed: $e")
            null
        }

    /**
     * Read KSX6924 extra records using the proprietary GET RECORD command.
     * CLA=0x90, INS=0x78, P1=index, P2=0, Le=0x10
     */
    suspend fun readKSX6924ExtraRecords(protocol: ISO7816Protocol): List<ByteArray> {
        val records = mutableListOf<ByteArray>()
        try {
            for (i in 0..0xf) {
                val record =
                    protocol.sendRequest(
                        ISO7816Protocol.CLASS_90,
                        0x78.toByte(), // INS_GET_RECORD
                        i.toByte(),
                        0.toByte(),
                        0x10.toByte(),
                    )
                records.add(record)
            }
        } catch (e: Exception) {
            println("[ISO7816] KSX6924 transaction record read failed: $e")
        }
        return records
    }
}
