/*
 * brute_force.metal
 *
 * Metal compute shader for MIFARE Classic hardnested brute force.
 *
 * Ports the Crypto1 LFSR cipher operations from Crypto1.kt to Metal
 * Shading Language for GPU-accelerated (odd, even) pair checking.
 *
 * Each thread checks one (odd, even) pair: rolls back 1 byte,
 * forward-clocks 4 bytes checking parity, and on success extracts
 * the 48-bit key and appends it to the results buffer atomically.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

#include <metal_stdlib>
using namespace metal;

// --- Result struct returned to host ---
struct BruteForceResult {
    uint odd_state;
    uint even_state;
    uint key_lo;  // lower 32 bits of 48-bit key
    uint key_hi;  // upper 16 bits of 48-bit key
};

// --- Crypto1 filter function (port of Crypto1.filter()) ---
// Nonlinear 20-bit to 1-bit Boolean function using lookup tables.
// Layer 1: 5 nibble-to-bit lookups; Layer 2: 5-bit selector.
inline uint crypto1_filter(uint x) {
    uint f;
    f  = (0xf22c0u >> (x & 0xf)) & 16u;
    f |= (0x6c9c0u >> ((x >> 4) & 0xf)) & 8u;
    f |= (0x3c8b0u >> ((x >> 8) & 0xf)) & 4u;
    f |= (0x1e458u >> ((x >> 12) & 0xf)) & 2u;
    f |= (0x0d938u >> ((x >> 16) & 0xf)) & 1u;
    return (0xEC57E80Au >> f) & 1u;
}

// --- XOR parity of all bits (port of Crypto1.parity()) ---
inline uint parity32(uint x) {
    uint v = x;
    v ^= v >> 16;
    v ^= v >> 8;
    v ^= v >> 4;
    return (0x6996u >> (v & 0xf)) & 1u;
}

// --- ISO 14443-3A odd parity (port of Crypto1Auth.oddParity()) ---
// Returns 1 when byte has even popcount, 0 when odd.
inline uint odd_parity8(uint b) {
    uint x = b;
    x ^= x >> 4;
    x ^= x >> 2;
    x ^= x >> 1;
    return (x & 1u) ^ 1u;
}

// --- LFSR constants ---
constant uint LF_POLY_ODD  = 0x29CE5Cu;
constant uint LF_POLY_EVEN = 0x870804u;

// --- LFSR rollback one bit (port of Crypto1State.lfsrRollbackBit()) ---
inline uint lfsr_rollback_bit(thread uint &odd, thread uint &even,
                              int input, bool is_encrypted) {
    odd &= 0xFFFFFFu;

    // Swap odd and even (XOR swap)
    odd ^= even;
    even ^= odd;
    odd ^= even;

    // Extract LSB of even
    uint out = even & 1u;
    even >>= 1;

    // Compute feedback
    uint feedback = out;
    feedback ^= (LF_POLY_EVEN & even);
    feedback ^= (LF_POLY_ODD & odd);
    feedback ^= (input != 0) ? 1u : 0u;

    uint ret = crypto1_filter(odd);
    feedback ^= is_encrypted ? (ret & 1u) : 0u;

    even |= parity32(feedback) << 23;

    return ret;
}

// --- LFSR rollback one byte (port of Crypto1State.lfsrRollbackByte()) ---
inline uint lfsr_rollback_byte(thread uint &odd, thread uint &even,
                               int input, bool is_encrypted) {
    uint ret = 0;
    for (int i = 7; i >= 0; i--) {
        ret |= lfsr_rollback_bit(odd, even, (input >> i) & 1, is_encrypted) << i;
    }
    return ret;
}

// --- LFSR forward one bit (port of Crypto1State.lfsrBit()) ---
inline uint lfsr_forward_bit(thread uint &odd, thread uint &even,
                             int input, bool is_encrypted) {
    uint ret = crypto1_filter(odd);

    uint feedin = is_encrypted ? (ret & 1u) : 0u;
    feedin ^= (input != 0) ? 1u : 0u;
    feedin ^= (LF_POLY_ODD & odd);
    feedin ^= (LF_POLY_EVEN & even);
    even = (even << 1) | parity32(feedin);

    // XOR swap odd/even
    odd ^= even;
    even ^= odd;
    odd ^= even;

    return ret;
}

// --- LFSR forward one byte (port of Crypto1State.lfsrByte()) ---
inline uint lfsr_forward_byte(thread uint &odd, thread uint &even,
                              int input, bool is_encrypted) {
    uint ret = 0;
    for (int i = 0; i < 8; i++) {
        ret |= lfsr_forward_bit(odd, even, (input >> i) & 1, is_encrypted) << i;
    }
    return ret;
}

// --- Extract 48-bit key (port of Crypto1State.getKey()) ---
// Interleaves odd and even halves back into a 48-bit key.
// CRITICAL: Must exactly match the Kotlin version's i^3 nibble reversal.
// Uses ulong to build the full 48-bit value identically to the Kotlin code,
// then splits into lo/hi for the result struct.
inline void get_key(uint odd, uint even, thread uint &key_lo, thread uint &key_hi) {
    ulong lfsr = 0;
    for (int i = 23; i >= 0; i--) {
        lfsr = (lfsr << 1) | (ulong)((odd  >> (i ^ 3)) & 1u);
        lfsr = (lfsr << 1) | (ulong)((even >> (i ^ 3)) & 1u);
    }
    key_lo = (uint)(lfsr & 0xFFFFFFFFul);
    key_hi = (uint)(lfsr >> 32);
}

// --- Params layout ---
// params[0]  = rollback_input
// params[1..4]  = input_bytes[0..3]
// params[5..8]  = enc_bytes[0..3]
// params[9..12] = enc_par_bits[0..3]

kernel void brute_force_parity(
    device const uint*          odd_states    [[buffer(0)]],
    device const uint*          even_states   [[buffer(1)]],
    constant uint*              params        [[buffer(2)]],
    device atomic_uint*         result_count  [[buffer(3)]],
    device BruteForceResult*    results       [[buffer(4)]],
    constant uint&              max_results   [[buffer(5)]],
    uint2                       gid           [[thread_position_in_grid]]
) {
    uint odd_idx  = gid.x;
    uint even_idx = gid.y;

    uint odd  = odd_states[odd_idx];
    uint even = even_states[even_idx];

    uint rollback_input = params[0];

    // Roll back 1 byte (post-byte-0 state → initial key state)
    lfsr_rollback_byte(odd, even, rollback_input, true);

    // Forward 4 bytes, checking parity after each
    for (int byte_idx = 0; byte_idx < 4; byte_idx++) {
        int input_byte = params[1 + byte_idx];
        int enc_byte   = params[5 + byte_idx];
        int enc_par    = params[9 + byte_idx];

        uint ks_byte = lfsr_forward_byte(odd, even, input_byte, true);
        uint ks_par  = crypto1_filter(odd);

        uint plain_byte = enc_byte ^ ks_byte;
        uint expected_par = odd_parity8(plain_byte) ^ ks_par;

        if (expected_par != (uint)enc_par) {
            return;  // Parity mismatch — discard this pair
        }
    }

    // All 4 bytes matched! Extract key from the initial state.
    // Need to re-rollback since we've now forward-clocked past the check.
    odd  = odd_states[odd_idx];
    even = even_states[even_idx];
    lfsr_rollback_byte(odd, even, rollback_input, true);

    uint key_lo, key_hi;
    get_key(odd, even, key_lo, key_hi);

    // Atomic append to results
    uint idx = atomic_fetch_add_explicit(result_count, 1, memory_order_relaxed);
    if (idx < max_results) {
        results[idx].odd_state  = odd_states[odd_idx];
        results[idx].even_state = even_states[even_idx];
        results[idx].key_lo     = key_lo;
        results[idx].key_hi     = key_hi;
    }
}
