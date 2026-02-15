/*
 * TMoneyPurseInfoResolver.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
 *
 * References: https://github.com/micolous/metrodroid/wiki/T-Money
 */

package com.codebutler.farebot.transit.tmoney

import com.codebutler.farebot.card.ksx6924.KSX6924PurseInfoResolver
import farebot.transit.tmoney.generated.resources.Res
import farebot.transit.tmoney.generated.resources.none
import farebot.transit.tmoney.generated.resources.tmoney_ccode_bc
import farebot.transit.tmoney.generated.resources.tmoney_ccode_citi
import farebot.transit.tmoney.generated.resources.tmoney_ccode_exchange
import farebot.transit.tmoney.generated.resources.tmoney_ccode_hana_sk
import farebot.transit.tmoney.generated.resources.tmoney_ccode_hyundai
import farebot.transit.tmoney.generated.resources.tmoney_ccode_kb
import farebot.transit.tmoney.generated.resources.tmoney_ccode_lotte
import farebot.transit.tmoney.generated.resources.tmoney_ccode_nonghyup
import farebot.transit.tmoney.generated.resources.tmoney_ccode_samsung
import farebot.transit.tmoney.generated.resources.tmoney_ccode_shinhan
import farebot.transit.tmoney.generated.resources.tmoney_ccode_woori
import farebot.transit.tmoney.generated.resources.tmoney_disrate_disabled_basic
import farebot.transit.tmoney.generated.resources.tmoney_disrate_disabled_companion
import farebot.transit.tmoney.generated.resources.tmoney_disrate_none
import farebot.transit.tmoney.generated.resources.tmoney_disrate_veteran_basic
import farebot.transit.tmoney.generated.resources.tmoney_disrate_veteran_companion
import farebot.transit.tmoney.generated.resources.tmoney_issuer_cardnet
import farebot.transit.tmoney.generated.resources.tmoney_issuer_eb
import farebot.transit.tmoney.generated.resources.tmoney_issuer_kec
import farebot.transit.tmoney.generated.resources.tmoney_issuer_kftci
import farebot.transit.tmoney.generated.resources.tmoney_issuer_korail
import farebot.transit.tmoney.generated.resources.tmoney_issuer_kscc
import farebot.transit.tmoney.generated.resources.tmoney_issuer_mondex
import farebot.transit.tmoney.generated.resources.tmoney_issuer_mybi
import farebot.transit.tmoney.generated.resources.tmoney_issuer_seoul_bus
import farebot.transit.tmoney.generated.resources.tmoney_tcode_kt
import farebot.transit.tmoney.generated.resources.tmoney_tcode_lg
import farebot.transit.tmoney.generated.resources.tmoney_tcode_sk
import farebot.transit.tmoney.generated.resources.tmoney_usercode_bus
import farebot.transit.tmoney.generated.resources.tmoney_usercode_child
import farebot.transit.tmoney.generated.resources.tmoney_usercode_disabled
import farebot.transit.tmoney.generated.resources.tmoney_usercode_inactive
import farebot.transit.tmoney.generated.resources.tmoney_usercode_lorry
import farebot.transit.tmoney.generated.resources.tmoney_usercode_regular
import farebot.transit.tmoney.generated.resources.tmoney_usercode_senior
import farebot.transit.tmoney.generated.resources.tmoney_usercode_test
import farebot.transit.tmoney.generated.resources.tmoney_usercode_youth
import com.codebutler.farebot.base.util.FormattedString

/**
 * [KSX6924PurseInfoResolver] singleton for T-Money.
 *
 * This contains mapping for IDs on a T-Money card.
 *
 * See https://github.com/micolous/metrodroid/wiki/T-Money for more information.
 */
object TMoneyPurseInfoResolver : KSX6924PurseInfoResolver() {
    override val issuers: Map<Int, FormattedString> by lazy {
        mapOf(
            // 0x00: reserved
            0x01 to FormattedString(Res.string.tmoney_issuer_kftci),
            // 0x02: A-CASH (에이캐시) (Also used by Snapper)
            0x03 to FormattedString(Res.string.tmoney_issuer_mybi),
            // 0x04: reserved
            // 0x05: V-Cash (브이캐시)
            0x06 to FormattedString(Res.string.tmoney_issuer_mondex),
            0x07 to FormattedString(Res.string.tmoney_issuer_kec),
            0x08 to FormattedString(Res.string.tmoney_issuer_kscc),
            0x09 to FormattedString(Res.string.tmoney_issuer_korail),
            // 0x0a: reserved
            0x0b to FormattedString(Res.string.tmoney_issuer_eb),
            0x0c to FormattedString(Res.string.tmoney_issuer_seoul_bus),
            0x0d to FormattedString(Res.string.tmoney_issuer_cardnet),
        )
    }

    override val userCodes: Map<Int, FormattedString> by lazy {
        mapOf(
            0x01 to FormattedString(Res.string.tmoney_usercode_regular),
            0x02 to FormattedString(Res.string.tmoney_usercode_child),
            // TTAK.KO 12.0240 disagrees
            0x03 to FormattedString(Res.string.tmoney_usercode_youth),
            // TTAK.KO 12.0240 disagrees
            0x04 to FormattedString(Res.string.tmoney_usercode_senior),
            // TTAK.KO 12.0240 disagrees
            0x05 to FormattedString(Res.string.tmoney_usercode_disabled),
            // Only in TTAK.KO 12.0240
            0x0f to FormattedString(Res.string.tmoney_usercode_test),
            0x11 to FormattedString(Res.string.tmoney_usercode_bus),
            0x12 to FormattedString(Res.string.tmoney_usercode_lorry),
            0xff to FormattedString(Res.string.tmoney_usercode_inactive),
        )
    }

    override val disRates: Map<Int, FormattedString> by lazy {
        mapOf(
            0x00 to FormattedString(Res.string.tmoney_disrate_none),
            0x10 to FormattedString(Res.string.tmoney_disrate_disabled_basic),
            0x11 to FormattedString(Res.string.tmoney_disrate_disabled_companion),
            // 0x12 - 0x1f: reserved
            0x20 to FormattedString(Res.string.tmoney_disrate_veteran_basic),
            0x21 to FormattedString(Res.string.tmoney_disrate_veteran_companion),
            // 0x22 - 0x2f: reserved
        )
    }

    override val tCodes: Map<Int, FormattedString> by lazy {
        mapOf(
            0x00 to FormattedString(Res.string.none),
            0x01 to FormattedString(Res.string.tmoney_tcode_sk),
            0x02 to FormattedString(Res.string.tmoney_tcode_kt),
            0x03 to FormattedString(Res.string.tmoney_tcode_lg),
        )
    }

    override val cCodes: Map<Int, FormattedString> by lazy {
        mapOf(
            0x00 to FormattedString(Res.string.none),
            0x01 to FormattedString(Res.string.tmoney_ccode_kb),
            0x02 to FormattedString(Res.string.tmoney_ccode_nonghyup),
            0x03 to FormattedString(Res.string.tmoney_ccode_lotte),
            0x04 to FormattedString(Res.string.tmoney_ccode_bc),
            0x05 to FormattedString(Res.string.tmoney_ccode_samsung),
            0x06 to FormattedString(Res.string.tmoney_ccode_shinhan),
            0x07 to FormattedString(Res.string.tmoney_ccode_citi),
            0x08 to FormattedString(Res.string.tmoney_ccode_exchange),
            0x09 to FormattedString(Res.string.tmoney_ccode_woori),
            0x0a to FormattedString(Res.string.tmoney_ccode_hana_sk),
            0x0b to FormattedString(Res.string.tmoney_ccode_hyundai),
        )
    }
}
