package com.codebutler.farebot.base.mdst

import java.io.File

actual fun loadTestFile(relativePath: String): ByteArray? {
    val possibleRoots =
        listOf(
            System.getenv("PROJECT_DIR"),
            System.getProperty("user.dir"),
            ".",
            "..",
        )

    for (root in possibleRoots) {
        if (root == null) continue
        val file = File(root, relativePath)
        if (file.exists()) {
            return file.readBytes()
        }
    }
    return null
}
