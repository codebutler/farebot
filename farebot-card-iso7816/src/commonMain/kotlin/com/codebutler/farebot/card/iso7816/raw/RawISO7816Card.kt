/*
 * RawISO7816Card.kt
 *
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.card.iso7816.raw

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.iso7816.ISO7816Application
import com.codebutler.farebot.card.iso7816.ISO7816Card
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class RawISO7816Card(
    @Contextual private val tagId: ByteArray,
    private val scannedAt: Instant,
    val applications: List<ISO7816Application>,
) : RawCard<ISO7816Card> {
    override fun cardType(): CardType = CardType.ISO7816

    override fun tagId(): ByteArray = tagId

    override fun scannedAt(): Instant = scannedAt

    override fun isUnauthorized(): Boolean = false

    override fun parse(): ISO7816Card = ISO7816Card.create(tagId, scannedAt, applications)

    companion object {
        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            applications: List<ISO7816Application>,
        ): RawISO7816Card = RawISO7816Card(tagId, scannedAt, applications)
    }
}
