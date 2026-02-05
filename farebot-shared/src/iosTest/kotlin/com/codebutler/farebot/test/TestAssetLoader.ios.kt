/*
 * TestAssetLoader.ios.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.test

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/**
 * iOS implementation of test resource loading.
 * Uses NSBundle to find resources in the test bundle.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun loadTestResource(path: String): ByteArray? {
    // Try to find the resource in the main bundle
    val bundle = NSBundle.mainBundle
    val pathComponents = path.split("/")
    val fileName = pathComponents.lastOrNull() ?: return null
    val directory = if (pathComponents.size > 1) {
        pathComponents.dropLast(1).joinToString("/")
    } else {
        null
    }

    val resourcePath = bundle.pathForResource(
        name = fileName.substringBeforeLast("."),
        ofType = fileName.substringAfterLast(".", ""),
        inDirectory = directory
    ) ?: return null

    val data = NSData.dataWithContentsOfFile(resourcePath) ?: return null
    return data.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return bytes
}
