/*
 * ISO7816TagReader.kt
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

package com.codebutler.farebot.app.core.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.TagReader
import com.codebutler.farebot.card.china.ChinaRegistry
import com.codebutler.farebot.card.desfire.DesfireTagReader
import com.codebutler.farebot.card.iso7816.ISO7816CardReader
import com.codebutler.farebot.card.iso7816.ISO7816Protocol
import com.codebutler.farebot.card.iso7816.raw.RawISO7816Card
import com.codebutler.farebot.card.ksx6924.KSX6924Application
import com.codebutler.farebot.card.nfc.AndroidCardTransceiver
import com.codebutler.farebot.card.nfc.CardTransceiver
import com.codebutler.farebot.key.CardKeys

/**
 * Tag reader for IsoDep tags that first tries ISO 7816 SELECT BY NAME
 * for known China and KSX6924 application identifiers, then falls back
 * to the DESFire protocol if no known ISO 7816 application is found.
 */
class ISO7816TagReader(tagId: ByteArray, tag: Tag) :
    TagReader<CardTransceiver, RawCard<*>, CardKeys>(tagId, tag, null) {

    override fun getTech(tag: Tag): CardTransceiver = AndroidCardTransceiver(IsoDep.get(tag))

    @Throws(Exception::class)
    override fun readTag(
        tagId: ByteArray,
        tag: Tag,
        tech: CardTransceiver,
        cardKeys: CardKeys?
    ): RawCard<*> {
        // Try ISO7816 applications first (China cards, KSX6924/T-Money)
        val iso7816Card = tryISO7816(tagId, tech)
        if (iso7816Card != null) {
            return iso7816Card
        }

        // Fall back to DESFire protocol
        return DesfireTagReader(tagId, tag).readTag()
    }

    private fun tryISO7816(tagId: ByteArray, transceiver: CardTransceiver): RawISO7816Card? {
        val appConfigs = buildAppConfigs()
        if (appConfigs.isEmpty()) return null

        return try {
            ISO7816CardReader.readCard(tagId, transceiver, appConfigs)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun buildAppConfigs(): List<ISO7816CardReader.AppConfig> {
            val configs = mutableListOf<ISO7816CardReader.AppConfig>()

            // China transit cards
            val chinaAppNames = ChinaRegistry.allAppNames
            if (chinaAppNames.isNotEmpty()) {
                configs.add(
                    ISO7816CardReader.AppConfig(
                        appNames = chinaAppNames,
                        type = "china",
                        readBalances = { protocol ->
                            ISO7816CardReader.readChinaBalances(protocol)
                        },
                        fileSelectors = buildChinaFileSelectors()
                    )
                )
            }

            // KSX6924 (T-Money, Snapper, Cashbee)
            configs.add(
                ISO7816CardReader.AppConfig(
                    appNames = KSX6924Application.APP_NAMES,
                    type = KSX6924Application.TYPE,
                    readBalances = { protocol ->
                        val balance = ISO7816CardReader.readKSX6924Balance(protocol)
                        if (balance != null) mapOf(0 to balance) else emptyMap()
                    },
                    readExtraData = { protocol ->
                        val records = ISO7816CardReader.readKSX6924ExtraRecords(protocol)
                        records.mapIndexed { index, data -> "extra/$index" to data }.toMap()
                    },
                    fileSelectors = buildKSX6924FileSelectors()
                )
            )

            return configs
        }

        private fun buildChinaFileSelectors(): List<ISO7816CardReader.FileSelector> {
            val selectors = mutableListOf<ISO7816CardReader.FileSelector>()
            for (fileId in intArrayOf(4, 5, 8, 9, 10, 21, 24, 25)) {
                selectors.add(ISO7816CardReader.FileSelector(fileId = fileId))
                selectors.add(ISO7816CardReader.FileSelector(parentDf = 0x1001, fileId = fileId))
            }
            return selectors
        }

        private fun buildKSX6924FileSelectors(): List<ISO7816CardReader.FileSelector> {
            return (1..5).map { fileId ->
                ISO7816CardReader.FileSelector(
                    parentDf = null, // Use app's own DF
                    fileId = fileId
                )
            }
        }
    }
}
