/*
 * SampleCard.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.app.core.sample

import android.content.Context
import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.util.ByteArray
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import java.util.Date

class SampleCard(val rawCard: RawSampleCard) : Card() {

    override fun getCardType(): CardType = rawCard.cardType()

    override fun getTagId(): ByteArray = rawCard.tagId()

    override fun getScannedAt(): Date = rawCard.scannedAt()

    override fun getAdvancedUi(context: Context?): FareBotUiTree {
        val uiBuilder = FareBotUiTree.builder(context)

        val section1Builder = uiBuilder.item().title("Sample Transit Section 1")

        section1Builder.item()
                .title("Example Item 1")
                .value("Value")

        section1Builder.item()
                .title("Example Item 2")
                .value("Value")

        val section2Builder = uiBuilder.item().title("Sample Transit Section 1")

        section2Builder.item()
                .title("Example Item 1")
                .value("Value")

        return uiBuilder.build()
    }
}
