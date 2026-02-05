/*
 * ValueDesfireFileSettings.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.card.desfire

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Contains FileSettings for Value file types.
 * See GetFileSettings for schemadata.
 */
@Serializable
data class ValueDesfireFileSettings(
    override val fileType: Byte,
    override val commSetting: Byte,
    @Contextual override val accessRights: ByteArray,
    val lowerLimit: Int,
    val upperLimit: Int,
    val limitedCreditValue: Int,
    val limitedCreditEnabled: Boolean
) : DesfireFileSettings() {

    companion object {
        fun create(
            fileType: Byte,
            commSetting: Byte,
            accessRights: ByteArray,
            lowerLimit: Int,
            upperLimit: Int,
            limitedCreditValue: Int,
            limitedCreditEnabled: Boolean
        ): ValueDesfireFileSettings = ValueDesfireFileSettings(
            fileType,
            commSetting,
            accessRights,
            lowerLimit,
            upperLimit,
            limitedCreditValue,
            limitedCreditEnabled
        )
    }
}
