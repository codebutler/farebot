package com.codebutler.farebot.base.util

object ByteUtils {
    fun getHexString(b: ByteArray): String {
        val sb = StringBuilder(b.size * 2)
        for (byte in b) {
            val v = byte.toInt() and 0xFF
            sb.append(HEX_CHARS[v ushr 4])
            sb.append(HEX_CHARS[v and 0x0F])
        }
        return sb.toString()
    }

    fun getHexString(
        b: ByteArray,
        @Suppress("UNUSED_PARAMETER") defaultResult: String,
    ): String = getHexString(b)

    fun hexStringToByteArray(s: String): ByteArray {
        if (s.length % 2 != 0) {
            throw IllegalArgumentException("Bad input string: $s")
        }

        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((s[i].digitToInt(16) shl 4) + s[i + 1].digitToInt(16)).toByte()
            i += 2
        }
        return data
    }

    fun intToByteArray(value: Int): ByteArray =
        byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte(),
        )

    fun byteArrayToInt(b: ByteArray): Int = byteArrayToInt(b, 0)

    private fun byteArrayToInt(
        b: ByteArray,
        offset: Int,
    ): Int = byteArrayToInt(b, offset, b.size)

    fun byteArrayToInt(
        b: ByteArray,
        offset: Int,
        length: Int,
    ): Int = byteArrayToLong(b, offset, length).toInt()

    fun byteArrayToLong(b: ByteArray): Long = byteArrayToLong(b, 0, b.size)

    fun byteArrayToLong(
        b: ByteArray,
        offset: Int,
        length: Int,
    ): Long {
        if (b.size < offset + length) {
            throw IllegalArgumentException("offset + length must be less than or equal to b.length")
        }

        var value: Long = 0
        for (i in 0 until length) {
            val shift = (length - 1 - i) * 8
            value += (b[i + offset].toInt() and 0x000000FF).toLong() shl shift
        }
        return value
    }

    fun byteArraySlice(
        b: ByteArray,
        offset: Int,
        length: Int,
    ): ByteArray = b.copyOfRange(offset, offset + length)

    fun convertBCDtoInteger(data: Byte): Int = (((data.toInt() and 0xF0) shr 4) * 10) + (data.toInt() and 0x0F)

    fun getBitsFromInteger(
        buffer: Int,
        iStartBit: Int,
        iLength: Int,
    ): Int = (buffer shr iStartBit) and (0xFF shr (8 - iLength))

    /**
     * Reverses a byte array, such that the last byte is first, and the first byte is last.
     *
     * @param buffer     Source buffer to reverse
     * @param iStartByte Start position in the buffer to read from
     * @param iLength    Number of bytes to read
     * @return A new byte array, of length iLength, with the bytes reversed
     */
    fun reverseBuffer(
        buffer: ByteArray,
        iStartByte: Int,
        iLength: Int,
    ): ByteArray {
        val reversed = ByteArray(iLength)
        val iEndByte = iStartByte + iLength
        for (x in 0 until iLength) {
            reversed[x] = buffer[iEndByte - x - 1]
        }
        return reversed
    }

    /**
     * Given an unsigned integer value, calculate the two's complement of the value if it is
     * actually a negative value
     *
     * @param input      Input value to convert
     * @param highestBit The position of the highest bit in the number, 0-indexed.
     * @return A signed integer containing it's converted value.
     */
    fun unsignedToTwoComplement(
        input: Int,
        highestBit: Int,
    ): Int {
        var inp = input
        if (getBitsFromInteger(inp, highestBit, 1) == 1) {
            // inverse all bits
            inp = inp xor ((2 shl highestBit) - 1)
            return -(1 + inp)
        }

        return inp
    }

    // Based on function from mfocGUI by 'Huuf' (http://www.huuf.info/OV/)
    fun getBitsFromBuffer(
        buffer: ByteArray,
        iStartBit: Int,
        iLength: Int,
    ): Int {
        // Note: Assumes big-endian
        val iEndBit = iStartBit + iLength - 1
        val iSByte = iStartBit / 8
        val iSBit = iStartBit % 8
        val iEByte = iEndBit / 8
        val iEBit = iEndBit % 8

        if (iSByte == iEByte) {
            return ((buffer[iEByte].toInt() and 0xFF) shr (7 - iEBit)) and (0xFF shr (8 - iLength))
        } else {
            var uRet = (
                (buffer[iSByte].toInt() and 0xFF and (0xFF shr iSBit)) shl (
                    ((iEByte - iSByte - 1) * 8) +
                        (iEBit + 1)
                )
            )

            for (i in iSByte + 1 until iEByte) {
                uRet = uRet or ((buffer[i].toInt() and 0xFF) shl (((iEByte - i - 1) * 8) + (iEBit + 1)))
            }

            uRet = uRet or ((buffer[iEByte].toInt() and 0xFF) shr (7 - iEBit))

            return uRet
        }
    }

    private val HEX_CHARS =
        charArrayOf(
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9',
            'a',
            'b',
            'c',
            'd',
            'e',
            'f',
        )
}
