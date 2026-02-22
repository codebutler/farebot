package com.codebutler.farebot.flipper

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerStatePoweredOff
import platform.CoreBluetooth.CBCentralManagerStatePoweredOn
import platform.CoreBluetooth.CBCentralManagerStateResetting
import platform.CoreBluetooth.CBCentralManagerStateUnauthorized
import platform.CoreBluetooth.CBCentralManagerStateUnsupported
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicWriteWithoutResponse
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.dataWithBytes
import platform.darwin.NSObject
import platform.posix.memcpy

/**
 * FlipperTransport implementation using iOS Core Bluetooth.
 * Connects to Flipper Zero's BLE Serial service.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBleSerialTransport(
    private val peripheral: CBPeripheral? = null,
) : FlipperBleTransportBase() {
    companion object {
        val SERIAL_SERVICE_UUID: CBUUID = CBUUID.UUIDWithString(SERIAL_SERVICE_UUID_STRING)
        val SERIAL_READ_UUID: CBUUID = CBUUID.UUIDWithString(SERIAL_READ_UUID_STRING)
        val SERIAL_WRITE_UUID: CBUUID = CBUUID.UUIDWithString(SERIAL_WRITE_UUID_STRING)
        val SERIAL_FLOW_CONTROL_UUID: CBUUID = CBUUID.UUIDWithString(SERIAL_FLOW_CONTROL_UUID_STRING)
        val FLIPPER_ADV_CBUUIDS: List<CBUUID> = FLIPPER_ADV_SERVICE_UUIDS.map { CBUUID.UUIDWithString(it) }
    }

    private var centralManager: CBCentralManager? = null
    private var connectedPeripheral: CBPeripheral? = null
    private var writeCharacteristic: CBCharacteristic? = null
    private var readCharacteristic: CBCharacteristic? = null
    private var flowControlCharacteristic: CBCharacteristic? = null

    private var connectionDeferred: CompletableDeferred<Unit>? = null
    private var servicesDeferred: CompletableDeferred<Unit>? = null
    private var notifyDeferred: CompletableDeferred<Unit>? = null
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

        // Enable notifications on serial read characteristic (data from Flipper)
        val read =
            readCharacteristic
                ?: throw FlipperException("Serial read characteristic not found")
        notifyDeferred = CompletableDeferred()
        target.setNotifyValue(true, forCharacteristic = read)

        withTimeout(CONNECT_TIMEOUT_MS) {
            notifyDeferred!!.await()
        }

        // Enable notifications on flow control characteristic and read initial value
        val fc = flowControlCharacteristic
        if (fc != null) {
            notifyDeferred = CompletableDeferred()
            target.setNotifyValue(true, forCharacteristic = fc)
            withTimeout(CONNECT_TIMEOUT_MS) {
                notifyDeferred!!.await()
            }
            target.readValueForCharacteristic(fc)
        }
    }

    override suspend fun write(data: ByteArray) {
        val peripheral = connectedPeripheral ?: throw FlipperException("Not connected")
        val write = writeCharacteristic ?: throw FlipperException("Write characteristic not found")

        val nsData = data.toNSData()
        peripheral.writeValue(nsData, forCharacteristic = write, type = CBCharacteristicWriteWithoutResponse)
    }

    override suspend fun platformClose() {
        val peripheral = connectedPeripheral ?: return
        centralManager?.cancelPeripheralConnection(peripheral)
        connectedPeripheral = null
        writeCharacteristic = null
        readCharacteristic = null
        flowControlCharacteristic = null
    }

    private suspend fun scanForFlipper(): CBPeripheral {
        scanDeferred = CompletableDeferred()

        val manager = CBCentralManager(delegate = centralDelegate, queue = null)
        centralManager = manager

        try {
            return withTimeout(SCAN_TIMEOUT_MS) {
                manager.scanForPeripheralsWithServices(
                    serviceUUIDs = FLIPPER_ADV_CBUUIDS,
                    options = null,
                )
                try {
                    scanDeferred!!.await()
                } finally {
                    manager.stopScan()
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw FlipperException("Flipper Zero not found (scan timed out)")
        }
    }

    private val centralDelegate =
        object : NSObject(), CBCentralManagerDelegateProtocol {
            override fun centralManagerDidUpdateState(central: CBCentralManager) {
                when (central.state) {
                    CBCentralManagerStatePoweredOn -> {
                        if (scanDeferred != null && scanDeferred?.isCompleted == false) {
                            central.scanForPeripheralsWithServices(
                                serviceUUIDs = FLIPPER_ADV_CBUUIDS,
                                options = null,
                            )
                        }
                    }
                    CBCentralManagerStateUnauthorized -> {
                        scanDeferred?.completeExceptionally(
                            FlipperException("Bluetooth permission denied. Enable Bluetooth access in Settings."),
                        )
                    }
                    CBCentralManagerStateUnsupported -> {
                        scanDeferred?.completeExceptionally(
                            FlipperException("Bluetooth is not supported on this device."),
                        )
                    }
                    CBCentralManagerStatePoweredOff -> {
                        scanDeferred?.completeExceptionally(
                            FlipperException("Bluetooth is turned off. Enable it in Settings."),
                        )
                    }
                    CBCentralManagerStateResetting -> {}
                    else -> {}
                }
            }

            override fun centralManager(
                central: CBCentralManager,
                didDiscoverPeripheral: CBPeripheral,
                advertisementData: Map<Any?, *>,
                RSSI: NSNumber,
            ) {
                central.stopScan()
                scanDeferred?.complete(didDiscoverPeripheral)
            }

            override fun centralManager(
                central: CBCentralManager,
                didConnectPeripheral: CBPeripheral,
            ) {
                connectionDeferred?.complete(Unit)
            }

            @ObjCSignatureOverride
            override fun centralManager(
                central: CBCentralManager,
                didFailToConnectPeripheral: CBPeripheral,
                error: NSError?,
            ) {
                connectionDeferred?.completeExceptionally(
                    FlipperException("BLE connection failed: ${error?.localizedDescription}"),
                )
            }

            @ObjCSignatureOverride
            override fun centralManager(
                central: CBCentralManager,
                didDisconnectPeripheral: CBPeripheral,
                error: NSError?,
            ) {
                connectedPeripheral = null
            }
        }

    private val peripheralDelegate =
        object : NSObject(), CBPeripheralDelegateProtocol {
            override fun peripheral(
                peripheral: CBPeripheral,
                didDiscoverServices: NSError?,
            ) {
                if (didDiscoverServices != null) {
                    servicesDeferred?.completeExceptionally(
                        FlipperException("Service discovery failed: ${didDiscoverServices.localizedDescription}"),
                    )
                    return
                }

                val service =
                    peripheral.services?.firstOrNull {
                        (it as? CBService)?.UUID == SERIAL_SERVICE_UUID
                    } as? CBService
                if (service != null) {
                    peripheral.discoverCharacteristics(
                        null,
                        forService = service,
                    )
                } else {
                    servicesDeferred?.completeExceptionally(
                        FlipperException("Serial service not found"),
                    )
                }
            }

            @ObjCSignatureOverride
            override fun peripheral(
                peripheral: CBPeripheral,
                didDiscoverCharacteristicsForService: CBService,
                error: NSError?,
            ) {
                if (error != null) {
                    servicesDeferred?.completeExceptionally(
                        FlipperException("Characteristic discovery failed: ${error.localizedDescription}"),
                    )
                    return
                }

                val characteristics = didDiscoverCharacteristicsForService.characteristics ?: return
                for (char in characteristics) {
                    val characteristic = char as? CBCharacteristic ?: continue
                    when (characteristic.UUID) {
                        SERIAL_READ_UUID -> readCharacteristic = characteristic
                        SERIAL_WRITE_UUID -> writeCharacteristic = characteristic
                        SERIAL_FLOW_CONTROL_UUID -> flowControlCharacteristic = characteristic
                    }
                }

                servicesDeferred?.complete(Unit)
            }

            @ObjCSignatureOverride
            override fun peripheral(
                peripheral: CBPeripheral,
                didUpdateNotificationStateForCharacteristic: CBCharacteristic,
                error: NSError?,
            ) {
                if (error != null) {
                    notifyDeferred?.completeExceptionally(
                        FlipperException("Failed to enable notifications: ${error.localizedDescription}"),
                    )
                } else {
                    notifyDeferred?.complete(Unit)
                }
            }

            @ObjCSignatureOverride
            override fun peripheral(
                peripheral: CBPeripheral,
                didWriteValueForCharacteristic: CBCharacteristic,
                error: NSError?,
            ) {
            }

            @ObjCSignatureOverride
            override fun peripheral(
                peripheral: CBPeripheral,
                didUpdateValueForCharacteristic: CBCharacteristic,
                error: NSError?,
            ) {
                if (error != null) return
                val nsData = didUpdateValueForCharacteristic.value ?: return
                when (didUpdateValueForCharacteristic.UUID) {
                    SERIAL_READ_UUID -> {
                        val bytes = nsData.toByteArray()
                        if (bytes.isNotEmpty()) {
                            onDataReceived(bytes)
                        }
                    }
                    SERIAL_FLOW_CONTROL_UUID -> {
                        // Flow control value received; not yet used for write throttling.
                    }
                }
            }
        }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
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
