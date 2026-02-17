@file:Suppress("MissingPermission")

package com.codebutler.farebot.flipper

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
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
        val SERIAL_RX_UUID: UUID = UUID.fromString("19ed82ae-ed21-4c9d-4145-228e62fe0000")
        val SERIAL_TX_UUID: UUID = UUID.fromString("19ed82ae-ed21-4c9d-4145-228e63fe0000")
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val SCAN_TIMEOUT_MS = 15_000L
    }

    private var gatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private val receiveChannel = Channel<ByteArray>(Channel.UNLIMITED)

    override val isConnected: Boolean
        get() = gatt != null

    override suspend fun connect() {
        val targetDevice = device ?: scanForFlipper()

        val connectionDeferred = CompletableDeferred<Unit>()
        val servicesDeferred = CompletableDeferred<Unit>()

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectionDeferred.complete(Unit)
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (!connectionDeferred.isCompleted) {
                        connectionDeferred.completeExceptionally(FlipperException("BLE connection failed (status $status)"))
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(SERIAL_SERVICE_UUID)
                    if (service != null) {
                        rxCharacteristic = service.getCharacteristic(SERIAL_RX_UUID)
                        txCharacteristic = service.getCharacteristic(SERIAL_TX_UUID)
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
            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                if (characteristic.uuid == SERIAL_TX_UUID) {
                    val data = characteristic.value
                    if (data != null && data.isNotEmpty()) {
                        receiveChannel.trySend(data)
                    }
                }
            }
        }

        val bluetoothGatt = targetDevice.connectGatt(context, false, callback)
        this.gatt = bluetoothGatt

        connectionDeferred.await()
        servicesDeferred.await()

        // Request higher MTU for better throughput
        bluetoothGatt.requestMtu(512)

        // Enable notifications on the TX characteristic
        val tx = txCharacteristic
            ?: throw FlipperException("TX characteristic not found")
        bluetoothGatt.setCharacteristicNotification(tx, true)
        val descriptor = tx.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt.writeDescriptor(descriptor)
        }
    }

    override suspend fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val data = receiveChannel.receive()
        val bytesToCopy = minOf(data.size, length)
        data.copyInto(buffer, offset, 0, bytesToCopy)
        return bytesToCopy
    }

    override suspend fun write(data: ByteArray) {
        val g = gatt ?: throw FlipperException("Not connected")
        val rx = rxCharacteristic ?: throw FlipperException("RX characteristic not found")
        rx.value = data
        if (!g.writeCharacteristic(rx)) {
            throw FlipperException("BLE write failed")
        }
    }

    override suspend fun close() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        rxCharacteristic = null
        txCharacteristic = null
        receiveChannel.close()
    }

    private suspend fun scanForFlipper(): BluetoothDevice {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
            ?: throw FlipperException("Bluetooth not available")

        if (!adapter.isEnabled) {
            throw FlipperException("Bluetooth is disabled")
        }

        return withTimeout(SCAN_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val scanner = adapter.bluetoothLeScanner
                    ?: throw FlipperException("BLE scanner not available")

                val callback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        scanner.stopScan(this)
                        cont.resume(result.device)
                    }

                    override fun onScanFailed(errorCode: Int) {
                        cont.resumeWithException(FlipperException("BLE scan failed (error $errorCode)"))
                    }
                }

                val filter = ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(SERIAL_SERVICE_UUID))
                    .build()
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                scanner.startScan(listOf(filter), settings, callback)

                cont.invokeOnCancellation {
                    scanner.stopScan(callback)
                }
            }
        }
    }
}
