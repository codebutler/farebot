/*
 * RawDesfireCard.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.desfire.raw

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.desfire.DesfireCard
import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class RawDesfireCard(
    @Contextual private val tagId: ByteArray,
    private val scannedAt: Instant,
    val applications: List<RawDesfireApplication>,
    val manufacturingData: RawDesfireManufacturingData,
    val appListLocked: Boolean = false
) : RawCard<DesfireCard> {

    override fun cardType(): CardType = CardType.MifareDesfire

    override fun tagId(): ByteArray = tagId

    override fun scannedAt(): Instant = scannedAt

    fun applications(): List<RawDesfireApplication> = applications
    fun manufacturingData(): RawDesfireManufacturingData = manufacturingData

    override fun isUnauthorized(): Boolean {
        for (application in applications) {
            for (file in application.files) {
                val error = file.error
                if (error == null || error.type != RawDesfireFile.Error.TYPE_UNAUTHORIZED) {
                    return false
                }
            }
        }
        return true
    }

    override fun parse(): DesfireCard {
        val parsedApplications = applications.map { it.parse() }
        return DesfireCard.create(tagId, scannedAt, parsedApplications, manufacturingData.parse(), appListLocked)
    }

    companion object {
        fun create(
            tagId: ByteArray,
            date: Instant,
            apps: List<RawDesfireApplication>,
            manufData: RawDesfireManufacturingData
        ): RawDesfireCard = RawDesfireCard(tagId, date, apps, manufData)
    }
}
