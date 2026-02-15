package com.codebutler.farebot.base.mdst

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun loadTestFile(relativePath: String): ByteArray? {
    val possibleRoots =
        listOf(
            getEnv("GITHUB_WORKSPACE") ?: "",
            getEnv("PROJECT_DIR") ?: "",
            "/Users/eric/Code/farebot",
            ".",
        )

    for (root in possibleRoots) {
        if (root.isEmpty()) continue
        val fullPath = "$root/$relativePath"
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(fullPath)) {
            val data = NSData.dataWithContentsOfFile(fullPath) ?: continue
            val bytes = ByteArray(data.length.toInt())
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
            return bytes
        }
    }
    return null
}

@OptIn(ExperimentalForeignApi::class)
private fun getEnv(name: String): String? = platform.posix.getenv(name)?.toKString()
