/*
 * TripObfuscator.kt
 *
 * Copyright 2017-2018 Michael Farrell <micolous+git@gmail.com>
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

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 * Obfuscates trip data for privacy when sharing card exports.
 *
 * This allows users to share card data without revealing their actual travel patterns.
 * Timestamps can be shifted by random offsets, and fares can be obfuscated.
 */
object TripObfuscator {
    private const val TAG = "TripObfuscator"

    /**
     * Remaps days of the year to a different day of the year.
     * This mapping is initialized once per random source to ensure consistent obfuscation
     * within a card (same day always maps to same obfuscated day).
     */
    private var calendarMapping = (0..365).shuffled()
    private var randomSource: Random = Random.Default

    /**
     * Sets the random source for obfuscation. This is primarily for testing purposes.
     * Setting a new random source will also regenerate the calendar mapping.
     */
    fun setRandomSource(random: Random) {
        randomSource = random
        calendarMapping = (0..365).shuffled(random)
    }

    /**
     * Gets the current random source.
     */
    fun getRandomSource(): Random = randomSource

    /**
     * Resets to default random source.
     */
    fun resetRandomSource() {
        randomSource = Random.Default
        calendarMapping = (0..365).shuffled()
    }

    /**
     * Obfuscates a date by remapping the day of year using the calendar mapping.
     *
     * @param input The date to obfuscate
     * @param tz The timezone to use for date calculations
     * @return Obfuscated date as an Instant at start of day
     */
    private fun obfuscateDate(input: Instant, tz: TimeZone): Instant {
        val localDate = input.toLocalDateTime(tz).date
        var year = localDate.year
        var dayOfYear = localDate.dayOfYear

        if (dayOfYear <= calendarMapping.size) {
            dayOfYear = calendarMapping[dayOfYear - 1] + 1 // dayOfYear is 1-based
        }
        // If out of range, just use the original (shouldn't happen normally)

        val today = Clock.System.now().toLocalDateTime(tz).date

        // Create the new date from year and day of year
        val newDate = LocalDate(year, 1, 1).plusDays(dayOfYear - 1)

        // Adjust for the time of year - if the obfuscated date is in the future, move it back a year
        val adjustedDate = if (newDate > today) {
            LocalDate(year - 1, 1, 1).plusDays(dayOfYear - 1)
        } else {
            newDate
        }

        return adjustedDate.atStartOfDayIn(tz)
    }

    /**
     * Obfuscates a timestamp by optionally shifting the date and/or time.
     *
     * @param input The timestamp to obfuscate
     * @param obfuscateDates If true, remap the date using the calendar mapping
     * @param obfuscateTimes If true, reduce time resolution and add random offset
     * @param tz The timezone to use for date/time calculations
     * @return The obfuscated timestamp
     */
    fun maybeObfuscateTimestamp(
        input: Instant,
        obfuscateDates: Boolean,
        obfuscateTimes: Boolean,
        tz: TimeZone = TimeZone.currentSystemDefault()
    ): Instant {
        if (!obfuscateDates && !obfuscateTimes) {
            return input
        }

        var result = input

        if (obfuscateDates) {
            // Get the date-only part obfuscated
            val obfuscatedDate = obfuscateDate(input, tz)
            // Preserve the time-of-day from the original
            val originalLocalDateTime = input.toLocalDateTime(tz)
            val obfuscatedLocalDate = obfuscatedDate.toLocalDateTime(tz).date
            val newLocalDateTime = LocalDateTime(
                obfuscatedLocalDate.year,
                obfuscatedLocalDate.month,
                obfuscatedLocalDate.day,
                originalLocalDateTime.hour,
                originalLocalDateTime.minute,
                originalLocalDateTime.second,
                originalLocalDateTime.nanosecond
            )
            result = newLocalDateTime.toInstant(tz)
        }

        if (obfuscateTimes) {
            val localDateTime = result.toLocalDateTime(tz)
            // Reduce resolution of timestamps to 5 minutes
            var minute = (localDateTime.minute + 2) / 5 * 5
            var hour = localDateTime.hour
            if (minute >= 60) {
                minute = 0
                hour = (hour + 1) % 24
            }
            val roundedLocalDateTime = LocalDateTime(
                localDateTime.year,
                localDateTime.month,
                localDateTime.day,
                hour,
                minute,
                0, // zero out seconds
                0  // zero out nanoseconds
            )
            result = roundedLocalDateTime.toInstant(tz)

            // Add a deviation of up to 350 minutes (5.5 hours) earlier or later
            val offsetMinutes = randomSource.nextInt(700) - 350
            result = result.plus(offsetMinutes.minutes)
        }

        return result
    }

    /**
     * Calculates the time delta that should be applied to timestamps in a trip.
     * This ensures that start and end timestamps maintain their relative relationship.
     *
     * @param startTimestamp The original start timestamp (may be null)
     * @param obfuscateDates Whether dates should be obfuscated
     * @param obfuscateTimes Whether times should be obfuscated
     * @param tz Timezone for date calculations
     * @return The time delta in milliseconds to apply to all timestamps in the trip
     */
    fun calculateTimeDelta(
        startTimestamp: Instant?,
        obfuscateDates: Boolean,
        obfuscateTimes: Boolean,
        tz: TimeZone = TimeZone.currentSystemDefault()
    ): Long {
        if (startTimestamp == null) return 0L

        val obfuscatedStart = maybeObfuscateTimestamp(startTimestamp, obfuscateDates, obfuscateTimes, tz)
        return obfuscatedStart.toEpochMilliseconds() - startTimestamp.toEpochMilliseconds()
    }

    /**
     * Applies a time delta to a timestamp.
     *
     * @param timestamp The original timestamp (may be null)
     * @param deltaMillis The time delta in milliseconds
     * @return The adjusted timestamp, or null if input was null
     */
    fun applyTimeDelta(timestamp: Instant?, deltaMillis: Long): Instant? {
        if (timestamp == null) return null
        return Instant.fromEpochMilliseconds(timestamp.toEpochMilliseconds() + deltaMillis)
    }

    /**
     * Obfuscates a trip.
     *
     * @param trip The trip to obfuscate
     * @param obfuscateDates Whether to obfuscate dates
     * @param obfuscateTimes Whether to obfuscate times
     * @param obfuscateFares Whether to obfuscate fares
     * @param tz Timezone for date calculations
     * @return An ObfuscatedTrip with the obfuscated data
     */
    fun obfuscateTrip(
        trip: Trip,
        obfuscateDates: Boolean,
        obfuscateTimes: Boolean,
        obfuscateFares: Boolean,
        tz: TimeZone = TimeZone.currentSystemDefault()
    ): ObfuscatedTrip {
        val timeDelta = calculateTimeDelta(trip.startTimestamp, obfuscateDates, obfuscateTimes, tz)
        return ObfuscatedTrip(trip, timeDelta, obfuscateFares, randomSource)
    }

    /**
     * Obfuscates a list of trips.
     *
     * @param trips The trips to obfuscate
     * @param obfuscateDates Whether to obfuscate dates
     * @param obfuscateTimes Whether to obfuscate times
     * @param obfuscateFares Whether to obfuscate fares
     * @param tz Timezone for date calculations
     * @return List of ObfuscatedTrips with obfuscated data
     */
    fun obfuscateTrips(
        trips: List<Trip>,
        obfuscateDates: Boolean,
        obfuscateTimes: Boolean,
        obfuscateFares: Boolean,
        tz: TimeZone = TimeZone.currentSystemDefault()
    ): List<ObfuscatedTrip> {
        return trips.map { obfuscateTrip(it, obfuscateDates, obfuscateTimes, obfuscateFares, tz) }
    }

    /**
     * Obfuscates a currency value by adding a random offset and multiplying by a random factor.
     * The sign of the original value is preserved.
     *
     * @param currency The original currency value
     * @param random Random source for obfuscation
     * @return Obfuscated currency value
     */
    fun obfuscateCurrency(currency: TransitCurrency, random: Random = randomSource): TransitCurrency {
        val fareOffset = random.nextInt(100) - 50
        val fareMultiplier = random.nextDouble() * 0.4 + 0.8 // 0.8 to 1.2

        var obfuscatedValue = ((currency.currency + fareOffset) * fareMultiplier).toInt()

        // Match the sign of the original fare
        if (obfuscatedValue > 0 && currency.currency < 0 || obfuscatedValue < 0 && currency.currency >= 0) {
            obfuscatedValue *= -1
        }

        return TransitCurrency(obfuscatedValue, currency.currencyCode, currency.divisor)
    }
}

/**
 * Extension function on LocalDate to add days.
 */
private fun LocalDate.plusDays(days: Int): LocalDate {
    // Convert to Instant, add days, convert back to LocalDate
    val instant = this.atStartOfDayIn(TimeZone.UTC)
    val newInstant = instant.plus(days.days)
    return newInstant.toLocalDateTime(TimeZone.UTC).date
}

