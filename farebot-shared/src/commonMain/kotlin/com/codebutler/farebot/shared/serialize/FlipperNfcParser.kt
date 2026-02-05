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
import com.codebutler.farebot.card.ultralight.UltralightPage
import com.codebutler.farebot.card.ultralight.raw.RawUltralightCard
import kotlin.time.Clock

object FlipperNfcParser {

    fun isFlipperFormat(data: String): Boolean =
        data.trimStart().startsWith("Filetype: Flipper NFC device")

    fun parse(data: String): RawCard<*>? {
        val lines = data.lines()
        val headers = parseHeaders(lines)

        val deviceType = headers["Device type"] ?: return null

        return when (deviceType) {
            "Mifare Classic" -> parseClassic(headers, lines)
            "NTAG/Ultralight" -> parseUltralight(headers, lines)
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

    private fun parseClassic(headers: Map<String, String>, lines: List<String>): RawClassicCard? {
        val tagId = parseTagId(headers) ?: return null
        val classicType = headers["Mifare Classic type"]
        val totalSectors = when (classicType) {
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
            val allUnread = sectorBlockIndices.all { blockIdx ->
                val hex = blockDataMap[blockIdx]
                hex == null || isAllUnread(hex)
            }

            if (allUnread) {
                sectors.add(RawClassicSector.createUnauthorized(sectorIndex))
            } else {
                val blocks = sectorBlockIndices.map { blockIdx ->
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

    private fun parseUltralight(headers: Map<String, String>, lines: List<String>): RawUltralightCard? {
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

    private fun mapUltralightType(type: String?): Int = when (type) {
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
}
