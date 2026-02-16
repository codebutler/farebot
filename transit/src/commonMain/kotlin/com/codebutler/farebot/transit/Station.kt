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

import com.codebutler.farebot.base.util.FormattedString
import farebot.transit.generated.resources.Res
import farebot.transit.generated.resources.unknown_station
import farebot.transit.generated.resources.unknown_station_format

data class Station(
    val humanReadableId: String? = null,
    val companyName: String? = null,
    val lineNames: List<String> = emptyList(),
    val latitude: Float? = null,
    val longitude: Float? = null,
    val isUnknown: Boolean = false,
    val humanReadableLineIds: List<String> = emptyList(),
    val stationName: String? = null,
    val shortStationName: String? = null,
    val formattedStationName: FormattedString? = null,
    val attributes: List<FormattedString> = emptyList(),
) {
    val displayName: FormattedString?
        get() {
            if (formattedStationName != null) return formattedStationName
            val name = shortStationName ?: stationName
            if (name != null) return FormattedString(name)
            if (isUnknown) {
                val id =
                    humanReadableId
                        ?: return FormattedString(Res.string.unknown_station)
                return FormattedString(Res.string.unknown_station_format, id)
            }
            return null
        }

    fun hasLocation(): Boolean = latitude != null && longitude != null

    fun addAttribute(attr: FormattedString): Station = copy(attributes = attributes + attr)

    companion object {
        fun unknown(id: String): Station =
            Station(
                humanReadableId = id,
                isUnknown = true,
            )

        fun nameOnly(name: String): Station =
            Station(
                stationName = name,
            )

        fun nameOnly(name: FormattedString): Station =
            Station(
                formattedStationName = name,
            )

        fun create(
            stationName: String?,
            shortStationName: String?,
            latitude: String?,
            longitude: String?,
        ): Station =
            Station(
                stationName = stationName,
                shortStationName = shortStationName,
                latitude = latitude?.toFloatOrNull(),
                longitude = longitude?.toFloatOrNull(),
            )

        fun create(
            name: String?,
            code: String?,
            abbreviation: String?,
            latitude: String?,
            longitude: String?,
        ): Station =
            Station(
                stationName = name,
                shortStationName = abbreviation,
                humanReadableId = code,
                latitude = latitude?.toFloatOrNull(),
                longitude = longitude?.toFloatOrNull(),
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

        fun stationName(stationName: String?): Builder {
            this.stationName = stationName
            return this
        }

        fun shortStationName(shortStationName: String?): Builder {
            this.shortStationName = shortStationName
            return this
        }

        fun companyName(companyName: String?): Builder {
            this.companyName = companyName
            return this
        }

        fun lineNames(lineNames: List<String>): Builder {
            this.lineNames = lineNames
            return this
        }

        fun latitude(latitude: String?): Builder {
            this.latitude = latitude
            return this
        }

        fun longitude(longitude: String?): Builder {
            this.longitude = longitude
            return this
        }

        fun code(code: String?): Builder {
            this.code = code
            return this
        }

        fun abbreviation(abbreviation: String?): Builder {
            this.abbreviation = abbreviation
            return this
        }

        fun build(): Station =
            Station(
                stationName = stationName,
                shortStationName = shortStationName ?: abbreviation,
                companyName = companyName,
                lineNames = lineNames,
                latitude = latitude?.toFloatOrNull(),
                longitude = longitude?.toFloatOrNull(),
                humanReadableId = code,
            )
    }
}
