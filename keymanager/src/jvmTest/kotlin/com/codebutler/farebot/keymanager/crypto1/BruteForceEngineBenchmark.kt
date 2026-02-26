/*
 * BruteForceEngineBenchmark.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * Benchmark comparing SimdBruteForceEngine (Vector API) vs ScalarBruteForceEngine
 * on the same workload to measure speedup from SIMD bitslicing.
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

package com.codebutler.farebot.keymanager.crypto1

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.time.TimeSource

class BruteForceEngineBenchmark {
    @Test
    fun benchmarkSimdVsScalar() =
        runBlocking {
            val oddStates = IntArray(100) { i -> (i * 262139 + 0xABC) and 0xFFFFFF }
            val evenStates = IntArray(6400) { i -> (i * 131071 + 0xDEF) and 0xFFFFFF }
            val rollbackInput = 0x42
            val inputBytes = intArrayOf(0x11, 0x22, 0x33, 0x44)
            val encBytes = intArrayOf(0xAA, 0xBB, 0xCC, 0xDD)
            val encParBits = intArrayOf(1, 0, 1, 0)
            val verifyFn: (Long) -> Boolean = { true } // accept all for throughput measurement

            // Warmup
            val simd = SimdBruteForceEngine()
            val scalar = ScalarBruteForceEngine()

            repeat(3) {
                simd.bruteForce(
                    oddStates.sliceArray(0 until 10),
                    evenStates.sliceArray(0 until 640),
                    rollbackInput,
                    inputBytes,
                    encBytes,
                    encParBits,
                    verifyFn,
                    null,
                )
                scalar.bruteForce(
                    oddStates.sliceArray(0 until 10),
                    evenStates.sliceArray(0 until 640),
                    rollbackInput,
                    inputBytes,
                    encBytes,
                    encParBits,
                    verifyFn,
                    null,
                )
            }

            val totalPairs = oddStates.size.toLong() * evenStates.size

            // Benchmark SIMD
            val mark1 = TimeSource.Monotonic.markNow()
            simd.bruteForce(oddStates, evenStates, rollbackInput, inputBytes, encBytes, encParBits, verifyFn, null)
            val simdElapsed = mark1.elapsedNow()

            // Benchmark Scalar
            val mark2 = TimeSource.Monotonic.markNow()
            scalar.bruteForce(oddStates, evenStates, rollbackInput, inputBytes, encBytes, encParBits, verifyFn, null)
            val scalarElapsed = mark2.elapsedNow()

            val simdMs = simdElapsed.inWholeMilliseconds.coerceAtLeast(1)
            val scalarMs = scalarElapsed.inWholeMilliseconds.coerceAtLeast(1)
            val simdRate = totalPairs * 1000 / simdMs
            val scalarRate = totalPairs * 1000 / scalarMs
            println("Total pairs: $totalPairs")
            println("SIMD:   $simdElapsed, rate=${simdRate / 1_000_000}M/s")
            println("Scalar: $scalarElapsed, rate=${scalarRate / 1_000_000}M/s")
            println("Speedup: ${"%.1f".format(simdRate.toDouble() / scalarRate)}x")
        }
}
