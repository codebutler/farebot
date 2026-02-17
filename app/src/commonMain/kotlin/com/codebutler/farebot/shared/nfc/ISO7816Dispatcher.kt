/*
 * ISO7816Dispatcher.kt
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

package com.codebutler.farebot.shared.nfc

import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.china.ChinaRegistry
import com.codebutler.farebot.card.desfire.DesfireCardReader
import com.codebutler.farebot.card.iso7816.ISO7816CardReader
import com.codebutler.farebot.card.ksx6924.KSX6924Application
import com.codebutler.farebot.card.nfc.CardTransceiver

/**
 * Shared ISO 7816 / DESFire dispatch logic.
 *
 * Tries known ISO 7816 applications (China transit, KSX6924/T-Money) first,
 * then falls back to the DESFire protocol if no known application is found.
 */
object ISO7816Dispatcher {
    suspend fun readCard(
        tagId: ByteArray,
        transceiver: CardTransceiver,
    ): RawCard<*> {
        val iso7816Card = tryISO7816(tagId, transceiver)
        if (iso7816Card != null) {
            return iso7816Card
        }
        return DesfireCardReader.readCard(tagId, transceiver)
    }

    private suspend fun tryISO7816(
        tagId: ByteArray,
        transceiver: CardTransceiver,
    ): RawCard<*>? {
        val appConfigs = buildAppConfigs()
        if (appConfigs.isEmpty()) return null

        return try {
            ISO7816CardReader.readCard(tagId, transceiver, appConfigs)
        } catch (e: Exception) {
            println("[ISO7816Dispatcher] ISO7816 read attempt failed: $e")
            null
        }
    }

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
                    fileSelectors = buildChinaFileSelectors(),
                ),
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
                fileSelectors = buildKSX6924FileSelectors(),
            ),
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
        val selectors = mutableListOf<ISO7816CardReader.FileSelector>()

        // Files 1-5
        for (fileId in 1..5) {
            selectors.add(
                ISO7816CardReader.FileSelector(
                    parentDf = null,
                    fileId = fileId,
                ),
            )
        }

        // File 0xdf00 (T-Money cards)
        // This file may not exist on all cards, but ISO7816CardReader handles failures gracefully
        selectors.add(
            ISO7816CardReader.FileSelector(
                parentDf = null,
                fileId = 0xdf00,
            ),
        )

        return selectors
    }
}
