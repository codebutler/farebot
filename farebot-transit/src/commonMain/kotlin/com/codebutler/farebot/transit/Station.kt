/*
 * Station.kt
 *
 * Copyright (C) 2011, 2015-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016, 2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright (C) 2019 Google
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

package com.codebutler.farebot.transit

import kotlinx.serialization.Serializable

@Serializable
data class Station(
    val humanReadableId: String? = null,
    val companyName: String? = null,
    val lineNames: List<String> = emptyList(),
    val stationNameRaw: String? = null,
    val shortStationNameRaw: String? = null,
    val latitude: Float? = null,
    val longitude: Float? = null,
    val isUnknown: Boolean = false,
    val humanReadableLineIds: List<String> = emptyList(),
    val attributes: List<String> = emptyList()
) {
    fun getStationName(showRawIds: Boolean = false): String? {
        if (isUnknown) {
            return humanReadableId ?: "Unknown"
        }
        val base = shortStationNameRaw ?: stationNameRaw
        if (showRawIds && humanReadableId != null && base != null) {
            return "$base [$humanReadableId]"
        }
        return base
    }

    val stationName: String? get() = getStationName()

    fun hasLocation(): Boolean = latitude != null && longitude != null

    fun addAttribute(attr: String): Station = copy(attributes = attributes + attr)

    companion object {
        fun unknown(id: String): Station = Station(
            humanReadableId = id,
            isUnknown = true
        )

        fun nameOnly(name: String): Station = Station(
            stationNameRaw = name
        )

        // Backwards-compatible factory methods

        fun create(
            stationName: String?,
            shortStationName: String?,
            latitude: String?,
            longitude: String?
        ): Station = Station(
            stationNameRaw = stationName,
            shortStationNameRaw = shortStationName,
            latitude = latitude?.toFloatOrNull(),
            longitude = longitude?.toFloatOrNull()
        )

        fun create(
            name: String?,
            code: String?,
            abbreviation: String?,
            latitude: String?,
            longitude: String?
        ): Station = Station(
            stationNameRaw = name,
            shortStationNameRaw = abbreviation,
            humanReadableId = code,
            latitude = latitude?.toFloatOrNull(),
            longitude = longitude?.toFloatOrNull()
        )

        fun builder(): Builder = Builder()
    }

    class Builder {
        private var stationName: String? = null
        private var shortStationName: String? = null
        private var companyName: String? = null
        private var lineNames: List<String> = emptyList()
        private var latitude: String? = null
        private var longitude: String? = null
        private var code: String? = null
        private var abbreviation: String? = null

        fun stationName(stationName: String?): Builder { this.stationName = stationName; return this }
        fun shortStationName(shortStationName: String?): Builder { this.shortStationName = shortStationName; return this }
        fun companyName(companyName: String?): Builder { this.companyName = companyName; return this }
        fun lineNames(lineNames: List<String>): Builder { this.lineNames = lineNames; return this }
        fun latitude(latitude: String?): Builder { this.latitude = latitude; return this }
        fun longitude(longitude: String?): Builder { this.longitude = longitude; return this }
        fun code(code: String?): Builder { this.code = code; return this }
        fun abbreviation(abbreviation: String?): Builder { this.abbreviation = abbreviation; return this }

        fun build(): Station = Station(
            stationNameRaw = stationName,
            shortStationNameRaw = shortStationName ?: abbreviation,
            companyName = companyName,
            lineNames = lineNames,
            latitude = latitude?.toFloatOrNull(),
            longitude = longitude?.toFloatOrNull(),
            humanReadableId = code
        )
    }
}
