/*
 * TransitSerializationTest.kt
 *
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

package com.codebutler.farebot.transit

import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for transit data serialization.
 *
 * Ported from Metrodroid's TransitDataSerializedTest.kt
 */
class TransitSerializationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testTransitCurrencySerializationRoundTrip() {
        val original = TransitCurrency.USD(350)

        // Serialize
        val jsonString = json.encodeToString(TransitCurrency.serializer(), original)
        assertTrue(jsonString.contains("350"))
        assertTrue(jsonString.contains("USD"))

        // Deserialize
        val deserialized = json.decodeFromString(TransitCurrency.serializer(), jsonString)
        assertEquals(original, deserialized)
    }

    @Test
    fun testTransitCurrencyWithCustomDivisor() {
        val original = TransitCurrency(5000, "JPY", 1)

        val jsonString = json.encodeToString(TransitCurrency.serializer(), original)
        val deserialized = json.decodeFromString(TransitCurrency.serializer(), jsonString)

        assertEquals(original.currency, deserialized.currency)
        assertEquals(original.currencyCode, deserialized.currencyCode)
        assertEquals(original.divisor, deserialized.divisor)
    }

    @Test
    fun testStationSerializationRoundTrip() {
        val original = Station(
            stationNameRaw = "Powell Street",
            companyName = "BART",
            latitude = 37.78447f,
            longitude = -122.40797f,
            humanReadableId = "BART:0001"
        )

        val jsonString = json.encodeToString(Station.serializer(), original)
        assertTrue(jsonString.contains("Powell Street"))
        assertTrue(jsonString.contains("BART"))

        val deserialized = json.decodeFromString(Station.serializer(), jsonString)
        assertEquals(original.stationName, deserialized.stationName)
        assertEquals(original.companyName, deserialized.companyName)
        assertEquals(original.latitude, deserialized.latitude)
        assertEquals(original.longitude, deserialized.longitude)
    }

    @Test
    fun testStationWithNullCoordinates() {
        val original = Station(
            stationNameRaw = "Unknown Station",
            companyName = null,
            latitude = null,
            longitude = null
        )

        val jsonString = json.encodeToString(Station.serializer(), original)
        val deserialized = json.decodeFromString(Station.serializer(), jsonString)

        assertEquals(original.stationName, deserialized.stationName)
        assertEquals(null, deserialized.companyName)
        assertEquals(null, deserialized.latitude)
        assertEquals(null, deserialized.longitude)
    }

    @Test
    fun testTransitBalanceSerializationRoundTrip() {
        val original = TransitBalance(
            balance = TransitCurrency.AUD(500),
            name = "Main Purse"
        )

        val jsonString = json.encodeToString(TransitBalance.serializer(), original)
        assertTrue(jsonString.contains("AUD"))
        assertTrue(jsonString.contains("Main Purse"))

        val deserialized = json.decodeFromString(TransitBalance.serializer(), jsonString)
        assertEquals(original.balance, deserialized.balance)
        assertEquals(original.name, deserialized.name)
    }

    @Test
    fun testTransitBalanceWithoutName() {
        val original = TransitBalance(
            balance = TransitCurrency.JPY(1000),
            name = null
        )

        val jsonString = json.encodeToString(TransitBalance.serializer(), original)
        val deserialized = json.decodeFromString(TransitBalance.serializer(), jsonString)

        assertEquals(original.balance, deserialized.balance)
        assertEquals(null, deserialized.name)
    }

    @Test
    fun testTransitIdentitySerializationRoundTrip() {
        val original = TransitIdentity(
            name = "Clipper",
            serialNumber = "572691763"
        )

        val jsonString = json.encodeToString(TransitIdentity.serializer(), original)
        assertTrue(jsonString.contains("Clipper"))
        assertTrue(jsonString.contains("572691763"))

        val deserialized = json.decodeFromString(TransitIdentity.serializer(), jsonString)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.serialNumber, deserialized.serialNumber)
    }

    // TODO: Re-enable once Trip.Mode and Subscription.SubscriptionState are @Serializable
    // @Test
    // fun testTripModeEnum() {
    //     // Verify all trip modes can be serialized/deserialized
    //     for (mode in Trip.Mode.entries) {
    //         val jsonString = json.encodeToString(Trip.Mode.serializer(), mode)
    //         val deserialized = json.decodeFromString(Trip.Mode.serializer(), jsonString)
    //         assertEquals(mode, deserialized)
    //     }
    // }
    //
    // @Test
    // fun testSubscriptionStateEnum() {
    //     // Verify all subscription states can be serialized/deserialized
    //     for (state in Subscription.SubscriptionState.entries) {
    //         val jsonString = json.encodeToString(Subscription.SubscriptionState.serializer(), state)
    //         val deserialized = json.decodeFromString(Subscription.SubscriptionState.serializer(), jsonString)
    //         assertEquals(state, deserialized)
    //     }
    // }

    @Test
    fun testMultipleCurrenciesInSerialization() {
        val currencies = listOf(
            TransitCurrency.USD(100),
            TransitCurrency.AUD(200),
            TransitCurrency.EUR(300),
            TransitCurrency.JPY(1000),
            TransitCurrency.GBP(500)
        )

        for (original in currencies) {
            val jsonString = json.encodeToString(TransitCurrency.serializer(), original)
            val deserialized = json.decodeFromString(TransitCurrency.serializer(), jsonString)
            assertEquals(original, deserialized, "Failed for currency: ${original.currencyCode}")
        }
    }

    @Test
    fun testNegativeCurrency() {
        val original = TransitCurrency.USD(-350)

        val jsonString = json.encodeToString(TransitCurrency.serializer(), original)
        val deserialized = json.decodeFromString(TransitCurrency.serializer(), jsonString)

        assertEquals(-350, deserialized.currency)
    }

    @Test
    fun testStationNameWithSpecialCharacters() {
        val original = Station(
            stationNameRaw = "San Jos\u00e9 Diridon", // é
            companyName = "Caltrain",
            latitude = 37.3298f,
            longitude = -121.9027f
        )

        val jsonString = json.encodeToString(Station.serializer(), original)
        val deserialized = json.decodeFromString(Station.serializer(), jsonString)

        assertEquals("San Jos\u00e9 Diridon", deserialized.stationName)
    }

    @Test
    fun testTransitCurrencyFieldNames() {
        // Test that serialized field names match expected format
        val currency = TransitCurrency.USD(350)
        val jsonString = json.encodeToString(TransitCurrency.serializer(), currency)

        // Should use SerialName annotations
        assertTrue(jsonString.contains("\"value\""), "Should serialize as 'value' not 'currency'")
        assertTrue(jsonString.contains("\"currencyCode\""), "Should have currencyCode field")
    }

    @Test
    fun testLargeCurrencyValue() {
        // Test with large values to ensure no overflow issues
        val original = TransitCurrency(Int.MAX_VALUE, "USD")

        val jsonString = json.encodeToString(TransitCurrency.serializer(), original)
        val deserialized = json.decodeFromString(TransitCurrency.serializer(), jsonString)

        assertEquals(Int.MAX_VALUE, deserialized.currency)
    }

    @Test
    fun testUnknownCurrencyCode() {
        val original = TransitCurrency.XXX(100)

        val jsonString = json.encodeToString(TransitCurrency.serializer(), original)
        val deserialized = json.decodeFromString(TransitCurrency.serializer(), jsonString)

        assertEquals("XXX", deserialized.currencyCode)
    }

    @Test
    fun testStationUnknownSerializationRoundTrip() {
        val original = Station.unknown("30890")

        val jsonString = json.encodeToString(Station.serializer(), original)
        assertTrue(jsonString.contains("30890"))
        assertTrue(jsonString.contains("isUnknown"))

        val deserialized = json.decodeFromString(Station.serializer(), jsonString)
        assertEquals(original.humanReadableId, deserialized.humanReadableId)
        assertEquals(original.isUnknown, deserialized.isUnknown)
        assertEquals(null, deserialized.stationNameRaw)
    }

    @Test
    fun testStationUnknownStationNameFormat() {
        val station = Station.unknown("0x1a2b")
        val name = station.stationName
        assertNotNull(name)
        // Station.unknown() uses Res.string.unknown_station_format which renders as "Unknown (id)"
        assertTrue(name.contains("0x1a2b"), "Unknown station name should contain the ID, got: $name")
    }

    @Test
    fun testStationNameOnlyPreservesRawName() {
        val station = Station.nameOnly("Bayfront")
        assertEquals("Bayfront", station.stationName)
        assertEquals(false, station.isUnknown)
        assertEquals(null, station.humanReadableId)
    }
}
