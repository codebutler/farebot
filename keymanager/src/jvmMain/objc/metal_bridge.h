/*
 * metal_bridge.h
 *
 * C API for the Metal GPU compute bridge used by MetalBruteForceEngine.
 * Called from Java/Kotlin via Panama FFM (java.lang.foreign).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

#ifndef METAL_BRIDGE_H
#define METAL_BRIDGE_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/** Opaque handle to Metal context (device + pipeline + command queue). */
typedef void* MetalContext;

/** Result struct for each parity-check survivor. */
typedef struct {
    uint32_t odd_state;
    uint32_t even_state;
    uint32_t key_lo;   /* lower 32 bits of 48-bit key */
    uint32_t key_hi;   /* upper 16 bits of 48-bit key */
} MetalBruteForceResult;

/**
 * Create a Metal context from a compiled .metallib file.
 *
 * @param metallib_path Path to the brute_force.metallib file.
 * @return Opaque context handle, or NULL if Metal is unavailable.
 */
MetalContext metal_create(const char* metallib_path);

/**
 * Destroy a Metal context and release all resources.
 */
void metal_destroy(MetalContext ctx);

/**
 * Run the brute force parity check on the GPU.
 *
 * Dispatches a 2D compute grid (odd_count x even_count) to check all
 * (odd, even) state pairs. Returns the number of survivors found.
 *
 * @param ctx           Metal context from metal_create().
 * @param odd_states    Array of candidate odd LFSR half-states.
 * @param odd_count     Number of odd states.
 * @param even_states   Array of candidate even LFSR half-states.
 * @param even_count    Number of even states.
 * @param rollback_input  Input byte for rolling back 1 byte.
 * @param input_bytes   4-element array of uid^encNonce bytes.
 * @param enc_bytes     4-element array of encrypted nonce bytes.
 * @param enc_par_bits  4-element array of encrypted parity bits.
 * @param results_out   Output buffer for survivor results.
 * @param max_results   Maximum number of results to return.
 * @return Number of survivors found (clamped to max_results).
 */
int32_t metal_brute_force(
    MetalContext ctx,
    const uint32_t* odd_states, int32_t odd_count,
    const uint32_t* even_states, int32_t even_count,
    uint32_t rollback_input,
    const uint32_t* input_bytes,
    const uint32_t* enc_bytes,
    const uint32_t* enc_par_bits,
    MetalBruteForceResult* results_out,
    int32_t max_results
);

/**
 * Get the name of the Metal GPU device.
 *
 * @param ctx Metal context.
 * @return Device name string (e.g., "Apple M3 Pro"). Do not free.
 */
const char* metal_device_name(MetalContext ctx);

#ifdef __cplusplus
}
#endif

#endif /* METAL_BRIDGE_H */
