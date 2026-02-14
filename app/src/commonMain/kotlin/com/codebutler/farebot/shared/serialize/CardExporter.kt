/*
 * CardExporter.kt
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
import com.codebutler.farebot.card.serialize.CardSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

/**
 * High-level export functionality for card data.
 *
 * Supports multiple export formats (JSON, XML) and includes
 * export metadata for compatibility tracking.
 */
class CardExporter(
    private val cardSerializer: CardSerializer,
    private val json: Json,
    private val versionCode: Int = 1,
    private val versionName: String = "1.0.0",
) {
    /**
     * Exports a single card to the specified format.
     */
    fun exportCard(
        card: RawCard<*>,
        format: ExportFormat = ExportFormat.JSON,
    ): String =
        when (format) {
            ExportFormat.JSON -> exportCardToJson(card)
            ExportFormat.XML -> XmlCardExporter.exportCard(card)
        }

    /**
     * Exports multiple cards to the specified format.
     */
    fun exportCards(
        cards: List<RawCard<*>>,
        format: ExportFormat = ExportFormat.JSON,
    ): String =
        when (format) {
            ExportFormat.JSON -> exportCardsToJson(cards)
            ExportFormat.XML -> XmlCardExporter.exportCards(cards)
        }

    /**
     * Exports a single card to JSON format with full card data.
     */
    private fun exportCardToJson(card: RawCard<*>): String = cardSerializer.serialize(card)

    /**
     * Exports multiple cards to JSON format with metadata.
     * This format is compatible with Metrodroid and FareBot exports.
     *
     * Format:
     * ```json
     * {
     *   "cards": [ ... ],
     *   "appName": "FareBot",
     *   "versionCode": 1,
     *   "versionName": "1.0.0",
     *   "exportedAt": "2024-01-15T10:30:00Z",
     *   "formatVersion": 1
     * }
     * ```
     */
    private fun exportCardsToJson(cards: List<RawCard<*>>): String {
        val cardElements =
            cards.map { card ->
                json.parseToJsonElement(cardSerializer.serialize(card))
            }

        val metadata = ExportMetadata.create(versionCode, versionName)

        val export =
            buildJsonObject {
                put("cards", JsonArray(cardElements))
                put("appName", metadata.appName)
                put("versionCode", metadata.versionCode)
                put("versionName", metadata.versionName)
                put("exportedAt", metadata.exportedAt.toString())
                put("formatVersion", metadata.formatVersion)
            }

        return json.encodeToString(JsonObject.serializer(), export)
    }

    /**
     * Generates a filename for exporting a single card.
     */
    fun generateFilename(
        card: RawCard<*>,
        format: ExportFormat = ExportFormat.JSON,
    ): String = ExportHelper.makeFilename(card, format)

    /**
     * Generates a filename for bulk export.
     */
    fun generateBulkFilename(format: ExportFormat = ExportFormat.JSON): String =
        ExportHelper.makeBulkExportFilename(format, Clock.System.now())

    companion object {
        /**
         * Creates an exporter with default settings.
         */
        fun create(
            cardSerializer: CardSerializer,
            json: Json =
                Json {
                    prettyPrint = true
                    encodeDefaults = false
                },
        ): CardExporter = CardExporter(cardSerializer, json)
    }
}
