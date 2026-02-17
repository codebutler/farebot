package com.codebutler.farebot.flipper

class JvmFlipperTransportFactory : FlipperTransportFactory {
    override val isUsbSupported: Boolean = true
    override val isBleSupported: Boolean = false

    override suspend fun createUsbTransport(): FlipperTransport =
        JvmUsbSerialTransport()

    override suspend fun createBleTransport(): FlipperTransport? = null
}
