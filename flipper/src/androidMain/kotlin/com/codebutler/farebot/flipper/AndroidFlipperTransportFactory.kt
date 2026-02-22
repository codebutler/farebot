package com.codebutler.farebot.flipper

import android.content.Context

class AndroidFlipperTransportFactory(
    private val context: Context,
) : FlipperTransportFactory {
    override val isUsbSupported: Boolean = true
    override val isBleSupported: Boolean = true

    override suspend fun createUsbTransport(): FlipperTransport = AndroidUsbSerialTransport(context)

    override suspend fun createBleTransport(): FlipperTransport = AndroidBleSerialTransport(context)
}
