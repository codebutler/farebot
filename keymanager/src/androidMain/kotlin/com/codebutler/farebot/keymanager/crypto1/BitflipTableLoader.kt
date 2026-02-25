package com.codebutler.farebot.keymanager.crypto1

import farebot.keymanager.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi

actual object BitflipTableLoader {
    @OptIn(ExperimentalResourceApi::class)
    actual fun loadEvenBitflipResource(): ByteArray? = loadResource("files/hardnested_bitflip_even.bin")

    @OptIn(ExperimentalResourceApi::class)
    actual fun loadOddBitflipResource(): ByteArray? = loadResource("files/hardnested_bitflip_odd.bin")

    @OptIn(ExperimentalResourceApi::class)
    private fun loadResource(path: String): ByteArray? =
        try {
            runBlocking {
                Res.readBytes(path)
            }
        } catch (_: Exception) {
            null
        }
}
