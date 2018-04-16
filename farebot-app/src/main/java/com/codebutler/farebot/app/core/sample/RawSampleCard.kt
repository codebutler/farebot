/*
 * RawSampleCard.kt
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

import com.codebutler.farebot.base.util.ByteArray
import com.codebutler.farebot.transit.registry.annotations.CardType
import com.codebutler.farebot.card.RawCard

import java.util.Date

class RawSampleCard : RawCard<SampleCard> {

    override fun cardType(): CardType = CardType.Sample

    override fun tagId(): ByteArray = ByteArray.create(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0))

    override fun scannedAt(): Date = Date()

    override fun isUnauthorized(): Boolean = false

    override fun parse(): SampleCard = SampleCard(this)
}
