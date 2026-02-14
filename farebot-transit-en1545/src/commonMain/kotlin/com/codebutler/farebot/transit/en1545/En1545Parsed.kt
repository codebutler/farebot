/*
 * En1545Parsed.kt
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

package com.codebutler.farebot.transit.en1545

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

sealed class En1545Value {
    data class IntValue(
        val v: Int,
    ) : En1545Value()

    data class StringValue(
        val v: String,
    ) : En1545Value()
}

class En1545Parsed(
    private val map: MutableMap<String, En1545Value> = mutableMapOf(),
) {
    operator fun plus(other: En1545Parsed) = En1545Parsed((map + other.map).toMutableMap())

    fun insertInt(
        name: String,
        path: String,
        value: Int,
    ) {
        map[makeFullName(name, path)] = En1545Value.IntValue(value)
    }

    fun insertString(
        name: String,
        path: String,
        value: String,
    ) {
        map[makeFullName(name, path)] = En1545Value.StringValue(value)
    }

    fun getInt(
        name: String,
        path: String = "",
    ): Int? = (map[makeFullName(name, path)] as? En1545Value.IntValue)?.v

    fun getInt(
        name: String,
        vararg ipath: Int,
    ): Int? {
        val path = StringBuilder()
        for (iel in ipath) {
            path.append("/").append(iel.toString())
        }
        return (map[makeFullName(name, path.toString())] as? En1545Value.IntValue)?.v
    }

    fun getIntOrZero(
        name: String,
        path: String = "",
    ) = getInt(name, path) ?: 0

    fun getString(
        name: String,
        path: String = "",
    ): String? = (map[makeFullName(name, path)] as? En1545Value.StringValue)?.v

    fun getTimeStamp(
        name: String,
        tz: TimeZone,
    ): Instant? {
        if (contains(En1545FixedInteger.dateTimeName(name))) {
            return En1545FixedInteger.parseTimeSec(
                getIntOrZero(En1545FixedInteger.dateTimeName(name)),
                tz,
            )
        }
        if (contains(En1545FixedInteger.dateTimeLocalName(name))) {
            return En1545FixedInteger.parseTimeSecLocal(
                getIntOrZero(En1545FixedInteger.dateTimeLocalName(name)),
                tz,
            )
        }
        if (contains(En1545FixedInteger.timeName(name)) && contains(En1545FixedInteger.dateName(name))) {
            return En1545FixedInteger.parseTime(
                getIntOrZero(En1545FixedInteger.dateName(name)),
                getIntOrZero(En1545FixedInteger.timeName(name)),
                tz,
            )
        }
        if (contains(En1545FixedInteger.timeLocalName(name)) && contains(En1545FixedInteger.dateName(name))) {
            return En1545FixedInteger.parseTimeLocal(
                getIntOrZero(En1545FixedInteger.dateName(name)),
                getIntOrZero(En1545FixedInteger.timeLocalName(name)),
                tz,
            )
        }
        if (contains(En1545FixedInteger.timePacked16Name(name)) && contains(En1545FixedInteger.dateName(name))) {
            return En1545FixedInteger.parseTimePacked16(
                getIntOrZero(En1545FixedInteger.dateName(name)),
                getIntOrZero(En1545FixedInteger.timePacked16Name(name)),
                tz,
            )
        }
        if (contains(En1545FixedInteger.timePacked11LocalName(name)) &&
            contains(En1545FixedInteger.datePackedName(name))
        ) {
            return En1545FixedInteger.parseTimePacked11Local(
                getIntOrZero(En1545FixedInteger.datePackedName(name)),
                getIntOrZero(En1545FixedInteger.timePacked11LocalName(name)),
                tz,
            )
        }
        if (contains(En1545FixedInteger.dateName(name))) {
            return En1545FixedInteger.parseDate(
                getIntOrZero(En1545FixedInteger.dateName(name)),
                tz,
            )
        }
        if (contains(En1545FixedInteger.datePackedName(name))) {
            return En1545FixedInteger.parseDatePacked(
                getIntOrZero(En1545FixedInteger.datePackedName(name)),
            )
        }
        if (contains(En1545FixedInteger.dateBCDName(name))) {
            return En1545FixedInteger.parseDateBCD(
                getIntOrZero(En1545FixedInteger.dateBCDName(name)),
            )
        }
        return null
    }

    fun contains(
        name: String,
        path: String = "",
    ): Boolean = map.containsKey(makeFullName(name, path))

    fun append(
        data: ByteArray,
        off: Int,
        field: En1545Field,
    ): En1545Parsed {
        field.parseField(data, off, "", this) { obj, offset, len -> obj.getBitsFromBuffer(offset, len) }
        return this
    }

    fun appendLeBits(
        data: ByteArray,
        off: Int,
        field: En1545Field,
    ): En1545Parsed {
        field.parseField(data, off, "", this) { obj, offset, len -> obj.getBitsFromBufferLeBits(offset, len) }
        return this
    }

    fun append(
        data: ByteArray,
        field: En1545Field,
    ): En1545Parsed = append(data, 0, field)

    fun appendLeBits(
        data: ByteArray,
        field: En1545Field,
    ): En1545Parsed = appendLeBits(data, 0, field)

    fun makeString(
        separator: String,
        skipSet: Set<String>,
    ): String {
        val ret = StringBuilder()
        for ((key, value) in map) {
            if (skipSet.contains(getBaseName(key))) {
                continue
            }
            ret.append(key).append(" = ")
            when (value) {
                is En1545Value.IntValue -> ret.append("0x${value.v.toString(16)}")
                is En1545Value.StringValue -> ret.append("\"${value.v}\"")
            }
            ret.append(separator)
        }
        return ret.toString()
    }

    override fun toString(): String = "[" + makeString(", ", emptySet()) + "]"

    val entries: Set<Map.Entry<String, En1545Value>>
        get() = map.entries

    companion object {
        private fun makeFullName(
            name: String,
            path: String?,
        ): String = if (path.isNullOrEmpty()) name else "$path/$name"

        private fun getBaseName(name: String): String = name.substring(name.lastIndexOf('/') + 1)
    }
}
