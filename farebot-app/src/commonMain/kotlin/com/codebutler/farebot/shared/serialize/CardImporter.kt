/*
 * CardImporter.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2024 Eric Butler <eric@codebutler.com>
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
import com.codebutler.farebot.card.serialize.CardSerializer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Clock

/**
 * Result of an import operation.
 */
sealed class ImportResult {
    /**
     * Successfully imported cards.
     */
    data class Success(
        val cards: List<RawCard<*>>,
        val format: ImportFormat,
        val metadata: ImportMetadata? = null,
    ) : ImportResult()

    /**
     * Failed to import due to an error.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : ImportResult()
}

/**
 * Detected import format.
 */
enum class ImportFormat {
    /** FareBot JSON format (current) */
    FAREBOT_JSON,

    /** FareBot bulk export JSON format with metadata */
    FAREBOT_BULK_JSON,

    /** Metrodroid JSON format */
    METRODROID_JSON,

    /** Legacy FareBot/Metrodroid XML format */
    XML,

    /** Flipper Zero .nfc dump format */
    FLIPPER_NFC,

    /** Unknown format */
    UNKNOWN,
}

/**
 * Metadata extracted from an import.
 */
data class ImportMetadata(
    val appName: String? = null,
    val versionCode: Int? = null,
    val versionName: String? = null,
    val exportedAt: String? = null,
    val formatVersion: Int? = null,
)

/**
 * High-level import functionality for card data.
 *
 * Supports multiple import formats (JSON, XML) and auto-detection
 * of format from file content.
 */
class CardImporter(
    private val cardSerializer: CardSerializer,
    private val json: Json,
) {
    private val _pendingImport = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val pendingImport: SharedFlow<String> = _pendingImport.asSharedFlow()

    fun submitImport(content: String) {
        _pendingImport.tryEmit(content)
    }

    /**
     * Imports card data from a string, auto-detecting the format.
     */
    fun importCards(data: String): ImportResult {
        val trimmed = data.trim()
        return try {
            when {
                trimmed.startsWith("Filetype: Flipper NFC device") -> importFromFlipper(trimmed)
                trimmed.startsWith("{") || trimmed.startsWith("[") -> importFromJson(trimmed)
                trimmed.startsWith("<?xml") || trimmed.startsWith("<card") || trimmed.startsWith("<cards") -> {
                    // XML import not yet supported - would require XML parser
                    ImportResult.Error("XML import not yet supported. Please use JSON format.")
                }
                else -> ImportResult.Error("Unknown file format")
            }
        } catch (e: Exception) {
            ImportResult.Error("Failed to import: ${e.message}", e)
        }
    }

    /**
     * Imports cards from JSON data.
     */
    private fun importFromJson(jsonData: String): ImportResult {
        val element = json.parseToJsonElement(jsonData)

        return when {
            element is JsonArray -> {
                // Array of cards (legacy format)
                val cards =
                    element.map { cardElement ->
                        deserializeCard(cardElement)
                    }
                ImportResult.Success(cards, ImportFormat.FAREBOT_JSON)
            }

            element is JsonObject -> importFromJsonObject(element)

            else -> ImportResult.Error("Invalid JSON format")
        }
    }

    /**
     * Imports cards from a JSON object.
     */
    private fun importFromJsonObject(obj: JsonObject): ImportResult {
        // Check if it's a bulk export (has "cards" array and metadata)
        val cardsElement = obj["cards"]
        if (cardsElement != null && cardsElement is JsonArray) {
            return importBulkExport(obj, cardsElement)
        }

        // Check if it's a Metrodroid format (has scannedAt and tagId as known keys)
        // or FareBot format (has cardType at top level)
        val cardType = obj["cardType"]
        val scannedAt = obj["scannedAt"]
        val tagId = obj["tagId"]

        return when {
            cardType != null -> {
                // FareBot single card format
                val card = cardSerializer.deserialize(json.encodeToString(JsonObject.serializer(), obj))
                ImportResult.Success(listOf(card), ImportFormat.FAREBOT_JSON)
            }

            scannedAt != null && tagId != null -> {
                // Metrodroid single card format
                val card = importMetrodroidCard(obj)
                ImportResult.Success(listOf(card), ImportFormat.METRODROID_JSON)
            }

            else -> ImportResult.Error("Unknown JSON card format")
        }
    }

    /**
     * Imports cards from a bulk export JSON object.
     */
    private fun importBulkExport(
        obj: JsonObject,
        cardsArray: JsonArray,
    ): ImportResult {
        val metadata =
            ImportMetadata(
                appName = obj["appName"]?.toString()?.removeSurrounding("\""),
                versionCode = obj["versionCode"]?.toString()?.toIntOrNull(),
                versionName = obj["versionName"]?.toString()?.removeSurrounding("\""),
                exportedAt = obj["exportedAt"]?.toString()?.removeSurrounding("\""),
                formatVersion = obj["formatVersion"]?.toString()?.toIntOrNull(),
            )

        val format =
            when {
                metadata.appName == "FareBot" -> ImportFormat.FAREBOT_BULK_JSON
                metadata.appName == "Metrodroid" -> ImportFormat.METRODROID_JSON
                else -> ImportFormat.FAREBOT_BULK_JSON
            }

        val cards =
            cardsArray.map { cardElement ->
                deserializeCard(cardElement)
            }

        return ImportResult.Success(cards, format, metadata)
    }

    /**
     * Deserializes a card from a JSON element.
     * Handles both FareBot and Metrodroid formats.
     */
    private fun deserializeCard(element: JsonElement): RawCard<*> {
        val obj = element.jsonObject
        val cardType = obj["cardType"]

        return if (cardType != null) {
            // FareBot format
            cardSerializer.deserialize(json.encodeToString(JsonObject.serializer(), obj))
        } else {
            // Metrodroid format - try to convert
            importMetrodroidCard(obj)
        }
    }

    /**
     * Imports a card from Metrodroid JSON format.
     * This performs format conversion from Metrodroid's structure to FareBot's.
     *
     * Note: This is a simplified implementation that handles the most common case.
     * Complex Metrodroid cards may require additional conversion logic.
     */
    private fun importMetrodroidCard(obj: JsonObject): RawCard<*> =
        MetrodroidJsonParser.parse(obj)
            ?: throw IllegalArgumentException(
                "Unsupported Metrodroid card format. Known keys: ${obj.keys.joinToString()}",
            )

    /**
     * Imports a binary .mfc (MIFARE Classic dump) file.
     */
    fun importMfcDump(bytes: ByteArray): ImportResult =
        try {
            val rawCard = parseMfcBytes(bytes)
            ImportResult.Success(listOf(rawCard), ImportFormat.UNKNOWN)
        } catch (e: Exception) {
            ImportResult.Error("Failed to parse MFC dump: ${e.message}", e)
        }

    private fun parseMfcBytes(bytes: ByteArray): RawClassicCard {
        val sectors = mutableListOf<RawClassicSector>()
        var offset = 0
        var sectorNum = 0

        while (offset < bytes.size) {
            val blockCount = if (sectorNum >= 32) 16 else 4
            val sectorSize = blockCount * 16
            if (offset + sectorSize > bytes.size) break

            val sectorBytes = bytes.copyOfRange(offset, offset + sectorSize)
            val blocks =
                (0 until blockCount).map { blockIndex ->
                    val blockStart = blockIndex * 16
                    val blockData = sectorBytes.copyOfRange(blockStart, blockStart + 16)
                    RawClassicBlock.create(blockIndex, blockData)
                }

            sectors.add(RawClassicSector.createData(sectorNum, blocks))
            offset += sectorSize
            sectorNum++
        }

        val tagId =
            if (sectors.isNotEmpty() && !sectors[0].blocks.isNullOrEmpty()) {
                sectors[0].blocks!![0].data.copyOfRange(0, 4)
            } else {
                byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
            }

        val maxSector =
            when {
                sectorNum <= 16 -> 15
                sectorNum <= 32 -> 31
                else -> 39
            }
        while (sectors.size <= maxSector) {
            sectors.add(RawClassicSector.createUnauthorized(sectors.size))
        }

        return RawClassicCard.create(tagId, Clock.System.now(), sectors)
    }

    private fun importFromFlipper(data: String): ImportResult {
        val rawCard =
            FlipperNfcParser.parse(data)
                ?: return ImportResult.Error(
                    "Failed to parse Flipper NFC dump. Unsupported card type or malformed file.",
                )
        return ImportResult.Success(listOf(rawCard), ImportFormat.FLIPPER_NFC)
    }

    companion object {
        /**
         * Creates an importer with default settings.
         */
        fun create(
            cardSerializer: CardSerializer,
            json: Json =
                Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                },
        ): CardImporter = CardImporter(cardSerializer, json)
    }
}
