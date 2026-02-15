/*
 * SuicaTrip.kt
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * Based on code from http://code.google.com/p/nfc-felica/
 * nfc-felica by Kazzz. See project URL for complete author information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Thanks to these resources for providing additional information about the Suica format:
 * http://www.denno.net/SFCardFan/
 * http://jennychan.web.fc2.com/format/suica.html
 * http://d.hatena.ne.jp/baroqueworksdev/20110206/1297001722
 * http://handasse.blogspot.com/2008/04/python-pasorisuica.html
 * http://sourceforge.jp/projects/felicalib/wiki/suica
 *
 * Some of these resources have been translated into English at:
 * https://github.com/micolous/metrodroid/wiki/Suica
 */

package com.codebutler.farebot.transit.suica

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.card.felica.FelicaBlock
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.base.util.FormattedString
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class SuicaTrip(
    val balance: Int,
    val consoleTypeInt: Int,
    private val processType: Int,
    val fareRaw: Int,
    override var startTimestamp: Instant?,
    override var endTimestamp: Instant?,
    override val startStation: Station?,
    override val endStation: Station?,
    val startStationId: Int,
    val endStationId: Int,
    val dateRaw: Int,
) : Trip() {
    companion object {
        private const val CONSOLE_BUS = 0x05
        private const val CONSOLE_CHARGE = 0x02

        /**
         * Used when localisePlaces=true to ensure route and line numbers are still read out in the
         * user's language.
         *
         * eg:
         * - "#7 Eastern Line" -> (local)#7 (foreign)Eastern Line
         * - "300 West" -> (local)300 (foreign)West
         * - "North Ferry" -> (foreign)North Ferry
         */
        private val LINE_NUMBER = Regex("(#?\\d+)?(\\D.+)")

        private fun isTVM(consoleTypeInt: Int): Boolean {
            val consoleType = consoleTypeInt and 0xFF
            val tvmConsoleTypes = intArrayOf(0x03, 0x07, 0x08, 0x12, 0x13, 0x14, 0x15)
            return consoleType in tvmConsoleTypes
        }

        fun parse(
            block: FelicaBlock,
            previousBalance: Int,
        ): SuicaTrip {
            val data = block.data

            val consoleTypeInt = data[0].toInt()
            val processType = data[1].toInt()

            val isProductSale = consoleTypeInt == 0xc7.toByte().toInt() || consoleTypeInt == 0xc8.toByte().toInt()

            val dateRaw = data.byteArrayToInt(4, 2)
            val startTimestamp = SuicaUtil.extractDate(isProductSale, data)

            @Suppress("UnnecessaryVariable")
            val endTimestamp = startTimestamp
            // Balance is little-endian
            val balance = data.byteArrayToIntReversed(10, 2)

            val regionCode = data[15].toInt() and 0xFF

            val fareRaw =
                if (previousBalance >= 0) {
                    previousBalance - balance
                } else {
                    // Can't get amount for first record.
                    0
                }

            val startStation: Station?
            val endStation: Station?
            // Unused block (new card)
            if (startTimestamp == null) {
                startStation = null
                endStation = null
            } else if (isProductSale || processType == CONSOLE_CHARGE.toByte().toInt()) {
                startStation = null
                endStation = null
            } else if (consoleTypeInt == CONSOLE_BUS.toByte().toInt()) {
                val busLineCode = data.byteArrayToInt(6, 2)
                val busStopCode = data.byteArrayToInt(8, 2)
                startStation = SuicaUtil.getBusStop(regionCode, busLineCode, busStopCode)
                endStation = null
            } else if (isTVM(consoleTypeInt)) {
                val railEntranceLineCode = data[6].toInt() and 0xFF
                val railEntranceStationCode = data[7].toInt() and 0xFF
                startStation =
                    SuicaUtil.getRailStation(
                        regionCode,
                        railEntranceLineCode,
                        railEntranceStationCode,
                    )
                endStation = null
            } else {
                val railEntranceLineCode = data[6].toInt() and 0xFF
                val railEntranceStationCode = data[7].toInt() and 0xFF
                val railExitLineCode = data[8].toInt() and 0xFF
                val railExitStationCode = data[9].toInt() and 0xFF
                startStation =
                    SuicaUtil.getRailStation(
                        regionCode,
                        railEntranceLineCode,
                        railEntranceStationCode,
                    )
                endStation =
                    SuicaUtil.getRailStation(
                        regionCode,
                        railExitLineCode,
                        railExitStationCode,
                    )
            }

            return SuicaTrip(
                balance = balance,
                consoleTypeInt = consoleTypeInt,
                processType = processType,
                fareRaw = fareRaw,
                startTimestamp = startTimestamp,
                endTimestamp = endTimestamp,
                startStation = startStation,
                endStation = endStation,
                startStationId = data.byteArrayToInt(6, 2),
                endStationId = data.byteArrayToInt(8, 2),
                dateRaw = dateRaw,
            )
        }
    }

    override val fare: TransitCurrency
        get() = TransitCurrency.JPY(fareRaw)

    override val routeName: FormattedString?
        get() {
            if (startStation == null) {
                val consoleTypeName = SuicaUtil.getConsoleTypeName(consoleTypeInt)
                val processTypeName = SuicaUtil.getProcessTypeName(processType)
                return consoleTypeName + FormattedString(" ") + processTypeName
            }
            return super.routeName
        }

    override val humanReadableRouteID: String?
        get() =
            if (startStation != null) {
                super.humanReadableRouteID
            } else {
                "${NumberUtils.intToHex(consoleTypeInt)} ${NumberUtils.intToHex(processType)}"
            }

    override val agencyName: FormattedString?
        get() = startStation?.companyName?.let { FormattedString(it) }

    override val mode: Mode
        get() {
            val consoleType = consoleTypeInt and 0xFF
            return when {
                isTVM(consoleTypeInt) -> Mode.TICKET_MACHINE
                processType == CONSOLE_CHARGE.toByte().toInt() -> Mode.TICKET_MACHINE
                consoleType == 0xc8 -> Mode.VENDING_MACHINE
                consoleType == 0xc7 -> Mode.POS
                consoleTypeInt == CONSOLE_BUS.toByte().toInt() -> Mode.BUS
                else -> Mode.METRO
            }
        }

    fun setEndTime(
        hour: Int,
        min: Int,
    ) {
        val ts = endTimestamp ?: return
        val tz = TimeZone.of("Asia/Tokyo")
        val date = ts.toLocalDateTime(tz).date
        endTimestamp =
            LocalDateTime(date.year, date.month, date.day, hour, min, 0)
                .toInstant(tz)
    }

    fun setStartTime(
        hour: Int,
        min: Int,
    ) {
        val ts = startTimestamp ?: return
        val tz = TimeZone.of("Asia/Tokyo")
        val date = ts.toLocalDateTime(tz).date
        startTimestamp =
            LocalDateTime(date.year, date.month, date.day, hour, min, 0)
                .toInstant(tz)
    }
}
