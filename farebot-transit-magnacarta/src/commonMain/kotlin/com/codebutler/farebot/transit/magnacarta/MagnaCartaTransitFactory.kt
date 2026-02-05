/*
 * MagnaCartaTransitFactory.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.magnacarta

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import farebot.farebot_transit_magnacarta.generated.resources.Res
import farebot.farebot_transit_magnacarta.generated.resources.magnacarta_card_name
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class MagnaCartaTransitFactory : TransitFactory<DesfireCard, MagnaCartaTransitInfo> {

    override fun check(card: DesfireCard): Boolean {
        return card.getApplication(MagnaCartaTransitInfo.APP_ID_BALANCE) != null
    }

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        return TransitIdentity.create(runBlocking { getString(Res.string.magnacarta_card_name) }, null)
    }

    override fun parseInfo(card: DesfireCard): MagnaCartaTransitInfo {
        val file2 = card.getApplication(MagnaCartaTransitInfo.APP_ID_BALANCE)
            ?.getFile(2) as? StandardDesfireFile
        val balance = file2?.data?.byteArrayToInt(6, 2)
        return MagnaCartaTransitInfo(mBalance = balance)
    }
}
