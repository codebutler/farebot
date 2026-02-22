package com.codebutler.farebot.flipper

/**
 * Factory for creating platform-specific FlipperTransport instances.
 * Each platform implements this to provide USB and/or BLE transport.
 */
interface FlipperTransportFactory {
    /** Returns true if USB transport is supported on this platform. */
    val isUsbSupported: Boolean

    /** Returns true if BLE transport is supported on this platform. */
    val isBleSupported: Boolean

    /**
     * Creates a USB serial transport.
     * May show a device picker dialog.
     * Returns null if USB is not supported or user cancelled.
     */
    suspend fun createUsbTransport(): FlipperTransport?

    /**
     * Creates a BLE serial transport.
     * May show a device picker/scan dialog.
     * Returns null if BLE is not supported or user cancelled.
     */
    suspend fun createBleTransport(): FlipperTransport?
}
