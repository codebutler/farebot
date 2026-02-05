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

import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.util.DateFormatStyle
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.formatDate
import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.codebutler.farebot.base.util.CurrencyFormatter

@Serializable
data class CEPASCard(
    @Contextual override val tagId: ByteArray,
    override val scannedAt: Instant,
    val purses: List<CEPASPurse>,
    val histories: List<CEPASHistory>
) : Card() {

    override val cardType: CardType = CardType.CEPAS

    fun getPurse(purse: Int): CEPASPurse? = purses[purse]

    fun getHistory(purse: Int): CEPASHistory? = histories[purse]

    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree {
        val cardUiBuilder = FareBotUiTree.builder(stringResource)

        val pursesUiBuilder = cardUiBuilder.item().title("Purses")
        for (purse in purses) {
            val purseUiBuilder = pursesUiBuilder.item()
                .title("Purse ID ${purse.id}")
            purseUiBuilder.item().title("CEPAS Version").value(purse.cepasVersion)
            purseUiBuilder.item().title("Purse Status").value(purse.purseStatus)
            purseUiBuilder.item().title("Purse Balance")
                .value(CurrencyFormatter.formatValue(purse.purseBalance / 100.0, "SGD"))
            purseUiBuilder.item().title("Purse Creation Date")
                .value(formatDate(Instant.fromEpochMilliseconds(purse.purseCreationDate * 1000L), DateFormatStyle.LONG))
            purseUiBuilder.item().title("Purse Expiry Date")
                .value(formatDate(Instant.fromEpochMilliseconds(purse.purseExpiryDate * 1000L), DateFormatStyle.LONG))
            purseUiBuilder.item().title("Autoload Amount").value(purse.autoLoadAmount)
            purseUiBuilder.item().title("CAN").value(purse.can)
            purseUiBuilder.item().title("CSN").value(purse.csn)

            val transactionUiBuilder = cardUiBuilder.item().title("Last Transaction Information")
            transactionUiBuilder.item().title("TRP").value(purse.lastTransactionTRP)
            transactionUiBuilder.item().title("Credit TRP").value(purse.lastCreditTransactionTRP)
            transactionUiBuilder.item().title("Credit Header").value(purse.lastCreditTransactionHeader)
            transactionUiBuilder.item().title("Debit Options").value(purse.lastTransactionDebitOptionsByte)

            val otherUiBuilder = cardUiBuilder.item().title("Other Purse Information")
            otherUiBuilder.item().title("Logfile Record Count").value(purse.logfileRecordCount)
            otherUiBuilder.item().title("Issuer Data Length").value(purse.issuerDataLength)
            otherUiBuilder.item().title("Issuer-specific Data").value(purse.issuerSpecificData)
        }

        return cardUiBuilder.build()
    }

    companion object {
        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            purses: List<CEPASPurse>,
            histories: List<CEPASHistory>
        ): CEPASCard {
            return CEPASCard(tagId, scannedAt, purses, histories)
        }
    }
}
