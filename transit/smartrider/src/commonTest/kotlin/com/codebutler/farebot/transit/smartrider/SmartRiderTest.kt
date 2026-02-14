/*
 * SmartRiderTest.kt
 *
 * Copyright 2016-2022 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.smartrider

import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for SmartRider card timestamp conversion.
 *
 * Ported from Metrodroid's SmartRiderTest.kt.
 */
class SmartRiderTest {
    /**
     * Tests timestamp conversion for SmartRider and MyWay cards.
     *
     * SmartRider/MyWay timestamps are stored as seconds since 2000-01-01 00:00:00 LOCAL time.
     * The convertTime function converts these to UTC Instants by applying the appropriate
     * timezone offset.
     *
     * SmartRider uses Perth timezone (Australia/Perth, UTC+8).
     * MyWay uses Sydney timezone with a fixed -11 hour offset from the base epoch.
     *
     * Both cards store the SAME local time, but the UTC Instant will be different
     * because they're in different timezones.
     */
    @Test
    fun testTimestamps() {
        // Test value: 529800999 seconds since 2000-01-01 00:00:00 LOCAL time
        // This represents 2016-10-14 22:56:39 local time on the card
        val epochTime = 529800999L

        // For MyWay (Sydney time with -11 hour offset):
        // The card stores local time, so 529800999 = 2016-10-14 22:56:39 Sydney time
        // The convertTime function uses a fixed -11 hour offset from 2000-01-01 UTC
        val myWayTime = convertTime(epochTime, SmartRiderType.MYWAY)
        assertNotNull(myWayTime, "MyWay time should not be null")

        // Verify the local datetime when interpreted in Sydney timezone
        // Should be 2016-10-14 22:56:39 local time
        val sydneyTime = myWayTime.toLocalDateTime(SYDNEY_TIMEZONE)
        assertEquals(2016, sydneyTime.year)
        assertEquals(Month.OCTOBER, sydneyTime.month)
        assertEquals(14, sydneyTime.day)
        assertEquals(22, sydneyTime.hour)
        assertEquals(56, sydneyTime.minute)
        assertEquals(39, sydneyTime.second)

        // For SmartRider (Perth time with -8 hour offset):
        // The card stores local time, so 529800999 = 2016-10-14 22:56:39 Perth time
        val smartRiderTime = convertTime(epochTime, SmartRiderType.SMARTRIDER)
        assertNotNull(smartRiderTime, "SmartRider time should not be null")

        // The UTC instants should be DIFFERENT because Perth and Sydney are in different timezones
        // Perth is UTC+8, Sydney (during October DST) is UTC+11
        // So the same local time represents different UTC moments

        // Verify the local datetime when interpreted in Perth timezone
        // Should be 2016-10-14 22:56:39 local time
        val perthTime = smartRiderTime.toLocalDateTime(PERTH_TIMEZONE)
        assertEquals(2016, perthTime.year)
        assertEquals(Month.OCTOBER, perthTime.month)
        assertEquals(14, perthTime.day)
        assertEquals(22, perthTime.hour)
        assertEquals(56, perthTime.minute)
        assertEquals(39, perthTime.second)
    }

    /**
     * Tests that zero epoch time returns null.
     */
    @Test
    fun testZeroTimestamp() {
        val result = convertTime(0L, SmartRiderType.SMARTRIDER)
        assertEquals(null, result, "Zero epoch should return null")
    }

    /**
     * Tests date conversion from days since 1997-01-01.
     */
    @Test
    fun testDateConversion() {
        // 0 days = 1997-01-01
        val date0 = convertDate(0)
        val local0 = date0.toLocalDateTime(TimeZone.UTC)
        assertEquals(1997, local0.year)
        assertEquals(Month.JANUARY, local0.month)
        assertEquals(1, local0.day)

        // 365 days = 1998-01-01 (1997 is not a leap year)
        val date365 = convertDate(365)
        val local365 = date365.toLocalDateTime(TimeZone.UTC)
        assertEquals(1998, local365.year)
        assertEquals(Month.JANUARY, local365.month)
        assertEquals(1, local365.day)

        // Test a specific known date: 2016-10-14 = how many days since 1997-01-01?
        // From 1997-01-01 to 2016-10-14:
        // Years 1997-2015: 19 years
        // Leap years in that range: 2000, 2004, 2008, 2012 = 4 leap years
        // Days from those years: 15*365 + 4*366 = 6939
        // 2016-01-01 is day 6939
        // Oct 14 is day-of-year 288 in 2016, so we add 287 more days (288-1)
        // Total: 6939 + 287 = 7226
        val dateKnown = convertDate(7226)
        val localKnown = dateKnown.toLocalDateTime(TimeZone.UTC)
        assertEquals(2016, localKnown.year)
        assertEquals(Month.OCTOBER, localKnown.month)
        assertEquals(14, localKnown.day)
    }

    /**
     * Tests SmartRiderTripBitfield parsing.
     */
    @Test
    fun testTripBitfield() {
        // Test bus mode (0x00)
        val busBitfield = SmartRiderTripBitfield(SmartRiderType.SMARTRIDER, 0x00)
        assertEquals(com.codebutler.farebot.transit.Trip.Mode.BUS, busBitfield.mode)

        // Test train mode for SmartRider (0x01)
        val trainBitfield = SmartRiderTripBitfield(SmartRiderType.SMARTRIDER, 0x01)
        assertEquals(com.codebutler.farebot.transit.Trip.Mode.TRAIN, trainBitfield.mode)

        // Test tram mode for MyWay (0x01)
        val tramBitfield = SmartRiderTripBitfield(SmartRiderType.MYWAY, 0x01)
        assertEquals(com.codebutler.farebot.transit.Trip.Mode.TRAM, tramBitfield.mode)

        // Test ferry mode (0x02)
        val ferryBitfield = SmartRiderTripBitfield(SmartRiderType.SMARTRIDER, 0x02)
        assertEquals(com.codebutler.farebot.transit.Trip.Mode.FERRY, ferryBitfield.mode)

        // Test tap on flag (0x10)
        val tapOnBitfield = SmartRiderTripBitfield(SmartRiderType.SMARTRIDER, 0x10)
        assertEquals(true, tapOnBitfield.isTapOn)
        assertEquals(false, tapOnBitfield.isTransfer)

        // Test transfer flag (0x08)
        val transferBitfield = SmartRiderTripBitfield(SmartRiderType.SMARTRIDER, 0x08)
        assertEquals(true, transferBitfield.isTransfer)
        assertEquals(false, transferBitfield.isTapOn)

        // Test synthetic flag (0x04)
        val syntheticBitfield = SmartRiderTripBitfield(SmartRiderType.SMARTRIDER, 0x04)
        assertEquals(true, syntheticBitfield.isSynthetic)

        // Test negative balance flag (0x80)
        val negativeBitfield = SmartRiderTripBitfield(SmartRiderType.SMARTRIDER, 0x80)
        assertEquals(true, negativeBitfield.isBalanceNegative)

        // Test combined flags
        val combinedBitfield = SmartRiderTripBitfield(SmartRiderType.SMARTRIDER, 0x19) // 0x01 + 0x08 + 0x10
        assertEquals(com.codebutler.farebot.transit.Trip.Mode.TRAIN, combinedBitfield.mode)
        assertEquals(true, combinedBitfield.isTransfer)
        assertEquals(true, combinedBitfield.isTapOn)
    }
}
