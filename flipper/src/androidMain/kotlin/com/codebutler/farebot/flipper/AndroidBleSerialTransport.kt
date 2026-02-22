@file:Suppress("MissingPermission")

package com.codebutler.farebot.flipper

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * FlipperTransport implementation using Android BLE.
 * Connects to Flipper Zero's BLE Serial service.
 */
@SuppressLint("MissingPermission")
class AndroidBleSerialTransport(
    private val context: Context,
    private val device: BluetoothDevice? = null,
) : FlipperTransport {
    companion object {
        val SERIAL_SERVICE_UUID: UUID = UUID.fromString("8fe5b3d5-2e7f-4a98-2a48-7acc60fe0000")

        // Phone reads FROM Flipper (subscribe to notifications)
        val SERIAL_READ_UUID: UUID = UUID.fromString("19ed82ae-ed21-4c9d-4145-228e61fe0000")

        // Phone writes TO Flipper
        val SERIAL_WRITE_UUID: UUID = UUID.fromString("19ed82ae-ed21-4c9d-4145-228e62fe0000")
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val SCAN_TIMEOUT_MS = 15_000L
        private const val CONNECT_TIMEOUT_MS = 10_000L
    }

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private val receiveChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private var readBuffer = byteArrayOf()

    override val isConnected: Boolean
        get() = gatt != null

    override suspend fun connect() {
        val targetDevice = device ?: scanForFlipper()

        val connectionDeferred = CompletableDeferred<Unit>()
        val servicesDeferred = CompletableDeferred<Unit>()

        val callback =
            object : BluetoothGattCallback() {
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int,
                ) {
                    if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                        connectionDeferred.complete(Unit)
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                        connectionDeferred.completeExceptionally(
                            FlipperException("BLE connection established with error (status $status)"),
                        )
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        val error = FlipperException("BLE disconnected (status $status)")
                        if (!connectionDeferred.isCompleted) {
                            connectionDeferred.completeExceptionally(error)
                        }
                        if (!servicesDeferred.isCompleted) {
                            servicesDeferred.completeExceptionally(error)
                        }
                        receiveChannel.close(error)
                    }
                }

                override fun onServicesDiscovered(
                    gatt: BluetoothGatt,
                    status: Int,
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val service = gatt.getService(SERIAL_SERVICE_UUID)
                        if (service != null) {
                            writeCharacteristic = service.getCharacteristic(SERIAL_WRITE_UUID)
                            readCharacteristic = service.getCharacteristic(SERIAL_READ_UUID)
                            servicesDeferred.complete(Unit)
                        } else {
                            servicesDeferred.completeExceptionally(
                                FlipperException("Serial service not found on device"),
                            )
                        }
                    } else {
                        servicesDeferred.completeExceptionally(
                            FlipperException("Service discovery failed (status $status)"),
                        )
                    }
                }

                @Deprecated("Deprecated in API 33")
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                ) {
                    if (characteristic.uuid == SERIAL_READ_UUID) {
                        val data = characteristic.value
                        if (data != null && data.isNotEmpty()) {
                            receiveChannel.trySend(data)
                        }
                    }
                }
            }

        val bluetoothGatt = targetDevice.connectGatt(context, false, callback)
        this.gatt = bluetoothGatt

        withTimeout(CONNECT_TIMEOUT_MS) {
            connectionDeferred.await()
        }
        withTimeout(CONNECT_TIMEOUT_MS) {
            servicesDeferred.await()
        }

        // Request higher MTU for better throughput
        bluetoothGatt.requestMtu(512)

        // Enable notifications on the serial read characteristic (data from Flipper)
        val read =
            readCharacteristic
                ?: throw FlipperException("Serial read characteristic not found")
        bluetoothGatt.setCharacteristicNotification(read, true)
        val descriptor =
            read.getDescriptor(CCCD_UUID)
                ?: throw FlipperException("Serial read notification descriptor not found")
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        if (!bluetoothGatt.writeDescriptor(descriptor)) {
            throw FlipperException("Failed to enable serial read notifications")
        }
    }

    override suspend fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        if (readBuffer.isEmpty()) {
            readBuffer = receiveChannel.receive()
        }
        val bytesToCopy = minOf(readBuffer.size, length)
        readBuffer.copyInto(buffer, offset, 0, bytesToCopy)
        readBuffer = readBuffer.copyOfRange(bytesToCopy, readBuffer.size)
        return bytesToCopy
    }

    override suspend fun write(data: ByteArray) {
        val g = gatt ?: throw FlipperException("Not connected")
        val write = writeCharacteristic ?: throw FlipperException("Write characteristic not found")
        write.value = data
        if (!g.writeCharacteristic(write)) {
            throw FlipperException("BLE write failed")
        }
    }

    override suspend fun close() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        writeCharacteristic = null
        readCharacteristic = null
        readBuffer = byteArrayOf()
        receiveChannel.close()
    }

    private suspend fun scanForFlipper(): BluetoothDevice {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter =
            bluetoothManager.adapter
                ?: throw FlipperException("Bluetooth not available")

        if (!adapter.isEnabled) {
            throw FlipperException("Bluetooth is disabled")
        }

        return withTimeout(SCAN_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val scanner =
                    adapter.bluetoothLeScanner
                        ?: throw FlipperException("BLE scanner not available")

                val callback =
                    object : ScanCallback() {
                        override fun onScanResult(
                            callbackType: Int,
                            result: ScanResult,
                        ) {
                            val name = result.device.name ?: return
                            if (name.startsWith("Flipper", ignoreCase = true)) {
                                scanner.stopScan(this)
                                cont.resume(result.device)
                            }
                        }

                        override fun onScanFailed(errorCode: Int) {
                            cont.resumeWithException(FlipperException("BLE scan failed (error $errorCode)"))
                        }
                    }

                val settings =
                    ScanSettings
                        .Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build()

                scanner.startScan(null, settings, callback)

                cont.invokeOnCancellation {
                    scanner.stopScan(callback)
                }
            }
        }
    }
}
