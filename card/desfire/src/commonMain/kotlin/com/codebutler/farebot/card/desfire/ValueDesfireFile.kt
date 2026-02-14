/*
 * ValueDesfireFile.kt
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

import com.codebutler.farebot.base.util.ByteUtils
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Represents a value file in Desfire
 */
@Serializable
data class ValueDesfireFile(
    override val id: Int,
    @Contextual override val fileSettings: DesfireFileSettings,
    val value: Int,
) : DesfireFile {
    companion object {
        fun create(
            fileId: Int,
            fileSettings: DesfireFileSettings,
            fileData: ByteArray,
        ): ValueDesfireFile {
            val myData = fileData.copyOf()
            myData.reverse()
            val value = ByteUtils.byteArrayToInt(myData)
            return ValueDesfireFile(fileId, fileSettings, value)
        }
    }
}
