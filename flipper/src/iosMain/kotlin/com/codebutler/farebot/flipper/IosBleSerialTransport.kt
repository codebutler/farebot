package com.codebutler.farebot.flipper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerStatePoweredOn
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicWriteWithResponse
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import platform.Foundation.create

/**
 * FlipperTransport implementation using iOS Core Bluetooth.
 * Connects to Flipper Zero's BLE Serial service.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBleSerialTransport(
    private val peripheral: CBPeripheral? = null,
) : FlipperTransport {
    companion object {
        val SERIAL_SERVICE_UUID: CBUUID = CBUUID.UUIDWithString("8fe5b3d5-2e7f-4a98-2a48-7acc60fe0000")
        val SERIAL_RX_UUID: CBUUID = CBUUID.UUIDWithString("19ed82ae-ed21-4c9d-4145-228e62fe0000")
        val SERIAL_TX_UUID: CBUUID = CBUUID.UUIDWithString("19ed82ae-ed21-4c9d-4145-228e63fe0000")
        private const val SCAN_TIMEOUT_MS = 15_000L
        private const val CONNECT_TIMEOUT_MS = 10_000L
    }

    private var centralManager: CBCentralManager? = null
    private var connectedPeripheral: CBPeripheral? = null
    private var rxCharacteristic: CBCharacteristic? = null
    private var txCharacteristic: CBCharacteristic? = null
    private val receiveChannel = Channel<ByteArray>(Channel.UNLIMITED)

    private var connectionDeferred: CompletableDeferred<Unit>? = null
    private var servicesDeferred: CompletableDeferred<Unit>? = null
    private var scanDeferred: CompletableDeferred<CBPeripheral>? = null

    override val isConnected: Boolean
        get() = connectedPeripheral != null

    override suspend fun connect() {
        val target = peripheral ?: scanForFlipper()

        connectionDeferred = CompletableDeferred()
        servicesDeferred = CompletableDeferred()

        val manager = centralManager ?: CBCentralManager(delegate = centralDelegate, queue = null)
        centralManager = manager

        target.delegate = peripheralDelegate
        connectedPeripheral = target

        manager.connectPeripheral(target, options = null)

        withTimeout(CONNECT_TIMEOUT_MS) {
            connectionDeferred!!.await()
        }

        target.discoverServices(listOf(SERIAL_SERVICE_UUID))

        withTimeout(CONNECT_TIMEOUT_MS) {
            servicesDeferred!!.await()
        }

        // Enable notifications on TX characteristic
        val tx = txCharacteristic
            ?: throw FlipperException("TX characteristic not found")
        target.setNotifyValue(true, forCharacteristic = tx)
    }

    override suspend fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val data = receiveChannel.receive()
        val bytesToCopy = minOf(data.size, length)
        data.copyInto(buffer, offset, 0, bytesToCopy)
        return bytesToCopy
    }

    override suspend fun write(data: ByteArray) {
        val peripheral = connectedPeripheral ?: throw FlipperException("Not connected")
        val rx = rxCharacteristic ?: throw FlipperException("RX characteristic not found")

        val nsData = data.toNSData()
        peripheral.writeValue(nsData, forCharacteristic = rx, type = CBCharacteristicWriteWithResponse)
    }

    override suspend fun close() {
        val peripheral = connectedPeripheral ?: return
        centralManager?.cancelPeripheralConnection(peripheral)
        connectedPeripheral = null
        rxCharacteristic = null
        txCharacteristic = null
        receiveChannel.close()
    }

    private suspend fun scanForFlipper(): CBPeripheral {
        scanDeferred = CompletableDeferred()

        val manager = CBCentralManager(delegate = centralDelegate, queue = null)
        centralManager = manager

        return withTimeout(SCAN_TIMEOUT_MS) {
            // Wait for powered on state
            if (manager.state != CBCentralManagerStatePoweredOn) {
                // Central delegate will start scan when powered on
            }
            manager.scanForPeripheralsWithServices(
                serviceUUIDs = listOf(SERIAL_SERVICE_UUID),
                options = null,
            )
            try {
                scanDeferred!!.await()
            } finally {
                manager.stopScan()
            }
        }
    }

    private val centralDelegate = object : NSObject(), CBCentralManagerDelegateProtocol {
        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            if (central.state == CBCentralManagerStatePoweredOn) {
                if (scanDeferred != null && scanDeferred?.isCompleted == false) {
                    central.scanForPeripheralsWithServices(
                        serviceUUIDs = listOf(SERIAL_SERVICE_UUID),
                        options = null,
                    )
                }
            }
        }

        override fun centralManager(
            central: CBCentralManager,
            didDiscoverPeripheral: CBPeripheral,
            advertisementData: Map<Any?, *>,
            RSSI: NSNumber,
        ) {
            scanDeferred?.complete(didDiscoverPeripheral)
        }

        override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
            connectionDeferred?.complete(Unit)
        }

        override fun centralManager(
            central: CBCentralManager,
            didFailToConnectPeripheral: CBPeripheral,
            error: NSError?,
        ) {
            connectionDeferred?.completeExceptionally(
                FlipperException("BLE connection failed: ${error?.localizedDescription}"),
            )
        }

        override fun centralManager(
            central: CBCentralManager,
            didDisconnectPeripheral: CBPeripheral,
            error: NSError?,
        ) {
            connectedPeripheral = null
        }
    }

    private val peripheralDelegate = object : NSObject(), CBPeripheralDelegateProtocol {
        override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
            if (didDiscoverServices != null) {
                servicesDeferred?.completeExceptionally(
                    FlipperException("Service discovery failed: ${didDiscoverServices.localizedDescription}"),
                )
                return
            }

            val service = peripheral.services?.firstOrNull { (it as? CBService)?.UUID == SERIAL_SERVICE_UUID } as? CBService
            if (service != null) {
                peripheral.discoverCharacteristics(
                    listOf(SERIAL_RX_UUID, SERIAL_TX_UUID),
                    forService = service,
                )
            } else {
                servicesDeferred?.completeExceptionally(
                    FlipperException("Serial service not found"),
                )
            }
        }

        override fun peripheral(peripheral: CBPeripheral, didDiscoverCharacteristicsForService: CBService, error: NSError?) {
            if (error != null) {
                servicesDeferred?.completeExceptionally(
                    FlipperException("Characteristic discovery failed: ${error.localizedDescription}"),
                )
                return
            }

            val characteristics = didDiscoverCharacteristicsForService.characteristics ?: emptyList<CBCharacteristic>()
            for (char in characteristics) {
                val characteristic = char as? CBCharacteristic ?: continue
                when (characteristic.UUID) {
                    SERIAL_RX_UUID -> rxCharacteristic = characteristic
                    SERIAL_TX_UUID -> txCharacteristic = characteristic
                }
            }

            servicesDeferred?.complete(Unit)
        }

        override fun peripheral(peripheral: CBPeripheral, didUpdateValueForCharacteristic: CBCharacteristic, error: NSError?) {
            if (error != null) return
            if (didUpdateValueForCharacteristic.UUID == SERIAL_TX_UUID) {
                val nsData = didUpdateValueForCharacteristic.value ?: return
                val bytes = nsData.toByteArray()
                if (bytes.isNotEmpty()) {
                    receiveChannel.trySend(bytes)
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    if (isEmpty()) return NSData()
    usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return byteArrayOf()
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this@toByteArray.bytes, length)
    }
    return bytes
}
