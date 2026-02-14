package com.codebutler.farebot.test

actual fun loadTestResource(path: String): ByteArray? {
    val stream =
        TestAssetLoader::class.java.getResourceAsStream("/$path")
            ?: TestAssetLoader::class.java.classLoader?.getResourceAsStream(path)
            ?: Thread.currentThread().contextClassLoader?.getResourceAsStream(path)

    return stream?.use { it.readBytes() }
}
