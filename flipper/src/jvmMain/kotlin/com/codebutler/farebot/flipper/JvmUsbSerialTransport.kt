package com.codebutler.farebot.flipper

import com.fazecast.jSerialComm.SerialPort

/**
 * FlipperTransport implementation using jSerialComm for Desktop JVM.
 * Finds and connects to the Flipper Zero's CDC virtual serial port.
 */
class JvmUsbSerialTransport(
    private val portDescriptor: String? = null,
) : FlipperTransport {
    companion object {
        private const val FLIPPER_VID = 0x0483
        private const val FLIPPER_PID = 0x5740
        private const val BAUD_RATE = 230400
        private const val READ_TIMEOUT_MS = 5000
    }

    private var serialPort: SerialPort? = null

    override val isConnected: Boolean
        get() = serialPort?.isOpen == true

    override val requiresRpcSessionInit: Boolean get() = true

    override suspend fun connect() {
        val port =
            if (portDescriptor != null) {
                SerialPort.getCommPort(portDescriptor)
            } else {
                findFlipperPort()
                    ?: throw FlipperException("Flipper Zero not found. Is it connected via USB?")
            }

        port.baudRate = BAUD_RATE
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0)

        if (!port.openPort()) {
            throw FlipperException("Failed to open serial port: ${port.systemPortName}")
        }

        serialPort = port
    }

    override suspend fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        val port = serialPort ?: throw FlipperException("Not connected")
        val tempBuffer = ByteArray(length)
        val bytesRead = port.readBytes(tempBuffer, length)
        if (bytesRead <= 0) {
            throw FlipperException("Serial read failed or timed out")
        }
        tempBuffer.copyInto(buffer, offset, 0, bytesRead)
        return bytesRead
    }

    override suspend fun write(data: ByteArray) {
        val port = serialPort ?: throw FlipperException("Not connected")
        val written = port.writeBytes(data, data.size)
        if (written < 0) {
            throw FlipperException("Serial write failed")
        }
        if (written < data.size) {
            throw FlipperException("Serial write incomplete: wrote $written of ${data.size} bytes")
        }
    }

    override suspend fun close() {
        serialPort?.closePort()
        serialPort = null
    }

    private fun findFlipperPort(): SerialPort? =
        SerialPort.getCommPorts().firstOrNull { port ->
            port.vendorID == FLIPPER_VID && port.productID == FLIPPER_PID
        }
}
