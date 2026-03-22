/*
 * metal_bridge.m
 *
 * Objective-C implementation of the Metal GPU compute bridge.
 * Creates Metal device, loads compiled .metallib, dispatches 2D compute
 * grids for hardnested brute force, and returns parity-check survivors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

#import <Metal/Metal.h>
#import <Foundation/Foundation.h>
#include "metal_bridge.h"

typedef struct {
    id<MTLDevice> device;
    id<MTLCommandQueue> commandQueue;
    id<MTLComputePipelineState> pipeline;
    id<MTLComputePipelineState> verifyPipeline;
    char deviceName[256];
} MetalContextImpl;

MetalContext metal_create(const char* metallib_path) {
    @autoreleasepool {
        id<MTLDevice> device = MTLCreateSystemDefaultDevice();
        if (!device) {
            return NULL;
        }

        NSString *path = [NSString stringWithUTF8String:metallib_path];
        NSURL *url = [NSURL fileURLWithPath:path];
        NSError *error = nil;

        id<MTLLibrary> library = [device newLibraryWithURL:url error:&error];
        if (!library) {
            NSLog(@"metal_bridge: Failed to load metallib: %@", error);
            return NULL;
        }

        id<MTLFunction> function = [library newFunctionWithName:@"brute_force_parity"];
        if (!function) {
            NSLog(@"metal_bridge: Failed to find brute_force_parity function");
            return NULL;
        }

        id<MTLComputePipelineState> pipeline =
            [device newComputePipelineStateWithFunction:function error:&error];
        if (!pipeline) {
            NSLog(@"metal_bridge: Failed to create pipeline: %@", error);
            return NULL;
        }

        // Create verify_key_nonces pipeline
        id<MTLFunction> verifyFunction = [library newFunctionWithName:@"verify_key_nonces"];
        id<MTLComputePipelineState> verifyPipeline = nil;
        if (verifyFunction) {
            verifyPipeline =
                [device newComputePipelineStateWithFunction:verifyFunction error:&error];
            if (!verifyPipeline) {
                NSLog(@"metal_bridge: Failed to create verify pipeline: %@", error);
                // Non-fatal: brute force still works, just no GPU verification
            }
        }

        id<MTLCommandQueue> queue = [device newCommandQueue];
        if (!queue) {
            NSLog(@"metal_bridge: Failed to create command queue");
            return NULL;
        }

        MetalContextImpl *ctx = (MetalContextImpl *)calloc(1, sizeof(MetalContextImpl));
        ctx->device = device;
        ctx->commandQueue = queue;
        ctx->pipeline = pipeline;
        ctx->verifyPipeline = verifyPipeline;

        const char *name = [[device name] UTF8String];
        if (name) {
            strncpy(ctx->deviceName, name, sizeof(ctx->deviceName) - 1);
        }

        // Retain Objective-C objects since we're storing them in a C struct
        CFRetain((__bridge CFTypeRef)device);
        CFRetain((__bridge CFTypeRef)queue);
        CFRetain((__bridge CFTypeRef)pipeline);
        if (verifyPipeline) {
            CFRetain((__bridge CFTypeRef)verifyPipeline);
        }

        return (MetalContext)ctx;
    }
}

void metal_destroy(MetalContext handle) {
    if (!handle) return;

    MetalContextImpl *ctx = (MetalContextImpl *)handle;

    if (ctx->verifyPipeline) {
        CFRelease((__bridge CFTypeRef)ctx->verifyPipeline);
    }
    CFRelease((__bridge CFTypeRef)ctx->pipeline);
    CFRelease((__bridge CFTypeRef)ctx->commandQueue);
    CFRelease((__bridge CFTypeRef)ctx->device);

    free(ctx);
}

int32_t metal_brute_force(
    MetalContext handle,
    const uint32_t* odd_states, int32_t odd_count,
    const uint32_t* even_states, int32_t even_count,
    uint32_t rollback_input,
    const uint32_t* input_bytes,
    const uint32_t* enc_bytes,
    const uint32_t* enc_par_bits,
    MetalBruteForceResult* results_out,
    int32_t max_results
) {
    @autoreleasepool {
        MetalContextImpl *ctx = (MetalContextImpl *)handle;

        id<MTLDevice> device = ctx->device;

        // Create buffers
        size_t oddSize = (size_t)odd_count * sizeof(uint32_t);
        size_t evenSize = (size_t)even_count * sizeof(uint32_t);

        id<MTLBuffer> oddBuf = [device newBufferWithBytes:odd_states
                                                   length:oddSize
                                                  options:MTLResourceStorageModeShared];
        id<MTLBuffer> evenBuf = [device newBufferWithBytes:even_states
                                                    length:evenSize
                                                   options:MTLResourceStorageModeShared];

        // Pack params: [rollback_input, input_bytes[4], enc_bytes[4], enc_par_bits[4]]
        uint32_t params[13];
        params[0] = rollback_input;
        for (int i = 0; i < 4; i++) {
            params[1 + i] = input_bytes[i];
            params[5 + i] = enc_bytes[i];
            params[9 + i] = enc_par_bits[i];
        }
        id<MTLBuffer> paramsBuf = [device newBufferWithBytes:params
                                                      length:sizeof(params)
                                                     options:MTLResourceStorageModeShared];

        // Result count (atomic)
        id<MTLBuffer> resultCountBuf = [device newBufferWithLength:sizeof(uint32_t)
                                                           options:MTLResourceStorageModeShared];
        memset([resultCountBuf contents], 0, sizeof(uint32_t));

        // Results buffer
        size_t resultsSize = (size_t)max_results * sizeof(MetalBruteForceResult);
        id<MTLBuffer> resultsBuf = [device newBufferWithLength:resultsSize
                                                       options:MTLResourceStorageModeShared];

        // Max results (constant)
        id<MTLBuffer> maxResultsBuf = [device newBufferWithBytes:&max_results
                                                          length:sizeof(uint32_t)
                                                         options:MTLResourceStorageModeShared];

        // Encode compute command
        id<MTLCommandBuffer> commandBuffer = [ctx->commandQueue commandBuffer];
        id<MTLComputeCommandEncoder> encoder = [commandBuffer computeCommandEncoder];

        [encoder setComputePipelineState:ctx->pipeline];
        [encoder setBuffer:oddBuf          offset:0 atIndex:0];
        [encoder setBuffer:evenBuf         offset:0 atIndex:1];
        [encoder setBuffer:paramsBuf       offset:0 atIndex:2];
        [encoder setBuffer:resultCountBuf  offset:0 atIndex:3];
        [encoder setBuffer:resultsBuf      offset:0 atIndex:4];
        [encoder setBuffer:maxResultsBuf   offset:0 atIndex:5];

        // 2D dispatch: (oddCount, evenCount)
        NSUInteger w = ctx->pipeline.threadExecutionWidth;  // typically 32
        NSUInteger h = ctx->pipeline.maxTotalThreadsPerThreadgroup / w;
        if (h > 8) h = 8;  // cap height to avoid oversizing

        MTLSize threadgroupSize = MTLSizeMake(w, h, 1);
        MTLSize gridSize = MTLSizeMake((NSUInteger)odd_count, (NSUInteger)even_count, 1);

        [encoder dispatchThreads:gridSize threadsPerThreadgroup:threadgroupSize];
        [encoder endEncoding];

        [commandBuffer commit];
        [commandBuffer waitUntilCompleted];

        if (commandBuffer.error) {
            NSLog(@"metal_bridge: GPU error: %@", commandBuffer.error);
            return 0;
        }

        // Read results
        uint32_t count = *(uint32_t *)[resultCountBuf contents];
        if (count > (uint32_t)max_results) {
            count = (uint32_t)max_results;
        }

        if (count > 0) {
            memcpy(results_out, [resultsBuf contents],
                   count * sizeof(MetalBruteForceResult));
        }

        return (int32_t)count;
    }
}

int32_t metal_verify_keys(
    MetalContext handle,
    const uint32_t* candidate_keys, int32_t key_count,
    const uint32_t* nonces, int32_t nonce_count,
    uint32_t uid,
    uint32_t* result_flags
) {
    @autoreleasepool {
        MetalContextImpl *ctx = (MetalContextImpl *)handle;

        if (!ctx->verifyPipeline) {
            NSLog(@"metal_bridge: verify pipeline not available");
            return -1;
        }

        id<MTLDevice> device = ctx->device;

        // Create buffers
        // candidate_keys: packed [key_lo, key_hi] pairs = 2 * key_count uint32s
        size_t keysSize = (size_t)key_count * 2 * sizeof(uint32_t);
        id<MTLBuffer> keysBuf = [device newBufferWithBytes:candidate_keys
                                                    length:keysSize
                                                   options:MTLResourceStorageModeShared];

        // nonces: packed [enc_nonce, enc_parity] pairs = 2 * nonce_count uint32s
        size_t noncesSize = (size_t)nonce_count * 2 * sizeof(uint32_t);
        id<MTLBuffer> noncesBuf = [device newBufferWithBytes:nonces
                                                      length:noncesSize
                                                     options:MTLResourceStorageModeShared];

        // params: [uid, nonce_count]
        uint32_t params[2];
        params[0] = uid;
        params[1] = (uint32_t)nonce_count;
        id<MTLBuffer> paramsBuf = [device newBufferWithBytes:params
                                                      length:sizeof(params)
                                                     options:MTLResourceStorageModeShared];

        // result_flags: one uint32 per key
        size_t flagsSize = (size_t)key_count * sizeof(uint32_t);
        id<MTLBuffer> flagsBuf = [device newBufferWithLength:flagsSize
                                                     options:MTLResourceStorageModeShared];
        memset([flagsBuf contents], 0, flagsSize);

        // Encode compute command
        id<MTLCommandBuffer> commandBuffer = [ctx->commandQueue commandBuffer];
        id<MTLComputeCommandEncoder> encoder = [commandBuffer computeCommandEncoder];

        [encoder setComputePipelineState:ctx->verifyPipeline];
        [encoder setBuffer:keysBuf    offset:0 atIndex:0];
        [encoder setBuffer:noncesBuf  offset:0 atIndex:1];
        [encoder setBuffer:paramsBuf  offset:0 atIndex:2];
        [encoder setBuffer:flagsBuf   offset:0 atIndex:3];

        // 1D dispatch: one thread per candidate key
        NSUInteger w = ctx->verifyPipeline.threadExecutionWidth;
        MTLSize threadgroupSize = MTLSizeMake(w, 1, 1);
        MTLSize gridSize = MTLSizeMake((NSUInteger)key_count, 1, 1);

        [encoder dispatchThreads:gridSize threadsPerThreadgroup:threadgroupSize];
        [encoder endEncoding];

        [commandBuffer commit];
        [commandBuffer waitUntilCompleted];

        if (commandBuffer.error) {
            NSLog(@"metal_bridge: GPU verify error: %@", commandBuffer.error);
            return -1;
        }

        // Copy results and count passes
        uint32_t *flags = (uint32_t *)[flagsBuf contents];
        int32_t passCount = 0;
        for (int32_t i = 0; i < key_count; i++) {
            result_flags[i] = flags[i];
            if (flags[i]) passCount++;
        }

        return passCount;
    }
}

const char* metal_device_name(MetalContext handle) {
    if (!handle) return "unknown";
    MetalContextImpl *ctx = (MetalContextImpl *)handle;
    return ctx->deviceName;
}
