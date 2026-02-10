/*
 * TransitRegion.kt
 *
 * Copyright 2019 Google
 * Copyright 2024 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit

sealed class TransitRegion {
    abstract val translatedName: String

    open val flagEmoji: String? get() = null

    open fun sortingKey(deviceRegion: String? = null): Pair<Int, String> =
        Pair(SECTION_MAIN, translatedName)

    data class Iso(val code: String) : TransitRegion() {
        override val translatedName: String
            get() = iso3166AlphaToName(code) ?: code

        override val flagEmoji: String
            get() = code.uppercase().map { char ->
                val codePoint = 0x1F1E6 - 'A'.code + char.code
                // Regional indicator symbols are supplementary characters (above U+FFFF),
                // encode as a UTF-16 surrogate pair.
                val high = ((codePoint - 0x10000) shr 10) + 0xD800
                val low = ((codePoint - 0x10000) and 0x3FF) + 0xDC00
                charArrayOf(high.toChar(), low.toChar()).concatToString()
            }.joinToString("")

        override fun sortingKey(deviceRegion: String?): Pair<Int, String> {
            val section = if (deviceRegion != null && code.equals(deviceRegion, ignoreCase = true)) {
                SECTION_NEARBY
            } else {
                SECTION_MAIN
            }
            return Pair(section, translatedName)
        }
    }

    data object Crimea : TransitRegion() {
        override val translatedName: String
            get() = "Crimea"

        override fun sortingKey(deviceRegion: String?): Pair<Int, String> {
            val section = if (deviceRegion != null &&
                (deviceRegion.equals("RU", ignoreCase = true) || deviceRegion.equals("UA", ignoreCase = true))
            ) {
                SECTION_NEARBY
            } else {
                SECTION_MAIN
            }
            return Pair(section, translatedName)
        }
    }

    data class Named(
        val name: String,
        val section: Int = SECTION_MAIN
    ) : TransitRegion() {
        override val translatedName: String
            get() = name

        override fun sortingKey(deviceRegion: String?): Pair<Int, String> =
            Pair(section, translatedName)
    }

    class DeviceRegionComparator(private val deviceRegion: String?) : Comparator<TransitRegion> {
        override fun compare(a: TransitRegion, b: TransitRegion): Int {
            val ak = a.sortingKey(deviceRegion)
            val bk = b.sortingKey(deviceRegion)
            if (ak.first != bk.first) {
                return ak.first.compareTo(bk.first)
            }
            return ak.second.compareTo(bk.second, ignoreCase = true)
        }
    }

    object RegionComparator : Comparator<TransitRegion> {
        override fun compare(a: TransitRegion, b: TransitRegion): Int {
            val ak = a.sortingKey()
            val bk = b.sortingKey()
            if (ak.first != bk.first) {
                return ak.first.compareTo(bk.first)
            }
            return ak.second.compareTo(bk.second, ignoreCase = true)
        }
    }

    companion object {
        const val SECTION_NEARBY = -2
        const val SECTION_WORLDWIDE = -1
        const val SECTION_MAIN = 0

        val AUSTRALIA = Iso("AU")
        val BELGIUM = Iso("BE")
        val BRAZIL = Iso("BR")
        val CANADA = Iso("CA")
        val CHILE = Iso("CL")
        val CHINA = Iso("CN")
        val DENMARK = Iso("DK")
        val ESTONIA = Iso("EE")
        val FINLAND = Iso("FI")
        val FRANCE = Iso("FR")
        val GEORGIA = Iso("GE")
        val GERMANY = Iso("DE")
        val HONG_KONG = Iso("HK")
        val INDONESIA = Iso("ID")
        val IRELAND = Iso("IE")
        val ISRAEL = Iso("IL")
        val ITALY = Iso("IT")
        val JAPAN = Iso("JP")
        val MALAYSIA = Iso("MY")
        val NETHERLANDS = Iso("NL")
        val NEW_ZEALAND = Iso("NZ")
        val POLAND = Iso("PL")
        val PORTUGAL = Iso("PT")
        val QATAR = Iso("QA")
        val RUSSIA = Iso("RU")
        val SINGAPORE = Iso("SG")
        val SOUTH_AFRICA = Iso("ZA")
        val SOUTH_KOREA = Iso("KR")
        val SPAIN = Iso("ES")
        val SWEDEN = Iso("SE")
        val SWITZERLAND = Iso("CH")
        val TAIWAN = Iso("TW")
        val TURKEY = Iso("TR")
        val UAE = Iso("AE")
        val UK = Iso("GB")
        val UKRAINE = Iso("UA")
        val USA = Iso("US")
        val WORLDWIDE = Named("Worldwide", SECTION_WORLDWIDE)
    }
}

private fun iso3166AlphaToName(code: String): String? = when (code.uppercase()) {
    "AE" -> "United Arab Emirates"
    "AU" -> "Australia"
    "BE" -> "Belgium"
    "BR" -> "Brazil"
    "CA" -> "Canada"
    "CH" -> "Switzerland"
    "CL" -> "Chile"
    "CN" -> "China"
    "DE" -> "Germany"
    "DK" -> "Denmark"
    "EE" -> "Estonia"
    "ES" -> "Spain"
    "FI" -> "Finland"
    "FR" -> "France"
    "GB" -> "United Kingdom"
    "GE" -> "Georgia"
    "HK" -> "Hong Kong"
    "ID" -> "Indonesia"
    "IE" -> "Ireland"
    "IL" -> "Israel"
    "IT" -> "Italy"
    "JP" -> "Japan"
    "KR" -> "South Korea"
    "MY" -> "Malaysia"
    "NL" -> "Netherlands"
    "NZ" -> "New Zealand"
    "PL" -> "Poland"
    "PT" -> "Portugal"
    "QA" -> "Qatar"
    "RU" -> "Russia"
    "SE" -> "Sweden"
    "SG" -> "Singapore"
    "TW" -> "Taiwan"
    "TR" -> "Turkey"
    "UA" -> "Ukraine"
    "US" -> "United States"
    "ZA" -> "South Africa"
    else -> null
}
