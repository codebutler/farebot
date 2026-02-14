/*
 * IntercodeLookupPassPass.kt
 *
 * Copyright 2023 by 'Altonss'
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

package com.codebutler.farebot.transit.calypso.intercode

import com.codebutler.farebot.base.mdst.MdstStationTableReader
import com.codebutler.farebot.base.util.getStringBlocking
import farebot.transit.calypso.generated.resources.*
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

internal object IntercodeLookupPassPass : IntercodeLookupSTR("passpass"), IntercodeLookupSingle {
    override val cardName: String = getStringBlocking(Res.string.card_name_passpass)

    override val subscriptionMap: Map<Int, ComposeStringResource> =
        mapOf(
            24577 to Res.string.ilevia_trajet_unitaire,
            24578 to Res.string.ilevia_trajet_unitaire_x10,
            25738 to Res.string.ilevia_mensuel,
            25743 to Res.string.ilevia_10mois,
        )

    override fun getRouteName(
        routeNumber: Int?,
        routeVariant: Int?,
        agency: Int?,
        transport: Int?,
    ): String? {
        if (agency == ILEVIA && routeNumber != null) {
            val reader = MdstStationTableReader.getReader("passpass")
            if (reader != null) {
                val line = reader.getLine(routeNumber)
                if (line?.name?.english != null) return line.name.english
            }
        }
        return super.getRouteName(routeNumber, routeNumber, agency, transport)
    }

    private const val ILEVIA = 23
}
