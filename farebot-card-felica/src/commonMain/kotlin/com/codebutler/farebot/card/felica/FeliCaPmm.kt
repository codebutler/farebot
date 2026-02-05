/*
 * FeliCaPmm.kt
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
 * FeliCa PMm (Manufacturing Parameters) - 8 bytes.
 *
 * Contains a 2-byte IC code and 6-byte maximum response time.
 */
class FeliCaPmm(bytes: ByteArray) {
    val icCode: ByteArray = byteArrayOf(bytes[0], bytes[1])
    val maximumResponseTime: ByteArray =
        byteArrayOf(bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7])

    /** ROM type extracted from IC code byte 0. */
    val romType: Int
        get() = icCode[0].toInt() and 0xFF

    /** IC type extracted from IC code byte 1. */
    val icType: Int
        get() = icCode[1].toInt() and 0xFF

    fun getBytes(): ByteArray = icCode + maximumResponseTime

    override fun toString(): String {
        return "PMm: " + FeliCaUtil.getHexString(getBytes())
    }
}
