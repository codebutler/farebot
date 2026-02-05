/*
 * OctopusTransitInfo.kt
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Portions based on FelicaCard.java from nfcard project
 * Copyright 2013 Sinpo Wei <sinpowei@gmail.com>
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

package com.codebutler.farebot.transit.octopus

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_octopus.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Reader for Octopus (Hong Kong)
 * https://github.com/micolous/metrodroid/wiki/Octopus
 */
class OctopusTransitInfo(
    private val octopusBalance: Int?,
    private val shenzhenBalance: Int?
) : TransitInfo() {

    private val hasOctopus: Boolean get() = octopusBalance != null
    private val hasShenzhen: Boolean get() = shenzhenBalance != null

    companion object {
        const val OCTOPUS_NAME = "Octopus"
        const val SZT_NAME = "Shenzhen Tong"
        const val DUAL_NAME = "Hu Tong Xing"
        private const val TAG = "OctopusTransitInfo"

        fun create(
            octopusBalance: Int?,
            shenzhenBalance: Int?,
            @Suppress("UNUSED_PARAMETER") hasOctopus: Boolean,
            @Suppress("UNUSED_PARAMETER") hasShenzen: Boolean
        ): OctopusTransitInfo {
            return OctopusTransitInfo(octopusBalance, shenzhenBalance)
        }
    }

    override val balance: TransitBalance?
        get() {
            // Octopus balance takes priority 1
            if (octopusBalance != null) {
                return TransitBalance(balance = TransitCurrency.HKD(octopusBalance))
            }
            // Shenzhen Tong balance takes priority 2
            if (shenzhenBalance != null) {
                return TransitBalance(balance = TransitCurrency.CNY(shenzhenBalance))
            }
            // Unhandled.
            return null
        }

    override val serialNumber: String?
        get() {
            // TODO: Find out where this is on the card.
            return null
        }

    override val cardName: String
        get() = runBlocking {
            if (hasShenzhen) {
                if (hasOctopus) {
                    getString(Res.string.octopus_dual_card_name)
                } else {
                    getString(Res.string.octopus_szt_card_name)
                }
            } else {
                getString(Res.string.octopus_card_name)
            }
        }

    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree? {
        // Dual-mode card, show the CNY balance here.
        val szt = shenzhenBalance
        if (hasOctopus && szt != null) {
            val uiBuilder = FareBotUiTree.builder(stringResource)
            val apbUiBuilder = uiBuilder.item()
                .title(Res.string.octopus_alternate_purse_balances)
            apbUiBuilder.item(
                Res.string.octopus_szt,
                TransitCurrency.CNY(szt).formatCurrencyString(isBalance = true)
            )
            return uiBuilder.build()
        }
        return null
    }
}
