/*
 * FlipperNfcParser.kt
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

package com.codebutler.farebot.shared.serialize

import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.classic.raw.RawClassicBlock
import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.card.classic.raw.RawClassicSector
import com.codebutler.farebot.card.desfire.raw.RawDesfireApplication
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard
import com.codebutler.farebot.card.desfire.raw.RawDesfireFile
import com.codebutler.farebot.card.desfire.raw.RawDesfireFileSettings
import com.codebutler.farebot.card.desfire.raw.RawDesfireManufacturingData
import com.codebutler.farebot.card.felica.FeliCaIdm
import com.codebutler.farebot.card.felica.FeliCaPmm
import com.codebutler.farebot.card.felica.FelicaBlock
import com.codebutler.farebot.card.felica.FelicaService
import com.codebutler.farebot.card.felica.FelicaSystem
import com.codebutler.farebot.card.felica.raw.RawFelicaCard
import com.codebutler.farebot.card.ultralight.UltralightPage
import com.codebutler.farebot.card.ultralight.raw.RawUltralightCard
import kotlin.time.Clock

object FlipperNfcParser {
    fun isFlipperFormat(data: String): Boolean = data.trimStart().startsWith("Filetype: Flipper NFC device")

    fun parse(data: String): RawCard<*>? {
        val lines = data.lines()
        val headers = parseHeaders(lines)

        val deviceType = headers["Device type"] ?: return null

        return when (deviceType) {
            "Mifare Classic" -> parseClassic(headers, lines)
            "NTAG/Ultralight" -> parseUltralight(headers, lines)
            "Mifare DESFire" -> parseDesfire(headers, lines)
            "FeliCa" -> parseFelica(headers, lines)
            else -> null
        }
    }

    private fun parseHeaders(lines: List<String>): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        for (line in lines) {
            if (line.startsWith("Block ") || line.startsWith("Page ")) break
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim()
                val value = line.substring(colonIndex + 1).trim()
                headers[key] = value
            }
        }
        return headers
    }

    private fun parseTagId(headers: Map<String, String>): ByteArray? {
        val uid = headers["UID"] ?: return null
        return parseHexBytes(uid)
    }

    private fun parseHexBytes(hex: String): ByteArray {
        val parts = hex.trim().split(" ").filter { it.isNotEmpty() }
        return ByteArray(parts.size) { i ->
            val part = parts[i]
            if (part == "??") {
                0x00
            } else {
                part.toInt(16).toByte()
            }
        }
    }

    private fun isAllUnread(hex: String): Boolean {
        val parts = hex.trim().split(" ").filter { it.isNotEmpty() }
        return parts.all { it == "??" }
    }

    // --- DESFire parsing ---

    private fun parseDesfire(
        headers: Map<String, String>,
        lines: List<String>,
    ): RawDesfireCard? {
        val tagId = parseTagId(headers) ?: return null

        // Parse PICC Version (28 bytes of manufacturing data)
        val piccVersionHex = headers["PICC Version"] ?: return null
        val manufData = RawDesfireManufacturingData.create(parseHexBytes(piccVersionHex))

        // Parse Application IDs: space-separated hex bytes, 3 bytes per app ID
        val appIdsHex = headers["Application IDs"] ?: return null
        val appIdBytes = parseHexBytes(appIdsHex)
        val appIds = mutableListOf<Int>()
        for (i in appIdBytes.indices step 3) {
            if (i + 2 < appIdBytes.size) {
                // Flipper stores app IDs in big-endian: FF FF FF -> 0xffffff
                val id =
                    ((appIdBytes[i].toInt() and 0xFF) shl 16) or
                        ((appIdBytes[i + 1].toInt() and 0xFF) shl 8) or
                        (appIdBytes[i + 2].toInt() and 0xFF)
                appIds.add(id)
            }
        }

        // Parse each application's files
        val apps =
            appIds.map { appId ->
                parseDesfireApplication(appId, lines)
            }

        return RawDesfireCard.create(tagId, Clock.System.now(), apps, manufData)
    }

    private fun parseDesfireApplication(
        appId: Int,
        lines: List<String>,
    ): RawDesfireApplication {
        val appHex = appId.toString(16).padStart(6, '0')
        val prefix = "Application $appHex"

        // Find file IDs line
        val fileIdsLine = lines.firstOrNull { it.startsWith("$prefix File IDs:") }
        if (fileIdsLine == null) {
            return RawDesfireApplication.create(appId, emptyList())
        }
        val fileIdsHex = fileIdsLine.substringAfter("File IDs:").trim()
        val fileIds = fileIdsHex.split(" ").filter { it.isNotEmpty() }.map { it.toInt(16) }

        // Parse each file
        val files =
            fileIds.map { fileId ->
                parseDesfireFile(appHex, fileId, lines)
            }

        return RawDesfireApplication.create(appId, files)
    }

    private fun parseDesfireFile(
        appHex: String,
        fileId: Int,
        lines: List<String>,
    ): RawDesfireFile {
        val prefix = "Application $appHex File $fileId"

        // Read file properties
        val fileType = findDesfireProperty(lines, prefix, "Type")?.toIntOrNull() ?: 0
        val commSettings = findDesfireProperty(lines, prefix, "Communication Settings")?.toIntOrNull() ?: 0
        val accessRightsHex = findDesfireProperty(lines, prefix, "Access Rights") ?: "00 00"
        val accessRights = parseHexBytes(accessRightsHex)
        val size = findDesfireProperty(lines, prefix, "Size")?.toIntOrNull() ?: 0

        // Build file settings bytes based on type
        val fileTypeByte = fileType.toByte()
        val commByte = commSettings.toByte()

        val settingsBytes: ByteArray
        val fileData: ByteArray?

        when (fileType) {
            0x00, 0x01 -> {
                // Standard / Backup: [type, comm, ar0, ar1, sizeLE0, sizeLE1, sizeLE2]
                settingsBytes =
                    byteArrayOf(
                        fileTypeByte,
                        commByte,
                        accessRights[0],
                        accessRights[1],
                        (size and 0xFF).toByte(),
                        ((size shr 8) and 0xFF).toByte(),
                        ((size shr 16) and 0xFF).toByte(),
                    )
                // Look for data line
                fileData = findDesfireFileData(appHex, fileId, lines)
                    ?: ByteArray(size) // Fallback: empty data of declared size
            }
            0x03, 0x04 -> {
                // Linear Record / Cyclic Record
                val max = findDesfireProperty(lines, prefix, "Max")?.toIntOrNull() ?: 0
                val cur = findDesfireProperty(lines, prefix, "Cur")?.toIntOrNull() ?: 0
                settingsBytes =
                    byteArrayOf(
                        fileTypeByte,
                        commByte,
                        accessRights[0],
                        accessRights[1],
                        (size and 0xFF).toByte(),
                        ((size shr 8) and 0xFF).toByte(),
                        ((size shr 16) and 0xFF).toByte(),
                        (max and 0xFF).toByte(),
                        ((max shr 8) and 0xFF).toByte(),
                        ((max shr 16) and 0xFF).toByte(),
                        (cur and 0xFF).toByte(),
                        ((cur shr 8) and 0xFF).toByte(),
                        ((cur shr 16) and 0xFF).toByte(),
                    )
                // Flipper cannot dump record files inline, so check for data anyway
                fileData = findDesfireFileData(appHex, fileId, lines)
                if (fileData == null) {
                    // No data available for record files — mark as invalid
                    return RawDesfireFile.createInvalid(
                        fileId,
                        RawDesfireFileSettings.create(settingsBytes),
                        "Record file data not available from Flipper dump",
                    )
                }
            }
            0x02 -> {
                // Value file: we don't have full settings from Flipper, create minimal
                settingsBytes =
                    byteArrayOf(
                        fileTypeByte,
                        commByte,
                        accessRights[0],
                        accessRights[1],
                        0,
                        0,
                        0,
                        0, // lowerLimit
                        0xFF.toByte(),
                        0xFF.toByte(),
                        0xFF.toByte(),
                        0x7F, // upperLimit
                        0,
                        0,
                        0,
                        0, // limitedCreditValue
                        0, // limitedCreditEnabled
                    )
                fileData = findDesfireFileData(appHex, fileId, lines)
                if (fileData == null) {
                    return RawDesfireFile.createInvalid(
                        fileId,
                        RawDesfireFileSettings.create(settingsBytes),
                        "Value file data not available from Flipper dump",
                    )
                }
            }
            else -> {
                settingsBytes =
                    byteArrayOf(
                        fileTypeByte,
                        commByte,
                        accessRights[0],
                        accessRights[1],
                        (size and 0xFF).toByte(),
                        ((size shr 8) and 0xFF).toByte(),
                        ((size shr 16) and 0xFF).toByte(),
                    )
                fileData = findDesfireFileData(appHex, fileId, lines)
                    ?: ByteArray(size)
            }
        }

        return RawDesfireFile.create(
            fileId,
            RawDesfireFileSettings.create(settingsBytes),
            fileData,
        )
    }

    private fun findDesfireProperty(
        lines: List<String>,
        prefix: String,
        property: String,
    ): String? {
        val key = "$prefix $property:"
        val line = lines.firstOrNull { it.startsWith(key) } ?: return null
        return line.substringAfter("$property:").trim()
    }

    private fun findDesfireFileData(
        appHex: String,
        fileId: Int,
        lines: List<String>,
    ): ByteArray? {
        // Data line is "Application {hex} File {id}: XX XX XX ..." with no property keyword
        // Property lines have "File N Type:", "File N Size:", etc.
        val dataPrefix = "Application $appHex File $fileId:"
        for (line in lines) {
            if (!line.startsWith(dataPrefix)) continue
            val afterPrefix = line.substringAfter(dataPrefix).trim()
            // Skip property lines (they have a keyword like "Type:", "Size:", etc.)
            if (afterPrefix.isEmpty()) continue
            // Check if it looks like hex data (starts with two hex chars)
            val firstToken = afterPrefix.split(" ").firstOrNull() ?: continue
            if (firstToken.length == 2 && firstToken.all { it in "0123456789ABCDEFabcdef" }) {
                return parseHexBytes(afterPrefix)
            }
        }
        return null
    }

    // --- FeliCa parsing ---

    private fun parseFelica(
        headers: Map<String, String>,
        lines: List<String>,
    ): RawFelicaCard? {
        val tagId = parseTagId(headers) ?: return null

        // Parse IDm and PMm
        val idmHex = headers["Manufacture id"] ?: return null
        val pmmHex = headers["Manufacture parameter"] ?: return null
        val idm = FeliCaIdm(parseHexBytes(idmHex))
        val pmm = FeliCaPmm(parseHexBytes(pmmHex))

        // Parse systems
        val systems = parseFelicaSystems(lines)

        return RawFelicaCard.create(tagId, Clock.System.now(), idm, pmm, systems)
    }

    private fun parseFelicaSystems(lines: List<String>): List<FelicaSystem> {
        val systems = mutableListOf<FelicaSystem>()

        // Find system declarations: "System NN: XXXX"
        val systemEntries = mutableListOf<Pair<Int, Int>>() // (lineIndex, systemCode)
        for ((index, line) in lines.withIndex()) {
            val match = SYSTEM_REGEX.matchEntire(line.trim())
            if (match != null) {
                val systemCode = match.groupValues[2].toInt(16)
                systemEntries.add(index to systemCode)
            }
        }

        for ((entryIndex, entry) in systemEntries.withIndex()) {
            val (startLine, systemCode) = entry
            val endLine =
                if (entryIndex + 1 < systemEntries.size) {
                    systemEntries[entryIndex + 1].first
                } else {
                    lines.size
                }

            val systemLines = lines.subList(startLine, endLine)

            // Collect all service codes from service listing
            val allServiceCodes = mutableSetOf<Int>()
            for (line in systemLines) {
                val serviceMatch = FELICA_SERVICE_REGEX.matchEntire(line.trim())
                if (serviceMatch != null) {
                    val code = serviceMatch.groupValues[1].toInt(16)
                    allServiceCodes.add(code)
                }
            }

            // Collect block data grouped by service code
            val serviceBlocks = mutableMapOf<Int, MutableList<FelicaBlock>>()
            for (line in systemLines) {
                val blockMatch = FELICA_BLOCK_REGEX.matchEntire(line.trim())
                if (blockMatch != null) {
                    val serviceCode = blockMatch.groupValues[1].toInt(16)
                    val blockIndex = blockMatch.groupValues[2].toInt(16)
                    val dataHex = blockMatch.groupValues[3]
                    val data = parseHexBytes(dataHex)
                    serviceBlocks
                        .getOrPut(serviceCode) { mutableListOf() }
                        .add(FelicaBlock.create(blockIndex.toByte(), data))
                }
            }

            // Build services from block data
            val services =
                serviceBlocks.map { (serviceCode, blocks) ->
                    FelicaService.create(serviceCode, blocks.sortedBy { it.address })
                }

            systems.add(FelicaSystem.create(systemCode, services, allServiceCodes))
        }

        return systems
    }

    // --- Classic parsing ---

    private fun parseClassic(
        headers: Map<String, String>,
        lines: List<String>,
    ): RawClassicCard? {
        val tagId = parseTagId(headers) ?: return null
        val classicType = headers["Mifare Classic type"]
        val totalSectors =
            when (classicType) {
                "4K" -> 40
                "1K" -> 16
                "Mini" -> 5
                else -> 16
            }

        // Parse all block lines
        val blockDataMap = mutableMapOf<Int, String>()
        for (line in lines) {
            val match = BLOCK_REGEX.matchEntire(line) ?: continue
            val blockIndex = match.groupValues[1].toInt()
            val blockHex = match.groupValues[2]
            blockDataMap[blockIndex] = blockHex
        }

        // Group blocks into sectors
        val sectors = mutableListOf<RawClassicSector>()
        var currentBlock = 0
        for (sectorIndex in 0 until totalSectors) {
            val blocksPerSector = if (sectorIndex < 32) 4 else 16
            val sectorBlockIndices = (currentBlock until currentBlock + blocksPerSector)

            // Check if ALL blocks in this sector are unread
            val allUnread =
                sectorBlockIndices.all { blockIdx ->
                    val hex = blockDataMap[blockIdx]
                    hex == null || isAllUnread(hex)
                }

            if (allUnread) {
                sectors.add(RawClassicSector.createUnauthorized(sectorIndex))
            } else {
                val blocks =
                    sectorBlockIndices.map { blockIdx ->
                        val hex = blockDataMap[blockIdx]
                        val data = if (hex != null) parseHexBytes(hex) else ByteArray(16)
                        RawClassicBlock.create(blockIdx, data)
                    }
                sectors.add(RawClassicSector.createData(sectorIndex, blocks))
            }

            currentBlock += blocksPerSector
        }

        return RawClassicCard.create(tagId, Clock.System.now(), sectors)
    }

    // --- Ultralight parsing ---

    private fun parseUltralight(
        headers: Map<String, String>,
        lines: List<String>,
    ): RawUltralightCard? {
        val tagId = parseTagId(headers) ?: return null

        // Parse page lines
        val pages = mutableListOf<UltralightPage>()
        for (line in lines) {
            val match = PAGE_REGEX.matchEntire(line) ?: continue
            val pageIndex = match.groupValues[1].toInt()
            val pageHex = match.groupValues[2]
            val data = parseHexBytes(pageHex)
            pages.add(UltralightPage.create(pageIndex, data))
        }

        if (pages.isEmpty()) return null

        val ultralightType = mapUltralightType(headers["NTAG/Ultralight type"])

        return RawUltralightCard.create(tagId, Clock.System.now(), pages, ultralightType)
    }

    private fun mapUltralightType(type: String?): Int =
        when (type) {
            "NTAG213" -> 2
            "NTAG215" -> 4
            "NTAG216" -> 6
            "Ultralight" -> 0
            "Ultralight C" -> 1
            "Ultralight EV1 11" -> 0
            "Ultralight EV1 21" -> 0
            "NTAG203" -> 0
            "NTAGI2C 1K" -> 0
            "NTAGI2C 2K" -> 0
            "NTAGI2C Plus 1K" -> 0
            "NTAGI2C Plus 2K" -> 0
            else -> 0
        }

    private val BLOCK_REGEX = Regex("""Block (\d+): (.+)""")
    private val PAGE_REGEX = Regex("""Page (\d+): (.+)""")
    private val SYSTEM_REGEX = Regex("""System (\d+): ([0-9A-Fa-f]{4})""")
    private val FELICA_SERVICE_REGEX = Regex("""Service [0-9A-Fa-f]+: \| Code ([0-9A-Fa-f]{4}) \|.*""")
    private val FELICA_BLOCK_REGEX =
        Regex(
            """Block [0-9A-Fa-f]+: \| Service code ([0-9A-Fa-f]{4}) \| Block index ([0-9A-Fa-f]{2}) \| Data: ((?:[0-9A-Fa-f]{2} )*[0-9A-Fa-f]{2}) \|""",
        )
}
