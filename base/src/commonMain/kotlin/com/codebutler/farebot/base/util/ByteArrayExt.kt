/*
 * ByteArrayExt.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.base.util

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// --- Hex / Base64 ---

fun ByteArray.hex(): String = ByteUtils.getHexString(this)

fun Int.hexByte(): String {
    val v = this and 0xFF
    return "${ByteUtils.HEX_CHARS[v ushr 4]}${ByteUtils.HEX_CHARS[v and 0x0F]}"
}

fun ByteArray.getHexString(
    offset: Int,
    length: Int,
): String = ByteUtils.getHexString(this.copyOfRange(offset, offset + length))

fun ByteArray.toHexDump(): String = joinToString(" ") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

@OptIn(ExperimentalEncodingApi::class)
fun ByteArray.toBase64(): String = Base64.encode(this)

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeBase64(): ByteArray = Base64.decode(this)

// --- Bit-level reading (big-endian) ---

/**
 * Reads bits from a byte array in big-endian bit order.
 * Ported from Metrodroid's ImmutableByteArray.getBitsFromBuffer.
 */
fun ByteArray.getBitsFromBuffer(
    iStartBit: Int,
    iLength: Int,
): Int = ByteUtils.getBitsFromBuffer(this, iStartBit, iLength)

/**
 * Reads bits from a byte array in little-endian bit order.
 * Ported from Metrodroid's ImmutableByteArray.getBitsFromBufferLeBits.
 */
fun ByteArray.getBitsFromBufferLeBits(
    iStartBit: Int,
    iLength: Int,
): Int {
    val iEndBit = iStartBit + iLength - 1
    val iSByte = iStartBit / 8
    val iSBit = iStartBit % 8
    val iEByte = iEndBit / 8
    val iEBit = iEndBit % 8

    if (iSByte == iEByte) {
        return (this[iEByte].toInt() shr iSBit) and (0xFF shr (8 - iLength))
    }

    var uRet = (this[iSByte].toInt() shr iSBit) and (0xFF shr iSBit)

    for (i in (iSByte + 1) until iEByte) {
        val t = ((this[i].toInt() and 0xFF) shl (((i - iSByte) * 8) - iSBit))
        uRet = uRet or t
    }

    val t = (this[iEByte].toInt() and ((1 shl (iEBit + 1)) - 1)) shl (((iEByte - iSByte) * 8) - iSBit)
    uRet = uRet or t

    return uRet
}

/**
 * Reads bits from a byte array in big-endian bit order, returning a signed value
 * using two's complement.
 */
fun ByteArray.getBitsFromBufferSigned(
    iStartBit: Int,
    iLength: Int,
): Int {
    val unsigned = getBitsFromBuffer(iStartBit, iLength)
    return unsignedToTwoComplement(unsigned, iLength - 1)
}

/**
 * Reads bits from a byte array in little-endian bit order, returning a signed value
 * using two's complement.
 */
fun ByteArray.getBitsFromBufferSignedLeBits(
    iStartBit: Int,
    iLength: Int,
): Int {
    val unsigned = getBitsFromBufferLeBits(iStartBit, iLength)
    return unsignedToTwoComplement(unsigned, iLength - 1)
}

private fun unsignedToTwoComplement(
    input: Int,
    highestBit: Int,
): Int =
    if (((input shr highestBit) and 1) == 1) {
        input - (2 shl highestBit)
    } else {
        input
    }

// --- Byte array to integer/long ---

fun ByteArray.byteArrayToInt(
    offset: Int,
    length: Int,
): Int = ByteUtils.byteArrayToInt(this, offset, length)

fun ByteArray.byteArrayToInt(): Int = ByteUtils.byteArrayToInt(this, 0, size)

fun ByteArray.byteArrayToLong(
    offset: Int,
    length: Int,
): Long = ByteUtils.byteArrayToLong(this, offset, length)

fun ByteArray.byteArrayToLong(): Long = ByteUtils.byteArrayToLong(this, 0, size)

fun ByteArray.byteArrayToIntReversed(
    offset: Int,
    length: Int,
): Int = byteArrayToLongReversed(offset, length).toInt()

fun ByteArray.byteArrayToIntReversed(): Int = byteArrayToIntReversed(0, size)

fun ByteArray.byteArrayToLongReversed(
    offset: Int,
    length: Int,
): Long =
    ByteUtils.byteArrayToLong(
        ByteArray(length) { this[offset + length - 1 - it] },
        0,
        length,
    )

fun ByteArray.byteArrayToLongReversed(): Long = byteArrayToLongReversed(0, size)

// --- Slicing ---

fun ByteArray.sliceOffLen(
    offset: Int,
    length: Int,
): ByteArray = copyOfRange(offset, offset + length)

fun ByteArray.sliceOffLenSafe(
    offset: Int,
    length: Int,
): ByteArray? {
    if (offset < 0 || length < 0 || offset > size) return null
    val safeLen = minOf(length, size - offset)
    if (safeLen == 0) return byteArrayOf()
    return sliceOffLen(offset, safeLen)
}

// --- Validation ---

fun ByteArray.isAllZero(): Boolean = all { it == 0.toByte() }

fun ByteArray.isAllFF(): Boolean = all { it == 0xFF.toByte() }

fun ByteArray.isASCII(): Boolean =
    all {
        (it in 0x20..0x7f) || it == 0x0d.toByte() || it == 0x0a.toByte()
    }

// --- Text decoding ---

fun ByteArray.readASCII(): String = readLatin1()

fun ByteArray.readLatin1(): String =
    map { (it.toInt() and 0xFF).toChar() }
        .filter { it != 0.toChar() }
        .toCharArray()
        .concatToString()

fun ByteArray.readUTF8(
    start: Int = 0,
    end: Int = size,
): String = decodeToString(startIndex = start, endIndex = end)

fun ByteArray.readUTF16(
    isLittleEndian: Boolean,
    start: Int = 0,
    end: Int = size,
): String {
    val ret =
        if (isLittleEndian) {
            CharArray((end - start) / 2) { byteArrayToIntReversed(start + it * 2, 2).toChar() }
                .concatToString()
        } else {
            CharArray((end - start) / 2) { byteArrayToInt(start + it * 2, 2).toChar() }
                .concatToString()
        }

    if ((end - start) % 2 != 0) {
        return ret + "\uFFFD"
    }
    return ret
}

fun ByteArray.readUTF16BOM(
    isLittleEndianDefault: Boolean,
    start: Int = 0,
    end: Int = size,
): String {
    if (end < start + 2) {
        return "\uFFFD"
    }
    return when (byteArrayToInt(start, 2)) {
        0xFEFF -> readUTF16(isLittleEndian = false, start = 2 + start, end = end)
        0xFFFE -> readUTF16(isLittleEndian = true, start = 2 + start, end = end)
        else -> readUTF16(isLittleEndian = isLittleEndianDefault, start = start, end = end)
    }
}

// --- BCD ---

fun ByteArray.convertBCDtoInteger(): Int =
    fold(0) { x, y ->
        (x * 100) + NumberUtils.convertBCDtoInteger(y)
    }

fun ByteArray.convertBCDtoInteger(
    offset: Int,
    length: Int,
): Int = sliceOffLen(offset, length).convertBCDtoInteger()

fun ByteArray.convertBCDtoLong(): Long =
    fold(0L) { x, y ->
        (x * 100L) + NumberUtils.convertBCDtoInteger(y).toLong()
    }

fun ByteArray.convertBCDtoLong(
    offset: Int,
    length: Int,
): Long = sliceOffLen(offset, length).convertBCDtoLong()

// --- Buffer manipulation ---

fun ByteArray.reverseBuffer(): ByteArray = ByteArray(size) { this[size - it - 1] }

fun ByteArray.startsWith(other: ByteArray): Boolean =
    size >= other.size && copyOfRange(0, other.size).contentEquals(other)

// --- Search ---

/**
 * Finds the first index starting from [start] where [predicate] is true.
 * Returns -1 if no such index exists.
 */
inline fun ByteArray.indexOfFirstStarting(
    start: Int,
    predicate: (Byte) -> Boolean,
): Int {
    for (i in start until size) {
        if (predicate(this[i])) return i
    }
    return -1
}

fun ByteArray.indexOf(
    needle: ByteArray,
    start: Int = 0,
    end: Int = size,
): Int {
    val needleSize = needle.size

    if (start < 0 ||
        start > lastIndex ||
        end > size ||
        start > end ||
        start > end - needleSize
    ) {
        return -1
    }

    if (needle.isEmpty()) {
        return start
    }

    return (start..(end - needleSize)).firstOrNull { off ->
        (0 until needleSize).all { p -> this[off + p] == needle[p] }
    } ?: -1
}
