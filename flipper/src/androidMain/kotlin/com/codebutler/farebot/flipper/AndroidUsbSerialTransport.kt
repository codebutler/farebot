package com.codebutler.farebot.flipper

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * FlipperTransport implementation using Android USB Host API.
 * Communicates with the Flipper Zero via CDC ACM (virtual serial port).
 *
 * Flipper Zero USB identifiers: VID 0x0483 (STMicroelectronics), PID 0x5740.
 */
class AndroidUsbSerialTransport(
    private val context: Context,
) : FlipperTransport {
    companion object {
        const val FLIPPER_VID = 0x0483
        const val FLIPPER_PID = 0x5740
        private const val ACTION_USB_PERMISSION = "com.codebutler.farebot.USB_PERMISSION"
        private const val TIMEOUT_MS = 5000
    }

    private var connection: UsbDeviceConnection? = null
    private var dataInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null

    override val isConnected: Boolean
        get() = connection != null

    override val requiresRpcSessionInit: Boolean get() = true

    override suspend fun connect() {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val device =
            findFlipperDevice(usbManager)
                ?: throw FlipperException("Flipper Zero not found. Is it connected via USB?")

        if (!usbManager.hasPermission(device)) {
            requestPermission(usbManager, device)
        }

        val conn =
            usbManager.openDevice(device)
                ?: throw FlipperException("Failed to open USB device")

        // Find the CDC Data interface (class 0x0A)
        var dataIface: UsbInterface? = null
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA) {
                dataIface = iface
                break
            }
        }

        if (dataIface == null) {
            conn.close()
            throw FlipperException("CDC Data interface not found on device")
        }

        if (!conn.claimInterface(dataIface, true)) {
            conn.close()
            throw FlipperException("Failed to claim CDC Data interface")
        }

        // Find bulk IN and OUT endpoints
        var bulkIn: UsbEndpoint? = null
        var bulkOut: UsbEndpoint? = null
        for (i in 0 until dataIface.endpointCount) {
            val ep = dataIface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_IN) {
                    bulkIn = ep
                } else {
                    bulkOut = ep
                }
            }
        }

        if (bulkIn == null || bulkOut == null) {
            conn.releaseInterface(dataIface)
            conn.close()
            throw FlipperException("Bulk endpoints not found")
        }

        connection = conn
        dataInterface = dataIface
        inEndpoint = bulkIn
        outEndpoint = bulkOut
    }

    override suspend fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        val conn = connection ?: throw FlipperException("Not connected")
        val ep = inEndpoint ?: throw FlipperException("No IN endpoint")

        val tempBuffer = ByteArray(length)
        val bytesRead = conn.bulkTransfer(ep, tempBuffer, length, TIMEOUT_MS)
        if (bytesRead < 0) {
            throw FlipperException("USB read failed (error $bytesRead)")
        }
        tempBuffer.copyInto(buffer, offset, 0, bytesRead)
        return bytesRead
    }

    override suspend fun write(data: ByteArray) {
        val conn = connection ?: throw FlipperException("Not connected")
        val ep = outEndpoint ?: throw FlipperException("No OUT endpoint")

        val result = conn.bulkTransfer(ep, data, data.size, TIMEOUT_MS)
        if (result < 0) {
            throw FlipperException("USB write failed (error $result)")
        }
    }

    override suspend fun close() {
        val conn = connection ?: return
        val iface = dataInterface
        if (iface != null) {
            conn.releaseInterface(iface)
        }
        conn.close()
        connection = null
        dataInterface = null
        inEndpoint = null
        outEndpoint = null
    }

    private fun findFlipperDevice(usbManager: UsbManager): UsbDevice? =
        usbManager.deviceList.values.firstOrNull { device ->
            device.vendorId == FLIPPER_VID && device.productId == FLIPPER_PID
        }

    @Suppress("UnspecifiedRegisterReceiverFlag")
    private suspend fun requestPermission(
        usbManager: UsbManager,
        device: UsbDevice,
    ) = suspendCancellableCoroutine { cont ->
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    context.unregisterReceiver(this)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (granted) {
                        cont.resume(Unit)
                    } else {
                        cont.resumeWithException(FlipperException("USB permission denied"))
                    }
                }
            }

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags)
        usbManager.requestPermission(device, permissionIntent)

        cont.invokeOnCancellation {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }
}
