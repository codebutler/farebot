/*
 * PN533Device.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.card.nfc.pn533

import org.usb4java.Context
import org.usb4java.DeviceHandle
import org.usb4java.DeviceList
import org.usb4java.LibUsb

/**
 * USB device discovery for PN533 NFC controllers.
 *
 * Enumerates USB devices via usb4java, matches known VID/PID pairs,
 * opens the device handle, and claims the bulk interface.
 */
object PN533Device {
    private val KNOWN_DEVICES =
        listOf(
            UsbId(0x04CC, 0x2533), // NXP PN533 (SCM SCL3711, etc.)
            UsbId(0x04E6, 0x5591), // SCM SCL3711 (alternate PID)
            UsbId(0x054C, 0x02E1), // Sony RC-S380 (PN533 variant)
        )

    private var context: Context? = null

    fun open(): PN533Transport? {
        val ctx = Context()
        val result = LibUsb.init(ctx)
        if (result != LibUsb.SUCCESS) {
            return null
        }

        val deviceList = DeviceList()
        val count = LibUsb.getDeviceList(ctx, deviceList)
        if (count < 0) {
            LibUsb.exit(ctx)
            return null
        }

        try {
            for (device in deviceList) {
                val descriptor = org.usb4java.DeviceDescriptor()
                if (LibUsb.getDeviceDescriptor(device, descriptor) != LibUsb.SUCCESS) continue

                val vid = descriptor.idVendor().toInt() and 0xFFFF
                val pid = descriptor.idProduct().toInt() and 0xFFFF

                if (KNOWN_DEVICES.none { it.vendorId == vid && it.productId == pid }) continue

                val handle = DeviceHandle()
                if (LibUsb.open(device, handle) != LibUsb.SUCCESS) continue

                // Detach kernel driver if active (required on Linux, safe no-op on macOS)
                if (LibUsb.kernelDriverActive(handle, 0) == 1) {
                    LibUsb.detachKernelDriver(handle, 0)
                }

                val claimResult = LibUsb.claimInterface(handle, 0)
                if (claimResult != LibUsb.SUCCESS) {
                    LibUsb.close(handle)
                    continue
                }

                context = ctx
                return PN533Transport(handle)
            }
        } finally {
            LibUsb.freeDeviceList(deviceList, true)
        }

        LibUsb.exit(ctx)
        return null
    }

    fun shutdown() {
        context?.let { LibUsb.exit(it) }
        context = null
    }

    private data class UsbId(
        val vendorId: Int,
        val productId: Int,
    )
}
