package com.codebutler.farebot.base.mdst

actual object ResourceAccessor {
    actual fun openMdstFile(dbName: String): ByteArray? =
        try {
            val path = "composeResources/farebot.farebot_base.generated.resources/files/$dbName.mdst"
            Thread
                .currentThread()
                .contextClassLoader
                ?.getResourceAsStream(path)
                ?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
}
