# Bitsliced Brute Force for Hardnested Attack

## Problem

Current brute force: ~35M pairs/sec (scalar, single-threaded per chunk).
With 4-8B candidate pairs, each tuple takes 2-4 minutes.
18 locked sectors × ~15 min/sector = ~4.5 hours total.

## Approach 1: JVM Vector API (Recommended)

JDK 25 has the stable Vector API (`jdk.incubator.vector` graduated).
Apple M-series has 128-bit NEON, mapped by HotSpot to NEON intrinsics.

### Concept

Bitslice the Crypto1 LFSR: instead of processing 1 state at a time,
pack N states into SIMD vectors and process them in parallel.

- `IntVector.SPECIES_128` → 4 states per operation (128-bit NEON)
- `IntVector.SPECIES_256` → 8 states per operation (AVX2, if available)
- `LongVector.SPECIES_128` → 2 states per operation

### What to bitslice

The inner brute force loop does:
1. `lfsrRollbackByte(rollbackInput, true)` — 8 reverse LFSR clocks
2. 4× `lfsrByte(inputByte, true)` — 32 forward LFSR clocks
3. 4× `filter(odd)` for parity check
4. Key extraction for survivors

Steps 1-3 are the bottleneck. Each LFSR clock involves:
- XOR of polynomial taps (bit operations on 24-bit halves)
- `filter()` — nonlinear 20→1 function (lookup tables)
- Parity computation

With bitslicing, each bit position becomes a separate SIMD vector.
48-bit LFSR → 48 SIMD vectors of width N. One clock step operates
on all N states simultaneously via bitwise AND/XOR/OR.

The `filter()` function needs to be decomposed into Boolean gates
(it's already defined as a two-layer Boolean function in Crypto1).

### Expected Speedup

- 128-bit NEON (M-series): 4× per operation → ~140M/s
- If we bitslice at bit level (48 bits × N): potentially 128× for 128-bit
- Realistic estimate: 8-16× → 280-560M/s

### References

- Proxmark3 `hardnested_bf_core.c` — AVX2/NEON bitsliced implementation
- "Ciphertext-only Cryptanalysis on Hardened Mifare Classic Cards" (ACM CCS 2015)

## Approach 2: Panama FFM (Foreign Function & Memory API)

Call native C code via `java.lang.foreign` (stable in JDK 22+).

### Concept

Port Proxmark3's `hardnested_bf_core.c` to a shared library (.dylib/.so),
call from Kotlin/JVM via Panama FFM. Zero-copy memory sharing.

### Pros
- Maximum performance (native SIMD intrinsics)
- Proven implementation from Proxmark3
- No JNI boilerplate (Panama is much cleaner)

### Cons
- Platform-specific native code (need to build per target)
- Doesn't work for Kotlin/Wasm or Kotlin/Native targets
- Maintenance burden for native build system

## Approach 3: Kotlin/Native + SIMD Intrinsics

For non-JVM targets, Kotlin/Native can interop with C.
Write the bitsliced core in C, expose via cinterop.

## Current Performance Baseline

- Scalar brute force: 24-35M pairs/sec (4 coroutine chunks)
- Nonce collection: ~100 nonces/sec (NFC I/O bound)
- Sum property + bitflip filtering: <1 sec
- Candidate space: 2-9B pairs per sector
- Total per sector: ~15 minutes (7 min collection + 8 min brute force)

## Priority

Medium — the attack works correctly now. Performance optimization
would reduce per-sector time from ~15 min to ~8-9 min (collection is
the bottleneck once brute force is fast enough).
