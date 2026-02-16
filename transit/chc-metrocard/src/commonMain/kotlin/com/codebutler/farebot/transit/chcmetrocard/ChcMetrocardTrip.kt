/*
 * ChcMetrocardTrip.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.chcmetrocard

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.mdst.TransportType
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.erg.ErgTrip
import com.codebutler.farebot.transit.erg.record.ErgPurseRecord

/**
 * Trip for CHC Metrocard (Christchurch, NZ).
 */
class ChcMetrocardTrip(
    purse: ErgPurseRecord,
    epochDate: Int,
) : ErgTrip(purse, epochDate, { TransitCurrency.NZD(it) }) {
    override val agencyName: FormattedString?
        get() = MdstStationLookup.getOperatorName(CHC_METROCARD_STR, purse.agency)?.let { FormattedString(it) }

    override val mode: Mode
        get() {
            val transportType = MdstStationLookup.getOperatorDefaultMode(CHC_METROCARD_STR, purse.agency)
            return when (transportType) {
                // There is a historic tram that circles the city, but not a commuter service, and does
                // not accept Metrocard. Therefore, everything unknown is a bus.
                TransportType.BUS -> Mode.BUS
                TransportType.TRAIN -> Mode.TRAIN
                TransportType.TRAM -> Mode.TRAM
                TransportType.METRO -> Mode.METRO
                TransportType.FERRY -> Mode.FERRY
                TransportType.TROLLEYBUS -> Mode.TROLLEYBUS
                TransportType.MONORAIL -> Mode.MONORAIL
                null -> Mode.BUS
                else -> Mode.BUS
            }
        }

    companion object {
        internal const val CHC_METROCARD_STR = "chc_metrocard"
    }
}
