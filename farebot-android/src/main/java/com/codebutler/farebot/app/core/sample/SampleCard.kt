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

import com.codebutler.farebot.base.ui.uiTree
import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import kotlin.time.Instant

class SampleCard(private val rawCard: RawSampleCard) : Card() {

    override val cardType: CardType = rawCard.cardType()

    override val tagId: ByteArray = rawCard.tagId()

    override val scannedAt: Instant = rawCard.scannedAt()

    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree = uiTree(stringResource) {
        item {
            title = "Sample Transit Section 1"
            item {
                title = "Example Item 1"
                value = "Value 1"
            }
            item {
                title = "Example Item 2"
                value = "Value 2"
            }
        }
        item {
            title = "Sample Transit Section 2"
            for (i in 1..10) {
                item {
                    title = "Example Item $i"
                    value = "Value $i"
                }
            }
        }
    }
}
