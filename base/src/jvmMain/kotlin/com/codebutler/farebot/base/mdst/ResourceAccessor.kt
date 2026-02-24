package com.codebutler.farebot.base.mdst

import co.touchlab.kermit.Logger

private val log = Logger.withTag("ResourceAccessor")

actual object ResourceAccessor {
    actual fun openMdstFile(dbName: String): ByteArray? =
        try {
            val path = "composeResources/farebot.base.generated.resources/files/$dbName.mdst"
            Thread
                .currentThread()
                .contextClassLoader
                ?.getResourceAsStream(path)
                ?.use { it.readBytes() }
        } catch (e: Exception) {
            log.w(e) { "Failed to load resource: $dbName" }
            null
        }
}
