/*
 * FeliCaTagAdapter.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.felica

/**
 * Platform adapter for FeliCa NFC-F tag communication.
 *
 * Each platform (Android, iOS) provides an implementation that wraps
 * the native NFC API. The shared [FeliCaReader] uses this interface
 * to execute the card-reading algorithm.
 */
interface FeliCaTagAdapter {
    /** Returns the 8-byte IDm from the tag. */
    fun getIDm(): ByteArray

    /** Returns the list of system codes reported by the tag. */
    fun getSystemCodes(): List<Int>

    /** Polls the tag with [systemCode] and returns the 8-byte PMm, or null on failure. */
    fun selectSystem(systemCode: Int): ByteArray?

    /** Returns the list of service codes for the currently-selected system. */
    fun getServiceCodes(): List<Int>

    /** Reads a single 16-byte block from [serviceCode] at [blockAddr], or null on failure. */
    fun readBlock(serviceCode: Int, blockAddr: Byte): ByteArray?
}
