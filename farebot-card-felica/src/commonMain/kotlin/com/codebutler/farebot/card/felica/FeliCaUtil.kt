/*
 * FeliCaUtil.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011 Kazzz
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codebutler.farebot.card.felica

/**
 * Byte/int conversion and formatting utilities for FeliCa data.
 *
 * Migrated from net.kazzz.felica.lib.Util.
 */
object FeliCaUtil {
    fun toBytes(a: Int): ByteArray {
        val bs = ByteArray(4)
        bs[3] = (0x000000ff and a).toByte()
        bs[2] = (0x000000ff and (a ushr 8)).toByte()
        bs[1] = (0x000000ff and (a ushr 16)).toByte()
        bs[0] = (0x000000ff and (a ushr 24)).toByte()
        return bs
    }

    fun toInt(vararg b: Byte): Int {
        require(b.isNotEmpty())

        if (b.size == 1) {
            return b[0].toInt() and 0xFF
        }
        if (b.size == 2) {
            var i = 0
            i = i or (b[0].toInt() and 0xFF)
            i = i shl 8
            i = i or (b[1].toInt() and 0xFF)
            return i
        }
        if (b.size == 3) {
            var i = 0
            i = i or (b[0].toInt() and 0xFF)
            i = i shl 8
            i = i or (b[1].toInt() and 0xFF)
            i = i shl 8
            i = i or (b[2].toInt() and 0xFF)
            return i
        }

        var result = 0
        for (idx in 0 until minOf(b.size, 4)) {
            result = result shl 8
            result = result or (b[idx].toInt() and 0xFF)
        }
        return result
    }

    fun getHexString(data: Byte): String = getHexString(byteArrayOf(data))

    fun getHexString(
        byteArray: ByteArray,
        vararg split: Int,
    ): String {
        val builder = StringBuilder()
        val target: ByteArray =
            if (split.size <= 1) {
                byteArray
            } else if (split.size < 2) {
                byteArray.copyOfRange(0, 0 + split[0])
            } else {
                byteArray.copyOfRange(split[0], split[0] + split[1])
            }
        for (b in target) {
            builder.append((b.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase())
        }
        return builder.toString()
    }

    fun getBinString(data: Byte): String = getBinString(byteArrayOf(data))

    fun getBinString(
        byteArray: ByteArray,
        vararg split: Int,
    ): String {
        val builder = StringBuilder()
        val target: ByteArray =
            if (split.size <= 1) {
                byteArray
            } else if (split.size < 2) {
                byteArray.copyOfRange(0, 0 + split[0])
            } else {
                byteArray.copyOfRange(split[0], split[0] + split[1])
            }

        for (b in target) {
            builder.append(
                (b.toInt() and 0xFF).toString(2).padStart(8, '0'),
            )
        }
        return builder.toString()
    }
}
