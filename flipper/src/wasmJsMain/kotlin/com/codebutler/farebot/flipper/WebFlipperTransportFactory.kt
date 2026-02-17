package com.codebutler.farebot.flipper

class WebFlipperTransportFactory : FlipperTransportFactory {
    override val isUsbSupported: Boolean = true
    override val isBleSupported: Boolean = true

    override suspend fun createUsbTransport(): FlipperTransport =
        WebSerialTransport()

    override suspend fun createBleTransport(): FlipperTransport =
        WebBleTransport()
}
