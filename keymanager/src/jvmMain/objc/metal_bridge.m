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

        id<MTLCommandQueue> queue = [device newCommandQueue];
        if (!queue) {
            NSLog(@"metal_bridge: Failed to create command queue");
            return NULL;
        }

        MetalContextImpl *ctx = (MetalContextImpl *)calloc(1, sizeof(MetalContextImpl));
        ctx->device = device;
        ctx->commandQueue = queue;
        ctx->pipeline = pipeline;

        const char *name = [[device name] UTF8String];
        if (name) {
            strncpy(ctx->deviceName, name, sizeof(ctx->deviceName) - 1);
        }

        // Retain Objective-C objects since we're storing them in a C struct
        CFRetain((__bridge CFTypeRef)device);
        CFRetain((__bridge CFTypeRef)queue);
        CFRetain((__bridge CFTypeRef)pipeline);

        return (MetalContext)ctx;
    }
}

void metal_destroy(MetalContext handle) {
    if (!handle) return;

    MetalContextImpl *ctx = (MetalContextImpl *)handle;

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

const char* metal_device_name(MetalContext handle) {
    if (!handle) return "unknown";
    MetalContextImpl *ctx = (MetalContextImpl *)handle;
    return ctx->deviceName;
}
