package com.codebutler.farebot.base.mdst

import farebot.farebot_base.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi

actual object ResourceAccessor {
    @OptIn(ExperimentalResourceApi::class)
    actual fun openMdstFile(dbName: String): ByteArray? =
        try {
            runBlocking {
                Res.readBytes("files/$dbName.mdst")
            }
        } catch (e: Exception) {
            null
        }
}
