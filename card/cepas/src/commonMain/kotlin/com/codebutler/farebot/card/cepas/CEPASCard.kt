/*
 * CEPASCard.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
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

package com.codebutler.farebot.card.cepas

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.ui.uiTree
import com.codebutler.farebot.base.util.CurrencyFormatter
import com.codebutler.farebot.base.util.DateFormatStyle
import com.codebutler.farebot.base.util.formatDate
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class CEPASCard(
    @Contextual override val tagId: ByteArray,
    override val scannedAt: Instant,
    val purses: List<CEPASPurse>,
    val histories: List<CEPASHistory>,
) : Card() {
    override val cardType: CardType = CardType.CEPAS

    fun getPurse(purse: Int): CEPASPurse? = purses[purse]

    fun getHistory(purse: Int): CEPASHistory? = histories[purse]

    override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
        item {
            title = "Purses"
            for (purse in purses) {
                item {
                    title = "Purse ID ${purse.id}"
                    item { title = "CEPAS Version"; value = purse.cepasVersion }
                    item { title = "Purse Status"; value = purse.purseStatus }
                    item {
                        title = "Purse Balance"
                        value = CurrencyFormatter.formatValue(purse.purseBalance / 100.0, "SGD")
                    }
                    item {
                        title = "Purse Creation Date"
                        value = formatDate(Instant.fromEpochMilliseconds(purse.purseCreationDate * 1000L), DateFormatStyle.LONG)
                    }
                    item {
                        title = "Purse Expiry Date"
                        value = formatDate(Instant.fromEpochMilliseconds(purse.purseExpiryDate * 1000L), DateFormatStyle.LONG)
                    }
                    item { title = "Autoload Amount"; value = purse.autoLoadAmount }
                    item { title = "CAN"; value = purse.can }
                    item { title = "CSN"; value = purse.csn }
                }
            }
        }
        for (purse in purses) {
            item {
                title = "Last Transaction Information"
                item { title = "TRP"; value = purse.lastTransactionTRP }
                item { title = "Credit TRP"; value = purse.lastCreditTransactionTRP }
                item { title = "Credit Header"; value = purse.lastCreditTransactionHeader }
                item { title = "Debit Options"; value = purse.lastTransactionDebitOptionsByte }
            }
            item {
                title = "Other Purse Information"
                item { title = "Logfile Record Count"; value = purse.logfileRecordCount }
                item { title = "Issuer Data Length"; value = purse.issuerDataLength }
                item { title = "Issuer-specific Data"; value = purse.issuerSpecificData }
            }
        }
    }

    companion object {
        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            purses: List<CEPASPurse>,
            histories: List<CEPASHistory>,
        ): CEPASCard = CEPASCard(tagId, scannedAt, purses, histories)
    }
}
