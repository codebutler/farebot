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
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/**
 * iOS implementation of test resource loading.
 * Reads from the source tree since NSBundle.mainBundle doesn't contain test resources.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun loadTestResource(path: String): ByteArray? {
    val possibleRoots = listOf(
        "/Users/eric/Code/farebot",
        getEnv("PROJECT_DIR"),
        ".",
        ".."
    )

    val resourceDirs = listOf(
        "farebot-shared/src/commonTest/resources",
        "src/commonTest/resources"
    )

    val fileManager = NSFileManager.defaultManager
    for (root in possibleRoots) {
        if (root.isNullOrEmpty()) continue
        for (dir in resourceDirs) {
            val fullPath = "$root/$dir/$path"
            if (fileManager.fileExistsAtPath(fullPath)) {
                val data = NSData.dataWithContentsOfFile(fullPath) ?: continue
                return data.toByteArray()
            }
        }
    }
    return null
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

@OptIn(ExperimentalForeignApi::class)
private fun getEnv(name: String): String? = platform.posix.getenv(name)?.toKString()
