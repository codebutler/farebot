/*
 * IntercodeLookupTisseo.kt
 *
 * Copyright 2018 Google
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

import com.codebutler.farebot.base.util.getStringBlocking
import farebot.farebot_transit_calypso.generated.resources.*
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

internal object IntercodeLookupTisseo : IntercodeLookupSTR("tisseo"), IntercodeLookupSingle {
    // https://www.tisseo.fr/les-tarifs/obtenir-une-carte-pastel
    override val cardName: String = getStringBlocking(Res.string.card_name_pastel)

    @Suppress("unused")
    private const val AGENCY_TISSEO = 1

    override val subscriptionMap: Map<Int, ComposeStringResource> =
        mapOf(
            300 to Res.string.tisseo_10_tickets,
            307 to Res.string.tisseo_1_ticket,
            335 to Res.string.tisseo_mensuel,
            336 to Res.string.tisseo_mensuel,
            455 to Res.string.tisseo_annuel,
            672 to Res.string.tisseo_annuel_26,
            674 to Res.string.tisseo_mensuel_26,
            676 to Res.string.tisseo_10_tickets_26,
            950 to Res.string.tisseo_velo,
        )
}
