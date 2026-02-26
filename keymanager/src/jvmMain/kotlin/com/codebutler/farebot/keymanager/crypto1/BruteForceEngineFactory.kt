/*
 * BruteForceEngineFactory.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * JVM actual implementation of createBruteForceEngine().
 * Cascade: Metal GPU → SIMD (Vector API) → Scalar fallback.
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

import co.touchlab.kermit.Logger

private val log = Logger.withTag("BruteForceEngine")

actual fun createBruteForceEngine(): BruteForceEngine {
    // 1. Try Metal GPU (macOS only)
    MetalBruteForceEngine.create()?.let {
        log.i { "Using Metal GPU: ${it.deviceName}" }
        return it
    }
    // 2. Try SIMD (Vector API)
    try {
        val simd = SimdBruteForceEngine()
        log.i { "Using SIMD (Vector API)" }
        return simd
    } catch (_: Throwable) {
        // Vector API not available
    }
    // 3. Scalar fallback
    log.i { "Using scalar engine" }
    return ScalarBruteForceEngine()
}

actual fun availableProcessors(): Int = Runtime.getRuntime().availableProcessors()
