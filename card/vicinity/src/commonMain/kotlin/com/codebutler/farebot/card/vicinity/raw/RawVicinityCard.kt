/*
 * RawVicinityCard.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.card.vicinity.raw

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.vicinity.VicinityCard
import com.codebutler.farebot.card.vicinity.VicinityPage
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class RawVicinityCard(
    @Contextual private val _tagId: ByteArray,
    private val _scannedAt: Instant,
    val pages: List<VicinityPage>,
    @Contextual val sysInfo: ByteArray? = null,
    val isPartialRead: Boolean = false,
) : RawCard<VicinityCard> {
    override fun cardType(): CardType = CardType.Vicinity

    override fun tagId(): ByteArray = _tagId

    override fun scannedAt(): Instant = _scannedAt

    override fun isUnauthorized(): Boolean = false

    override fun parse(): VicinityCard = VicinityCard.create(tagId(), scannedAt(), pages, sysInfo, isPartialRead)

    companion object {
        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            pages: List<VicinityPage>,
            sysInfo: ByteArray? = null,
            isPartialRead: Boolean = false,
        ): RawVicinityCard = RawVicinityCard(tagId, scannedAt, pages, sysInfo, isPartialRead)
    }
}
