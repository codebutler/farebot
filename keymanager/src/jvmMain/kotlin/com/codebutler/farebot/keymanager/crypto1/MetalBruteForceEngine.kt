/*
 * MetalBruteForceEngine.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * Metal GPU-accelerated brute force engine for the hardnested attack.
 * Uses Panama FFM (java.lang.foreign) to call into libmetal_bridge.dylib,
 * which dispatches Crypto1 parity checking to an Apple Metal compute shader.
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
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.nio.file.Files
import java.nio.file.Path

private val log = Logger.withTag("MetalBruteForceEngine")

/**
 * Metal GPU-accelerated brute force engine.
 *
 * Dispatches (odd, even) pair checking to the GPU via Metal compute shader.
 * The GPU performs parity checking and key extraction; the CPU does
 * multi-nonce verification on survivors.
 *
 * Even states are chunked into batches of [EVEN_CHUNK_SIZE] to limit
 * Metal buffer sizes and allow progress reporting between chunks.
 */
class MetalBruteForceEngine private constructor(
    private val metalContext: MemorySegment,
    private val metalBruteForceHandle: MethodHandle,
    private val metalDestroyHandle: MethodHandle,
    val deviceName: String,
) : BruteForceEngine {
    companion object {
        private const val EVEN_CHUNK_SIZE = 100_000
        private const val MAX_RESULTS_PER_DISPATCH = 4096

        // MetalBruteForceResult struct: 4 x uint32_t = 16 bytes
        private val RESULT_STRUCT_LAYOUT: MemoryLayout =
            MemoryLayout.structLayout(
                ValueLayout.JAVA_INT.withName("odd_state"),
                ValueLayout.JAVA_INT.withName("even_state"),
                ValueLayout.JAVA_INT.withName("key_lo"),
                ValueLayout.JAVA_INT.withName("key_hi"),
            )

        private const val RESULT_STRUCT_SIZE = 16L // 4 * sizeof(int32)

        /**
         * Try to create a MetalBruteForceEngine.
         *
         * Returns null if:
         * - Not running on macOS
         * - Native libraries not found on classpath
         * - Metal device unavailable (e.g., no GPU)
         */
        fun create(): MetalBruteForceEngine? {
            if (!System.getProperty("os.name").orEmpty().contains("Mac")) {
                log.d { "Not macOS, Metal unavailable" }
                return null
            }

            try {
                // Extract native resources from classpath to temp dir
                val tempDir = Files.createTempDirectory("farebot-metal")
                tempDir.toFile().deleteOnExit()

                val dylibPath = extractResource("native/libmetal_bridge.dylib", tempDir)
                val metallibPath = extractResource("native/brute_force.metallib", tempDir)

                if (dylibPath == null || metallibPath == null) {
                    log.d { "Native resources not found on classpath" }
                    return null
                }

                // Load the dylib
                val arena = Arena.global()
                val lookup = SymbolLookup.libraryLookup(dylibPath, arena)
                val linker = Linker.nativeLinker()

                // metal_create(const char* metallib_path) -> void*
                val createHandle =
                    linker.downcallHandle(
                        lookup.find("metal_create").orElseThrow(),
                        FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                    )

                // metal_device_name(void* ctx) -> const char*
                val deviceNameHandle =
                    linker.downcallHandle(
                        lookup.find("metal_device_name").orElseThrow(),
                        FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                        ),
                    )

                // metal_destroy(void* ctx) -> void
                val destroyHandle =
                    linker.downcallHandle(
                        lookup.find("metal_destroy").orElseThrow(),
                        FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS,
                        ),
                    )

                // metal_brute_force(...) -> int32
                val bruteForceHandle =
                    linker.downcallHandle(
                        lookup.find("metal_brute_force").orElseThrow(),
                        FunctionDescriptor.of(
                            ValueLayout.JAVA_INT, // return: int32_t
                            ValueLayout.ADDRESS, // ctx
                            ValueLayout.ADDRESS, // odd_states
                            ValueLayout.JAVA_INT, // odd_count
                            ValueLayout.ADDRESS, // even_states
                            ValueLayout.JAVA_INT, // even_count
                            ValueLayout.JAVA_INT, // rollback_input
                            ValueLayout.ADDRESS, // input_bytes
                            ValueLayout.ADDRESS, // enc_bytes
                            ValueLayout.ADDRESS, // enc_par_bits
                            ValueLayout.ADDRESS, // results_out
                            ValueLayout.JAVA_INT, // max_results
                        ),
                    )

                // Create Metal context
                val pathStr = metallibPath.toString()
                val pathSegment = arena.allocateFrom(pathStr)
                val ctx = createHandle.invoke(pathSegment) as MemorySegment

                if (ctx == MemorySegment.NULL || ctx.address() == 0L) {
                    log.d { "metal_create returned NULL — Metal unavailable" }
                    return null
                }

                // Get device name
                val namePtr = deviceNameHandle.invoke(ctx) as MemorySegment
                val devName =
                    namePtr
                        .reinterpret(256)
                        .getString(0)

                log.i { "Metal GPU initialized: $devName" }

                return MetalBruteForceEngine(ctx, bruteForceHandle, destroyHandle, devName)
            } catch (e: Exception) {
                log.d(e) { "Failed to initialize Metal engine" }
                return null
            }
        }

        private fun extractResource(
            resourcePath: String,
            targetDir: Path,
        ): Path? {
            val stream =
                MetalBruteForceEngine::class.java.classLoader
                    ?.getResourceAsStream(resourcePath)
                    ?: return null

            val targetFile = targetDir.resolve(resourcePath.substringAfterLast('/'))
            stream.use { Files.copy(it, targetFile) }
            targetFile.toFile().deleteOnExit()
            return targetFile
        }
    }

    override suspend fun bruteForce(
        oddStates: IntArray,
        evenStates: IntArray,
        rollbackInput: Int,
        inputBytes: IntArray,
        encBytes: IntArray,
        encParBits: IntArray,
        verifyFn: (Long) -> Boolean,
        onProgress: ((tested: Long, candidates: Int) -> Unit)?,
    ): List<Long> {
        val candidates = mutableListOf<Long>()
        var totalTested = 0L

        // Process even states in chunks to limit buffer sizes
        val evenChunks = evenStates.toList().chunked(EVEN_CHUNK_SIZE)

        for (chunk in evenChunks) {
            val chunkEvenStates = chunk.toIntArray()

            Arena.ofConfined().use { arena ->
                // Allocate off-heap buffers
                val oddBuf = arena.allocateFrom(ValueLayout.JAVA_INT, *oddStates)
                val evenBuf = arena.allocateFrom(ValueLayout.JAVA_INT, *chunkEvenStates)
                val inputBytesBuf = arena.allocateFrom(ValueLayout.JAVA_INT, *inputBytes)
                val encBytesBuf = arena.allocateFrom(ValueLayout.JAVA_INT, *encBytes)
                val encParBitsBuf = arena.allocateFrom(ValueLayout.JAVA_INT, *encParBits)
                val resultsBuf =
                    arena.allocate(
                        RESULT_STRUCT_LAYOUT,
                        MAX_RESULTS_PER_DISPATCH.toLong(),
                    )

                // Dispatch to GPU
                val survivorCount =
                    metalBruteForceHandle.invoke(
                        metalContext,
                        oddBuf,
                        oddStates.size,
                        evenBuf,
                        chunkEvenStates.size,
                        rollbackInput,
                        inputBytesBuf,
                        encBytesBuf,
                        encParBitsBuf,
                        resultsBuf,
                        MAX_RESULTS_PER_DISPATCH,
                    ) as Int

                // Process survivors: extract keys and verify
                for (i in 0 until survivorCount) {
                    val offset = i.toLong() * RESULT_STRUCT_SIZE
                    val keyLo = resultsBuf.get(ValueLayout.JAVA_INT, offset + 8).toLong() and 0xFFFFFFFFL
                    val keyHi = resultsBuf.get(ValueLayout.JAVA_INT, offset + 12).toLong() and 0xFFFFL
                    val candidateKey = (keyHi shl 32) or keyLo

                    if (verifyFn(candidateKey)) {
                        candidates.add(candidateKey)
                    }
                }

                totalTested += oddStates.size.toLong() * chunkEvenStates.size.toLong()
            }

            onProgress?.invoke(totalTested, candidates.size)
        }

        return candidates
    }

    /** Release native Metal resources. */
    fun destroy() {
        try {
            metalDestroyHandle.invoke(metalContext)
        } catch (e: Exception) {
            log.w(e) { "Failed to destroy Metal context" }
        }
    }
}
