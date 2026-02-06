/*
 * MdstStationLookup.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.base.mdst

import com.codebutler.farebot.base.util.getSystemLanguage

/**
 * Result of looking up a station from an MdST database.
 */
data class MdstStationResult(
    val stationName: String?,
    val shortStationName: String?,
    val companyName: String?,
    val lineNames: List<String>,
    val latitude: Float,
    val longitude: Float
) {
    val hasLocation: Boolean
        get() = latitude != 0f || longitude != 0f
}

/**
 * Convenience methods for looking up station data from MdST database files.
 */
object MdstStationLookup {

    /**
     * Check if the device language matches any of the database's local languages.
     * This is used to determine whether to prefer local or English names.
     *
     * Exposed for testing purposes.
     */
    internal fun shouldUseLocalName(deviceLanguage: String, localLanguages: List<String>): Boolean {
        return localLanguages.any { lang ->
            lang.equals(deviceLanguage, ignoreCase = true) ||
                lang.startsWith(deviceLanguage, ignoreCase = true) ||
                deviceLanguage.startsWith(lang, ignoreCase = true)
        }
    }

    /**
     * Select the best name from a Names object based on the device locale.
     * Exposed for testing purposes.
     *
     * @param names The Names object containing English and local name variants
     * @param localLanguages List of language codes for which local names are appropriate
     * @param deviceLanguage The device's current language code (or null to use system default)
     * @param isShort If true, prefer short name variants; otherwise prefer full names
     * @return The best name to display, or null if no name is available
     */
    internal fun selectName(
        names: Names?,
        localLanguages: List<String>,
        deviceLanguage: String? = null,
        isShort: Boolean = false
    ): String? {
        if (names == null) return null

        val language = deviceLanguage ?: getSystemLanguage()
        val useLocal = shouldUseLocalName(language, localLanguages)

        return if (isShort) {
            if (useLocal) {
                names.localShort.ifEmpty { names.englishShort.ifEmpty { null } }
            } else {
                names.englishShort.ifEmpty { names.localShort.ifEmpty { null } }
            }
        } else {
            if (useLocal) {
                names.local.ifEmpty { names.english.ifEmpty { null } }
            } else {
                names.english.ifEmpty { names.local.ifEmpty { null } }
            }
        }
    }

    /**
     * Look up a station by ID in the specified MdST database.
     *
     * @param dbName The name of the MdST file (without extension)
     * @param stationId The station ID to look up
     * @return Station result, or null if not found
     */
    fun getStation(dbName: String, stationId: Int): MdstStationResult? {
        val reader = MdstStationTableReader.getReader(dbName) ?: return null
        val station = reader.getStationById(stationId) ?: return null

        val operatorName = if (station.operatorId != 0) {
            val op = reader.getOperator(station.operatorId)
            selectBestName(op?.name, reader.localLanguages)
        } else null

        val lineNames = station.lineId.mapNotNull { lineId ->
            val line = reader.getLine(lineId)
            // Use short names for lines (matching Metrodroid's selectBestName(isShort=true))
            selectShortName(line?.name, reader.localLanguages)
                ?: selectBestName(line?.name, reader.localLanguages)
        }

        return MdstStationResult(
            stationName = selectBestName(station.name, reader.localLanguages),
            shortStationName = selectShortName(station.name, reader.localLanguages),
            companyName = operatorName,
            lineNames = lineNames,
            latitude = station.latitude,
            longitude = station.longitude
        )
    }

    /**
     * Get the operator name from an MdST database.
     *
     * @param dbName The name of the MdST file (without extension)
     * @param operatorId The operator ID to look up
     * @param isShort If true, returns the short form of the operator name if available
     * @return Operator name, or null if not found
     */
    fun getOperatorName(dbName: String, operatorId: Int, isShort: Boolean = false): String? {
        val reader = MdstStationTableReader.getReader(dbName) ?: return null
        val op = reader.getOperator(operatorId) ?: return null
        return if (isShort) {
            selectShortName(op.name, reader.localLanguages)
                ?: selectBestName(op.name, reader.localLanguages)
        } else {
            selectBestName(op.name, reader.localLanguages)
        }
    }

    /**
     * Get the line name from an MdST database.
     */
    fun getLineName(dbName: String, lineId: Int): String? {
        val reader = MdstStationTableReader.getReader(dbName) ?: return null
        val line = reader.getLine(lineId) ?: return null
        return selectBestName(line.name, reader.localLanguages)
    }

    /**
     * Get the transport type for an operator's default mode.
     */
    fun getOperatorDefaultMode(dbName: String, operatorId: Int): TransportType? {
        val reader = MdstStationTableReader.getReader(dbName) ?: return null
        return reader.getOperatorDefaultTransport(operatorId)
    }

    /**
     * Get the transport type for a line.
     */
    fun getLineMode(dbName: String, lineId: Int): TransportType? {
        val reader = MdstStationTableReader.getReader(dbName) ?: return null
        return reader.getLineTransport(lineId)
    }

    /**
     * Select the best name based on the device locale.
     * If the device language matches one of the local languages for this database,
     * prefer the local name. Otherwise fall back to English.
     */
    private fun selectBestName(names: Names?, localLanguages: List<String>): String? {
        return selectName(names, localLanguages, isShort = false)
    }

    /**
     * Select the best short name based on the device locale.
     * If the device language matches one of the local languages for this database,
     * prefer the local short name. Otherwise fall back to English short name.
     */
    private fun selectShortName(names: Names?, localLanguages: List<String>): String? {
        return selectName(names, localLanguages, isShort = true)
    }
}
