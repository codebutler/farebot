package com.codebutler.farebot.flipper

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
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

    private fun log(msg: String) {
        FlipperDebugLog.log(msg)
    }

    // Diagnostics — included in timeout error messages
    private var lastBleState: Long = -1
    private var scanStarted: Boolean = false
    private val discoveredDevices = mutableListOf<String>()

    override val isConnected: Boolean
        get() = connectedPeripheral != null

    override suspend fun connect() {
        log("connect() called, peripheral=${peripheral?.name}")
        val target = peripheral ?: scanForFlipper()
        log("connect() got target peripheral: ${target.name}, identifier=${target.identifier}")

        connectionDeferred = CompletableDeferred()
        servicesDeferred = CompletableDeferred()

        val manager = centralManager ?: CBCentralManager(delegate = centralDelegate, queue = null)
        centralManager = manager
        log("connect() centralManager state=${manager.state}")

        target.delegate = peripheralDelegate
        connectedPeripheral = target

        log("connect() calling connectPeripheral...")
        manager.connectPeripheral(target, options = null)

        log("connect() waiting for connection (timeout=${CONNECT_TIMEOUT_MS}ms)...")
        withTimeout(CONNECT_TIMEOUT_MS) {
            connectionDeferred!!.await()
        }
        log("connect() connected! Discovering services...")

        target.discoverServices(listOf(SERIAL_SERVICE_UUID))

        log("connect() waiting for service discovery (timeout=${CONNECT_TIMEOUT_MS}ms)...")
        withTimeout(CONNECT_TIMEOUT_MS) {
            servicesDeferred!!.await()
        }
        log(
            "connect() services discovered! write=$writeCharacteristic, read=$readCharacteristic, flowControl=$flowControlCharacteristic",
        )

        // Enable notifications on serial read characteristic (data from Flipper)
        val read =
            readCharacteristic
                ?: throw FlipperException("Serial read characteristic not found")
        notifyDeferred = CompletableDeferred()
        log("connect() enabling notifications on serial read...")
        target.setNotifyValue(true, forCharacteristic = read)

        withTimeout(CONNECT_TIMEOUT_MS) {
            notifyDeferred!!.await()
        }
        log("connect() notifications confirmed on serial read.")

        // Enable notifications on flow control characteristic and read initial value
        val fc = flowControlCharacteristic
        if (fc != null) {
            notifyDeferred = CompletableDeferred()
            log("connect() enabling notifications on flow control...")
            target.setNotifyValue(true, forCharacteristic = fc)
            withTimeout(CONNECT_TIMEOUT_MS) {
                notifyDeferred!!.await()
            }
            log("connect() flow control notifications enabled, reading initial value...")
            target.readValueForCharacteristic(fc)
        } else {
            log("connect() WARNING: flow control characteristic not found")
        }

        log("connect() Done!")
    }

    override suspend fun write(data: ByteArray) {
        val peripheral = connectedPeripheral ?: throw FlipperException("Not connected")
        val write = writeCharacteristic ?: throw FlipperException("Write characteristic not found")

        val hex = data.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        log("write(): ${data.size} bytes, hex=$hex")

        val nsData = data.toNSData()
        peripheral.writeValue(nsData, forCharacteristic = write, type = CBCharacteristicWriteWithoutResponse)
    }

    override suspend fun platformClose() {
        log("close() called")
        val peripheral = connectedPeripheral ?: return
        centralManager?.cancelPeripheralConnection(peripheral)
        connectedPeripheral = null
        writeCharacteristic = null
        readCharacteristic = null
        flowControlCharacteristic = null
    }

    private suspend fun scanForFlipper(): CBPeripheral {
        log("scanForFlipper() starting...")
        scanDeferred = CompletableDeferred()
        lastBleState = -1
        scanStarted = false
        discoveredDevices.clear()

        log("scanForFlipper() creating CBCentralManager...")
        val manager = CBCentralManager(delegate = centralDelegate, queue = null)
        centralManager = manager
        log("scanForFlipper() CBCentralManager created, initial state=${manager.state}")

        try {
            return withTimeout(SCAN_TIMEOUT_MS) {
                log(
                    "scanForFlipper() inside withTimeout, manager.state=${manager.state}, poweredOn=${manager.state == CBCentralManagerStatePoweredOn}",
                )
                // Wait for powered on state
                if (manager.state != CBCentralManagerStatePoweredOn) {
                    log("scanForFlipper() NOT powered on yet, delegate will start scan")
                    // Central delegate will start scan when powered on
                } else {
                    log("scanForFlipper() ALREADY powered on, starting scan immediately")
                }
                log("scanForFlipper() calling scanForPeripheralsWithServices(null)...")
                manager.scanForPeripheralsWithServices(
                    serviceUUIDs = null,
                    options = null,
                )
                scanStarted = true
                log("scanForFlipper() scan started, awaiting scanDeferred...")
                try {
                    scanDeferred!!.await()
                } finally {
                    log("scanForFlipper() stopping scan (finally block)")
                    manager.stopScan()
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            val deviceList =
                if (discoveredDevices.isEmpty()) {
                    "none"
                } else {
                    discoveredDevices.joinToString("; ")
                }
            log("scanForFlipper() TIMED OUT! BLE state=$lastBleState, scanStarted=$scanStarted, devices=$deviceList")
            throw FlipperException(
                "Flipper Zero not found (scan timed out). " +
                    "BLE state: $lastBleState, scan started: $scanStarted, " +
                    "devices seen: $deviceList",
            )
        }
    }

    private val centralDelegate =
        object : NSObject(), CBCentralManagerDelegateProtocol {
            override fun centralManagerDidUpdateState(central: CBCentralManager) {
                lastBleState = central.state
                log("centralManagerDidUpdateState: state=${central.state}")
                when (central.state) {
                    CBCentralManagerStatePoweredOn -> {
                        log(
                            "  -> PoweredOn! scanDeferred=${scanDeferred != null}, isCompleted=${scanDeferred?.isCompleted}",
                        )
                        if (scanDeferred != null && scanDeferred?.isCompleted == false) {
                            log("  -> Starting scan from delegate...")
                            central.scanForPeripheralsWithServices(
                                serviceUUIDs = null,
                                options = null,
                            )
                            scanStarted = true
                            log("  -> Scan started from delegate")
                        }
                    }
                    CBCentralManagerStateUnauthorized -> {
                        log("  -> Unauthorized!")
                        scanDeferred?.completeExceptionally(
                            FlipperException("Bluetooth permission denied. Enable Bluetooth access in Settings."),
                        )
                    }
                    CBCentralManagerStateUnsupported -> {
                        log("  -> Unsupported!")
                        scanDeferred?.completeExceptionally(
                            FlipperException("Bluetooth is not supported on this device."),
                        )
                    }
                    CBCentralManagerStatePoweredOff -> {
                        log("  -> PoweredOff!")
                        scanDeferred?.completeExceptionally(
                            FlipperException("Bluetooth is turned off. Enable it in Settings."),
                        )
                    }
                    CBCentralManagerStateResetting -> {
                        log("  -> Resetting, waiting...")
                    }
                    else -> {
                        log("  -> Unknown state: ${central.state}")
                    }
                }
            }

            override fun centralManager(
                central: CBCentralManager,
                didDiscoverPeripheral: CBPeripheral,
                advertisementData: Map<Any?, *>,
                RSSI: NSNumber,
            ) {
                val advName = advertisementData[CBAdvertisementDataLocalNameKey] as? String
                val periphName = didDiscoverPeripheral.name
                val name = advName ?: periphName
                val uuid = didDiscoverPeripheral.identifier.UUIDString()

                log("didDiscoverPeripheral: advName=$advName, periphName=$periphName, uuid=$uuid, RSSI=$RSSI")

                // Log all advertisement data keys for first few devices
                if (discoveredDevices.size < 10) {
                    val keys = advertisementData.keys.map { it.toString() }
                    log("  advertisementData keys: $keys")
                }

                // Capture all named devices for diagnostics
                if (name != null && discoveredDevices.size < 50) {
                    val entry = "$name (adv=${advName != null}, periph=${periphName != null})"
                    if (entry !in discoveredDevices) {
                        discoveredDevices.add(entry)
                        log("  NEW device: $entry (total: ${discoveredDevices.size})")
                    }
                }

                if (name != null && name.startsWith(FLIPPER_DEVICE_PREFIX, ignoreCase = true)) {
                    log("  FOUND FLIPPER! name=$name, stopping scan and completing deferred")
                    central.stopScan()
                    scanDeferred?.complete(didDiscoverPeripheral)
                }
            }

            override fun centralManager(
                central: CBCentralManager,
                didConnectPeripheral: CBPeripheral,
            ) {
                log("didConnectPeripheral: ${didConnectPeripheral.name}")
                connectionDeferred?.complete(Unit)
            }

            @ObjCSignatureOverride
            override fun centralManager(
                central: CBCentralManager,
                didFailToConnectPeripheral: CBPeripheral,
                error: NSError?,
            ) {
                log("didFailToConnectPeripheral: ${didFailToConnectPeripheral.name}, error=$error")
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
                log("didDisconnectPeripheral: ${didDisconnectPeripheral.name}, error=$error")
                connectedPeripheral = null
            }
        }

    private val peripheralDelegate =
        object : NSObject(), CBPeripheralDelegateProtocol {
            override fun peripheral(
                peripheral: CBPeripheral,
                didDiscoverServices: NSError?,
            ) {
                log("didDiscoverServices: error=$didDiscoverServices, services=${peripheral.services?.size}")
                if (didDiscoverServices != null) {
                    log("  Service discovery FAILED: ${didDiscoverServices.localizedDescription}")
                    servicesDeferred?.completeExceptionally(
                        FlipperException("Service discovery failed: ${didDiscoverServices.localizedDescription}"),
                    )
                    return
                }

                peripheral.services?.forEach { svc ->
                    val service = svc as? CBService
                    log("  Found service: ${service?.UUID}")
                }

                val service =
                    peripheral.services?.firstOrNull {
                        (it as? CBService)?.UUID == SERIAL_SERVICE_UUID
                    } as? CBService
                if (service != null) {
                    log("  Serial service FOUND! Discovering ALL characteristics...")
                    peripheral.discoverCharacteristics(
                        null, // discover ALL characteristics
                        forService = service,
                    )
                } else {
                    log("  Serial service NOT FOUND among ${peripheral.services?.size} services")
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
                log("didDiscoverCharacteristics: service=${didDiscoverCharacteristicsForService.UUID}, error=$error")
                if (error != null) {
                    servicesDeferred?.completeExceptionally(
                        FlipperException("Characteristic discovery failed: ${error.localizedDescription}"),
                    )
                    return
                }

                val characteristics = didDiscoverCharacteristicsForService.characteristics ?: return
                log("  Found ${characteristics.size} characteristics")
                for (char in characteristics) {
                    val characteristic = char as? CBCharacteristic ?: continue
                    log("  Characteristic: ${characteristic.UUID}")
                    when (characteristic.UUID) {
                        SERIAL_READ_UUID -> {
                            readCharacteristic = characteristic
                            log("    -> Serial read characteristic set")
                        }
                        SERIAL_WRITE_UUID -> {
                            writeCharacteristic = characteristic
                            log("    -> Serial write characteristic set")
                        }
                        SERIAL_FLOW_CONTROL_UUID -> {
                            flowControlCharacteristic = characteristic
                            log("    -> Flow control characteristic set")
                        }
                    }
                }

                log("  Completing servicesDeferred")
                servicesDeferred?.complete(Unit)
            }

            @ObjCSignatureOverride
            override fun peripheral(
                peripheral: CBPeripheral,
                didUpdateNotificationStateForCharacteristic: CBCharacteristic,
                error: NSError?,
            ) {
                log(
                    "didUpdateNotificationState: char=${didUpdateNotificationStateForCharacteristic.UUID}, isNotifying=${didUpdateNotificationStateForCharacteristic.isNotifying()}, error=$error",
                )
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
                log("didWriteValueForCharacteristic: char=${didWriteValueForCharacteristic.UUID}, error=$error")
            }

            @ObjCSignatureOverride
            override fun peripheral(
                peripheral: CBPeripheral,
                didUpdateValueForCharacteristic: CBCharacteristic,
                error: NSError?,
            ) {
                if (error != null) {
                    log(
                        "didUpdateValueForCharacteristic ERROR: char=${didUpdateValueForCharacteristic.UUID}, error=$error",
                    )
                    return
                }
                val nsData = didUpdateValueForCharacteristic.value
                log(
                    "didUpdateValueForCharacteristic: char=${didUpdateValueForCharacteristic.UUID}, dataLen=${nsData?.length}",
                )
                when (didUpdateValueForCharacteristic.UUID) {
                    SERIAL_READ_UUID -> {
                        if (nsData == null) return
                        val bytes = nsData.toByteArray()
                        log("  -> Serial read data: ${bytes.size} bytes")
                        if (bytes.isNotEmpty()) {
                            onDataReceived(bytes)
                        }
                    }
                    SERIAL_FLOW_CONTROL_UUID -> {
                        if (nsData != null) {
                            val bytes = nsData.toByteArray()
                            val freeSpace = parseFlowControl(bytes)
                            if (freeSpace != null) {
                                log("  -> Flow control: freeSpace=$freeSpace bytes")
                            } else {
                                log("  -> Flow control: unexpected data length=${nsData.length}")
                            }
                        } else {
                            log("  -> Flow control: unexpected data length=null")
                        }
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
