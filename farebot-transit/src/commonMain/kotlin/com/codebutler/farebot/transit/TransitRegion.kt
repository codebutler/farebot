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

/**
 * Represents a geographic region for a transit card.
 *
 * Regions are used to group cards by location in the supported cards list.
 */
sealed class TransitRegion {
    /**
     * The human-readable name of this region, typically a country name.
     */
    abstract val translatedName: String

    /**
     * Sorting key for ordering regions. The first element is a section priority
     * (lower = higher priority), the second is the name for alphabetical sorting.
     */
    open val sortingKey: Pair<Int, String>
        get() = Pair(SECTION_MAIN, translatedName)

    /**
     * A region based on an ISO 3166-1 alpha-2 country code.
     */
    data class Iso(val code: String) : TransitRegion() {
        override val translatedName: String
            get() = iso3166AlphaToName(code) ?: code

        override val sortingKey: Pair<Int, String>
            get() = Pair(SECTION_MAIN, translatedName)
    }

    /**
     * Special region for Crimea, which may be shown near the user
     * if they are in Russia or Ukraine.
     */
    data object Crimea : TransitRegion() {
        override val translatedName: String
            get() = "Crimea"
    }

    /**
     * A region with a custom name and section priority.
     */
    data class Named(
        val name: String,
        val section: Int = SECTION_MAIN
    ) : TransitRegion() {
        override val translatedName: String
            get() = name

        override val sortingKey: Pair<Int, String>
            get() = Pair(section, translatedName)
    }

    /**
     * Comparator for sorting regions by section priority, then alphabetically by name.
     */
    object RegionComparator : Comparator<TransitRegion> {
        override fun compare(a: TransitRegion, b: TransitRegion): Int {
            val ak = a.sortingKey
            val bk = b.sortingKey
            if (ak.first != bk.first) {
                return ak.first.compareTo(bk.first)
            }
            return ak.second.compareTo(bk.second, ignoreCase = true)
        }
    }

    companion object {
        // On very top put cards that are most likely to be relevant to the user
        const val SECTION_NEARBY = -2
        // Then put "Worldwide" cards like EMV and Amiibo
        const val SECTION_WORLDWIDE = -1
        // Then goes the rest
        const val SECTION_MAIN = 0

        // Common regions as convenience constants
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

/**
 * Convert ISO 3166-1 alpha-2 country code to country name.
 * This is a simple lookup table - expand as needed.
 */
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
