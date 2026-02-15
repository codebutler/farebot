/*
 * IntercodeLookupOura.kt
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

import farebot.transit.calypso.generated.resources.Res
import farebot.transit.calypso.generated.resources.card_name_oura
import farebot.transit.calypso.generated.resources.oura_billet_tarif_normal
import org.jetbrains.compose.resources.StringResource as ComposeStringResource
import com.codebutler.farebot.base.util.FormattedString

internal object IntercodeLookupOura : IntercodeLookupSTR("oura"), IntercodeLookupSingle {
    override val cardName: FormattedString = FormattedString(Res.string.card_name_oura)

    override val subscriptionMapByAgency: Map<Pair<Int?, Int>, ComposeStringResource> =
        mapOf(
            Pair(2, 0x6601) to Res.string.oura_billet_tarif_normal,
        )
}
