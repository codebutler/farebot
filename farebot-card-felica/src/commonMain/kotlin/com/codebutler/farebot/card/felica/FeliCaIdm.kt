/*
 * FeliCaIdm.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011 Kazzz
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 *
 * Contains improvements ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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
 * FeliCa IDm (Identification Manufacturer code) - 8 bytes.
 *
 * Contains a 2-byte manufacture code and 6-byte card identification.
 */
class FeliCaIdm(
    bytes: ByteArray,
) {
    val manufactureCode: ByteArray = byteArrayOf(bytes[0], bytes[1])
    val cardIdentification: ByteArray =
        byteArrayOf(bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7])

    /** Manufacture code as a 16-bit integer. */
    val manufactureCodeInt: Int
        get() =
            ((manufactureCode[0].toInt() and 0xFF) shl 8) or
                (manufactureCode[1].toInt() and 0xFF)

    /** Card identification number as a 48-bit long. */
    val cardIdentificationLong: Long
        get() {
            var result = 0L
            for (b in cardIdentification) {
                result = (result shl 8) or (b.toLong() and 0xFF)
            }
            return result
        }

    fun getBytes(): ByteArray = manufactureCode + cardIdentification

    override fun toString(): String = "IDm: " + FeliCaUtil.getHexString(getBytes())
}
