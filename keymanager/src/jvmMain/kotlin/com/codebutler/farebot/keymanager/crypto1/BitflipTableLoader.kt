package com.codebutler.farebot.keymanager.crypto1

actual object BitflipTableLoader {
    actual fun loadEvenBitflipResource(): ByteArray? = loadResource("hardnested_bitflip_even.bin")

    actual fun loadOddBitflipResource(): ByteArray? = loadResource("hardnested_bitflip_odd.bin")

    private fun loadResource(name: String): ByteArray? =
        try {
            val path = "composeResources/farebot.keymanager.generated.resources/files/$name"
            Thread
                .currentThread()
                .contextClassLoader
                ?.getResourceAsStream(path)
                ?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
}
