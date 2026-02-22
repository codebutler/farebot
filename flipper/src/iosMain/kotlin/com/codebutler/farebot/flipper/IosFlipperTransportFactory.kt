package com.codebutler.farebot.flipper

class IosFlipperTransportFactory : FlipperTransportFactory {
    override val isUsbSupported: Boolean = false
    override val isBleSupported: Boolean = true

    override suspend fun createUsbTransport(): FlipperTransport? = null

    override suspend fun createBleTransport(): FlipperTransport = IosBleSerialTransport()
}
