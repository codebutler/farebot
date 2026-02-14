/*
 * XmlCardExporter.kt
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

import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.base.util.toBase64
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.cepas.raw.RawCEPASCard
import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.card.classic.raw.RawClassicSector
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard
import com.codebutler.farebot.card.felica.raw.RawFelicaCard
import com.codebutler.farebot.card.iso7816.raw.RawISO7816Card
import com.codebutler.farebot.card.ultralight.raw.RawUltralightCard
import com.codebutler.farebot.card.vicinity.raw.RawVicinityCard

/**
 * Exports cards to XML format compatible with Metrodroid/legacy FareBot.
 *
 * The XML format uses the same structure as the original FareBot Android app
 * to ensure backward compatibility with existing tools and Metrodroid.
 */
object XmlCardExporter {
    private const val XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"

    /**
     * Exports a single card to XML format.
     */
    fun exportCard(card: RawCard<*>): String =
        buildString {
            append(XML_HEADER)
            append("\n")
            appendCardXml(card)
        }

    /**
     * Exports multiple cards to XML format wrapped in a <cards> element.
     */
    fun exportCards(cards: List<RawCard<*>>): String =
        buildString {
            append(XML_HEADER)
            append("\n<cards>\n")
            for (card in cards) {
                appendCardXml(card, indent = "  ")
                append("\n")
            }
            append("</cards>")
        }

    private fun StringBuilder.appendCardXml(
        card: RawCard<*>,
        indent: String = "",
    ) {
        val tagId = card.tagId().hex()
        val scannedAt = card.scannedAt().toEpochMilliseconds()
        val cardType = card.cardType().toInteger()

        append(indent)
        append("<card")
        appendAttr("type", cardType.toString())
        appendAttr("id", tagId)
        appendAttr("scanned_at", scannedAt.toString())
        append(">")
        append("\n")

        when (card) {
            is RawDesfireCard -> appendDesfireCard(card, "$indent  ")
            is RawClassicCard -> appendClassicCard(card, "$indent  ")
            is RawUltralightCard -> appendUltralightCard(card, "$indent  ")
            is RawFelicaCard -> appendFelicaCard(card, "$indent  ")
            is RawCEPASCard -> appendCepasCard(card, "$indent  ")
            is RawISO7816Card -> appendIso7816Card(card, "$indent  ")
            is RawVicinityCard -> appendVicinityCard(card, "$indent  ")
        }

        append(indent)
        append("</card>")
    }

    private fun StringBuilder.appendDesfireCard(
        card: RawDesfireCard,
        indent: String,
    ) {
        // Manufacturing data - export raw bytes as base64
        append(indent)
        append("<manufacturing-data>")
        append(card.manufacturingData.data.toBase64())
        append("</manufacturing-data>\n")

        // Applications
        for (app in card.applications) {
            append(indent)
            append("<application")
            appendAttr("id", app.appId.toString())
            append(">\n")

            for (file in app.files) {
                val fileIndent = "$indent  "
                append(fileIndent)
                append("<file")
                appendAttr("id", file.fileId.toString())
                append(">\n")

                // Settings - export raw bytes
                append("$fileIndent  ")
                append("<settings>")
                append(file.fileSettings.data.toBase64())
                append("</settings>\n")

                val error = file.error
                val fileData = file.fileData
                if (error != null) {
                    append("$fileIndent  ")
                    append("<error")
                    appendAttr("type", error.type.toString())
                    append(">")
                    appendEscaped(error.message ?: "")
                    append("</error>\n")
                } else if (fileData != null) {
                    append("$fileIndent  ")
                    append("<data>")
                    append(fileData.toBase64())
                    append("</data>\n")
                }

                append(fileIndent)
                append("</file>\n")
            }

            append(indent)
            append("</application>\n")
        }
    }

    private fun StringBuilder.appendClassicCard(
        card: RawClassicCard,
        indent: String,
    ) {
        for (sector in card.sectors()) {
            append(indent)
            append("<sector")
            appendAttr("index", sector.index.toString())

            when (sector.type) {
                RawClassicSector.TYPE_UNAUTHORIZED -> {
                    appendAttr("unauthorized", "true")
                    val errMsg = sector.errorMessage
                    if (errMsg != null) {
                        append(">\n")
                        append("$indent  ")
                        append("<error>")
                        appendEscaped(errMsg)
                        append("</error>\n")
                        append(indent)
                        append("</sector>\n")
                    } else {
                        append("/>\n")
                    }
                }
                RawClassicSector.TYPE_INVALID -> {
                    appendAttr("invalid", "true")
                    val errMsg = sector.errorMessage
                    if (errMsg != null) {
                        append(">\n")
                        append("$indent  ")
                        append("<error>")
                        appendEscaped(errMsg)
                        append("</error>\n")
                        append(indent)
                        append("</sector>\n")
                    } else {
                        append("/>\n")
                    }
                }
                else -> {
                    append(">\n")

                    sector.blocks?.let { blocks ->
                        for (block in blocks) {
                            append("$indent  ")
                            append("<block")
                            appendAttr("index", block.index.toString())
                            appendAttr("type", block.type())
                            append(">")
                            append(block.data.toBase64())
                            append("</block>\n")
                        }
                    }

                    append(indent)
                    append("</sector>\n")
                }
            }
        }
    }

    private fun StringBuilder.appendUltralightCard(
        card: RawUltralightCard,
        indent: String,
    ) {
        append(indent)
        append("<ultralightType>")
        append(card.ultralightType.toString())
        append("</ultralightType>\n")

        for (page in card.pages) {
            append(indent)
            append("<page")
            appendAttr("index", page.index.toString())
            append(">")
            append(page.data.toBase64())
            append("</page>\n")
        }
    }

    private fun StringBuilder.appendFelicaCard(
        card: RawFelicaCard,
        indent: String,
    ) {
        // IDm
        append(indent)
        append("<idm>")
        append(card.idm.getBytes().toBase64())
        append("</idm>\n")

        // PMm
        append(indent)
        append("<pmm>")
        append(card.pmm.getBytes().toBase64())
        append("</pmm>\n")

        // Systems
        for (system in card.systems) {
            append(indent)
            append("<system")
            appendAttr("code", system.code.toString())
            if (system.skipped) {
                appendAttr("skipped", "true")
            }
            append(">\n")

            for (service in system.services) {
                append("$indent  ")
                append("<service")
                appendAttr("code", service.serviceCode.toString())
                if (service.skipped) {
                    appendAttr("skipped", "true")
                }
                append(">\n")

                for (block in service.blocks) {
                    append("$indent    ")
                    append("<block")
                    appendAttr("address", block.address.toInt().toString())
                    append(">")
                    append(block.data.toBase64())
                    append("</block>\n")
                }

                append("$indent  ")
                append("</service>\n")
            }

            append(indent)
            append("</system>\n")
        }
    }

    private fun StringBuilder.appendCepasCard(
        card: RawCEPASCard,
        indent: String,
    ) {
        // Purses
        for (purse in card.purses) {
            append(indent)
            append("<purse")
            appendAttr("id", purse.id.toString())
            val purseErrMsg = purse.errorMessage
            val purseData = purse.data
            if (purseErrMsg != null) {
                append(">\n")
                append("$indent  ")
                append("<error>")
                appendEscaped(purseErrMsg)
                append("</error>\n")
                append(indent)
                append("</purse>\n")
            } else if (purseData != null) {
                append(">")
                append(purseData.toBase64())
                append("</purse>\n")
            } else {
                append("/>\n")
            }
        }

        // Histories
        for (history in card.histories) {
            append(indent)
            append("<history")
            appendAttr("id", history.id.toString())
            val histErrMsg = history.errorMessage
            val histData = history.data
            if (histErrMsg != null) {
                append(">\n")
                append("$indent  ")
                append("<error>")
                appendEscaped(histErrMsg)
                append("</error>\n")
                append(indent)
                append("</history>\n")
            } else if (histData != null) {
                append(">")
                append(histData.toBase64())
                append("</history>\n")
            } else {
                append("/>\n")
            }
        }
    }

    private fun StringBuilder.appendIso7816Card(
        card: RawISO7816Card,
        indent: String,
    ) {
        for (app in card.applications) {
            append(indent)
            append("<application")
            appendAttr("type", app.type)
            val appName = app.appName
            val appFci = app.appFci
            if (appName != null) {
                appendAttr("application-name", appName.toBase64())
            }
            if (appFci != null) {
                appendAttr("application-data", appFci.toBase64())
            }
            append(">\n")

            // Regular files
            for ((selector, file) in app.files) {
                append("$indent  ")
                append("<file")
                appendAttr("name", selector)
                append(">\n")

                for ((recIndex, record) in file.records) {
                    append("$indent    ")
                    append("<record")
                    appendAttr("index", recIndex.toString())
                    append(">")
                    append(record.toBase64())
                    append("</record>\n")
                }

                val binaryData = file.binaryData
                val fci = file.fci
                if (binaryData != null) {
                    append("$indent    ")
                    append("<data>")
                    append(binaryData.toBase64())
                    append("</data>\n")
                }

                if (fci != null) {
                    append("$indent    ")
                    append("<fci>")
                    append(fci.toBase64())
                    append("</fci>\n")
                }

                append("$indent  ")
                append("</file>\n")
            }

            // SFI files
            for ((sfi, file) in app.sfiFiles) {
                append("$indent  ")
                append("<sfi-file")
                appendAttr("sfi", sfi.toString())
                append(">\n")

                for ((recIndex, record) in file.records) {
                    append("$indent    ")
                    append("<record")
                    appendAttr("index", recIndex.toString())
                    append(">")
                    append(record.toBase64())
                    append("</record>\n")
                }

                val binaryData = file.binaryData
                if (binaryData != null) {
                    append("$indent    ")
                    append("<data>")
                    append(binaryData.toBase64())
                    append("</data>\n")
                }

                append("$indent  ")
                append("</sfi-file>\n")
            }

            append(indent)
            append("</application>\n")
        }
    }

    private fun StringBuilder.appendVicinityCard(
        card: RawVicinityCard,
        indent: String,
    ) {
        val sysInfo = card.sysInfo
        if (sysInfo != null) {
            append(indent)
            append("<sysinfo>")
            append(sysInfo.toBase64())
            append("</sysinfo>\n")
        }

        if (card.isPartialRead) {
            append(indent)
            append("<partial_read>true</partial_read>\n")
        }

        for (page in card.pages) {
            append(indent)
            append("<page")
            appendAttr("index", page.index.toString())
            if (page.isUnauthorized) {
                appendAttr("unauthorized", "true")
            }
            append(">")
            if (!page.isUnauthorized) {
                append(page.data.toBase64())
            }
            append("</page>\n")
        }
    }

    private fun StringBuilder.appendAttr(
        name: String,
        value: String,
    ) {
        append(" ")
        append(name)
        append("=\"")
        appendEscapedAttr(value)
        append("\"")
    }

    private fun StringBuilder.appendEscaped(text: String) {
        for (c in text) {
            when (c) {
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '&' -> append("&amp;")
                else -> append(c)
            }
        }
    }

    private fun StringBuilder.appendEscapedAttr(text: String) {
        for (c in text) {
            when (c) {
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '&' -> append("&amp;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(c)
            }
        }
    }

    private fun CardType.toInteger(): Int =
        when (this) {
            CardType.MifareClassic -> 0
            CardType.MifareUltralight -> 1
            CardType.MifareDesfire -> 2
            CardType.CEPAS -> 3
            CardType.FeliCa -> 4
            CardType.ISO7816 -> 5
            CardType.Vicinity -> 6
            CardType.Sample -> 7
        }
}
