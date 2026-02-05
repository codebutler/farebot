/*
 * TripObfuscatorTest.kt
 *
 * Copyright 2025 Eric Butler <eric@codebutler.com>
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

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TripObfuscatorTest {

    @BeforeTest
    fun setUp() {
        // Use a fixed seed for reproducible tests
        TripObfuscator.setRandomSource(Random(12345))
    }

    @AfterTest
    fun tearDown() {
        TripObfuscator.resetRandomSource()
    }

    @Test
    fun testObfuscateCurrency() {
        val original = TransitCurrency.USD(500) // $5.00

        val obfuscated = TripObfuscator.obfuscateCurrency(original)

        // The obfuscated value should be different
        assertNotEquals(original.currency, obfuscated.currency)

        // Currency code should be preserved
        assertEquals(original.currencyCode, obfuscated.currencyCode)

        // Divisor should be preserved
        assertEquals(original.divisor, obfuscated.divisor)

        // Sign should be preserved for positive values
        assertTrue(obfuscated.currency > 0)
    }

    @Test
    fun testObfuscateCurrencyNegative() {
        val original = TransitCurrency.USD(-300) // -$3.00

        val obfuscated = TripObfuscator.obfuscateCurrency(original)

        // Sign should be preserved for negative values
        assertTrue(obfuscated.currency < 0)
    }

    @Test
    fun testMaybeObfuscateTimestampNoObfuscation() {
        val timestamp = Instant.fromEpochMilliseconds(1704067200000) // 2024-01-01 00:00:00 UTC
        val tz = TimeZone.UTC

        val result = TripObfuscator.maybeObfuscateTimestamp(
            timestamp,
            obfuscateDates = false,
            obfuscateTimes = false,
            tz = tz
        )

        // No obfuscation should return the same timestamp
        assertEquals(timestamp, result)
    }

    @Test
    fun testMaybeObfuscateTimestampWithTimeObfuscation() {
        val timestamp = Instant.fromEpochMilliseconds(1704067200000) // 2024-01-01 00:00:00 UTC
        val tz = TimeZone.UTC

        val result = TripObfuscator.maybeObfuscateTimestamp(
            timestamp,
            obfuscateDates = false,
            obfuscateTimes = true,
            tz = tz
        )

        // Time should be different due to random offset
        assertNotEquals(timestamp, result)
    }

    @Test
    fun testCalculateTimeDeltaNullTimestamp() {
        val delta = TripObfuscator.calculateTimeDelta(
            startTimestamp = null,
            obfuscateDates = true,
            obfuscateTimes = true
        )

        assertEquals(0L, delta)
    }

    @Test
    fun testApplyTimeDeltaNullTimestamp() {
        val result = TripObfuscator.applyTimeDelta(null, 1000L)
        assertNull(result)
    }

    @Test
    fun testApplyTimeDelta() {
        val timestamp = Instant.fromEpochMilliseconds(1704067200000)
        val delta = 60000L // 1 minute

        val result = TripObfuscator.applyTimeDelta(timestamp, delta)

        assertEquals(1704067260000, result?.toEpochMilliseconds())
    }

    @Test
    fun testObfuscateTrip() {
        val trip = TestTrip(
            startTimestamp = Instant.fromEpochMilliseconds(1704067200000),
            endTimestamp = Instant.fromEpochMilliseconds(1704070800000),
            fare = TransitCurrency.USD(250),
            mode = Trip.Mode.BUS
        )

        val obfuscatedTrip = TripObfuscator.obfuscateTrip(
            trip,
            obfuscateDates = true,
            obfuscateTimes = true,
            obfuscateFares = true,
            tz = TimeZone.UTC
        )

        // Timestamps should be obfuscated
        assertNotEquals(trip.startTimestamp, obfuscatedTrip.startTimestamp)
        assertNotEquals(trip.endTimestamp, obfuscatedTrip.endTimestamp)

        // Fare should be obfuscated
        assertNotEquals(trip.fare?.currency, obfuscatedTrip.fare?.currency)

        // Mode should be preserved
        assertEquals(trip.mode, obfuscatedTrip.mode)
    }

    @Test
    fun testObfuscateTrips() {
        val trips = listOf(
            TestTrip(
                startTimestamp = Instant.fromEpochMilliseconds(1704067200000),
                mode = Trip.Mode.BUS
            ),
            TestTrip(
                startTimestamp = Instant.fromEpochMilliseconds(1704070800000),
                mode = Trip.Mode.TRAIN
            )
        )

        val obfuscatedTrips = TripObfuscator.obfuscateTrips(
            trips,
            obfuscateDates = true,
            obfuscateTimes = true,
            obfuscateFares = true,
            tz = TimeZone.UTC
        )

        assertEquals(2, obfuscatedTrips.size)
        assertEquals(Trip.Mode.BUS, obfuscatedTrips[0].mode)
        assertEquals(Trip.Mode.TRAIN, obfuscatedTrips[1].mode)
    }

    /**
     * Simple test implementation of Trip for testing purposes.
     */
    private class TestTrip(
        override val startTimestamp: Instant?,
        override val endTimestamp: Instant? = null,
        override val fare: TransitCurrency? = null,
        override val mode: Mode
    ) : Trip()
}
