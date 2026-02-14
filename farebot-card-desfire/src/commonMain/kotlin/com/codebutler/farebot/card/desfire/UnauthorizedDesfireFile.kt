/*
 * UnauthorizedDesfireFile.kt
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
 * Represents a DESFire file which could not be read due to
 * access control limits.
 */
@Serializable
data class UnauthorizedDesfireFile(
    override val id: Int,
    @Contextual override val fileSettings: DesfireFileSettings,
    val errorMessage: String,
) : DesfireFile {
    companion object {
        fun create(
            fileId: Int,
            settings: DesfireFileSettings,
            errorMessage: String,
        ): UnauthorizedDesfireFile = UnauthorizedDesfireFile(fileId, settings, errorMessage)
    }
}
