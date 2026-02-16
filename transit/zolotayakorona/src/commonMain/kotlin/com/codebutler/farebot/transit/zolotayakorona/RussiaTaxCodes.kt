/*
 * RussiaTaxCodes.kt
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

package com.codebutler.farebot.transit.zolotayakorona

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.NumberUtils
import farebot.transit.zolotayakorona.generated.resources.*
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource

/**
 * Tax codes assigned by Russian Tax agency for places both inside Russia and outside (e.g. Baikonur).
 * Used by Zolotaya Korona and Umarsh systems.
 */
object RussiaTaxCodes {
    @Suppress("FunctionName")
    fun BCDToTimeZone(bcd: Int): TimeZone = TAX_CODE_TIMEZONES[bcd] ?: TimeZone.of("Europe/Moscow")

    fun codeToName(regionNum: Int): FormattedString {
        val bcd = NumberUtils.intToBCD(regionNum)
        val nameRes = TAX_CODE_NAMES[bcd]
        return if (nameRes != null) {
            FormattedString(nameRes)
        } else {
            FormattedString(Res.string.russia_region_unknown, regionNum.toString())
        }
    }

    fun codeToTimeZone(regionNum: Int): TimeZone =
        TAX_CODE_TIMEZONES[NumberUtils.intToBCD(regionNum)] ?: TimeZone.of("Europe/Moscow")

    @Suppress("FunctionName")
    fun BCDToName(regionNum: Int): FormattedString {
        val nameRes = TAX_CODE_NAMES[regionNum]
        return if (nameRes != null) {
            FormattedString(nameRes)
        } else {
            FormattedString(Res.string.russia_region_unknown, regionNum.toString(16))
        }
    }

    private val TAX_CODE_NAMES =
        mapOf<Int, StringResource>(
            0x04 to Res.string.russia_region_04,
            0x11 to Res.string.russia_region_11,
            0x12 to Res.string.russia_region_12,
            0x18 to Res.string.russia_region_18,
            0x22 to Res.string.russia_region_22,
            0x23 to Res.string.russia_region_23,
            0x25 to Res.string.russia_region_25,
            0x27 to Res.string.russia_region_27,
            0x28 to Res.string.russia_region_28,
            0x29 to Res.string.russia_region_29,
            0x33 to Res.string.russia_region_33,
            0x41 to Res.string.russia_region_41,
            0x42 to Res.string.russia_region_42,
            0x43 to Res.string.russia_region_43,
            0x45 to Res.string.russia_region_45,
            0x52 to Res.string.russia_region_52,
            0x53 to Res.string.russia_region_53,
            0x54 to Res.string.russia_region_54,
            0x55 to Res.string.russia_region_55,
            0x56 to Res.string.russia_region_56,
            0x58 to Res.string.russia_region_58,
            0x60 to Res.string.russia_region_60,
            0x63 to Res.string.russia_region_63,
            0x65 to Res.string.russia_region_65,
            0x66 to Res.string.russia_region_66,
            0x74 to Res.string.russia_region_74,
            0x76 to Res.string.russia_region_76,
            0x79 to Res.string.russia_region_79,
            0x91 to Res.string.russia_region_91,
        )

    private val TAX_CODE_TIMEZONES =
        mapOf(
            0x04 to TimeZone.of("Asia/Krasnoyarsk"),
            0x11 to TimeZone.of("Europe/Kirov"),
            0x12 to TimeZone.of("Europe/Moscow"),
            0x18 to TimeZone.of("Europe/Samara"),
            0x22 to TimeZone.of("Asia/Krasnoyarsk"),
            0x23 to TimeZone.of("Europe/Moscow"),
            0x25 to TimeZone.of("Asia/Vladivostok"),
            0x27 to TimeZone.of("Asia/Vladivostok"),
            0x28 to TimeZone.of("Asia/Yakutsk"),
            0x29 to TimeZone.of("Europe/Moscow"),
            0x33 to TimeZone.of("Europe/Moscow"),
            0x41 to TimeZone.of("Asia/Kamchatka"),
            0x42 to TimeZone.of("Asia/Novokuznetsk"),
            0x43 to TimeZone.of("Europe/Kirov"),
            0x45 to TimeZone.of("Asia/Yekaterinburg"),
            0x52 to TimeZone.of("Europe/Moscow"),
            0x53 to TimeZone.of("Europe/Moscow"),
            0x54 to TimeZone.of("Asia/Novosibirsk"),
            0x55 to TimeZone.of("Asia/Omsk"),
            0x56 to TimeZone.of("Asia/Yekaterinburg"),
            0x58 to TimeZone.of("Europe/Moscow"),
            0x60 to TimeZone.of("Europe/Moscow"),
            0x63 to TimeZone.of("Europe/Samara"),
            0x65 to TimeZone.of("Asia/Sakhalin"),
            0x66 to TimeZone.of("Asia/Yekaterinburg"),
            0x74 to TimeZone.of("Asia/Yekaterinburg"),
            0x76 to TimeZone.of("Europe/Moscow"),
            0x79 to TimeZone.of("Asia/Vladivostok"),
            0x91 to TimeZone.of("Europe/Simferopol"),
        )
}
