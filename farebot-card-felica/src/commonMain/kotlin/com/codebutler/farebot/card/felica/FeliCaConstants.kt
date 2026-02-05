/*
 * FeliCaConstants.kt
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
 * Constants for FeliCa NFC-F protocol.
 *
 * Migrated from net.kazzz.felica.lib.FeliCaLib.
 */
object FeliCaConstants {

    // Command codes
    const val COMMAND_POLLING: Byte = 0x00
    const val RESPONSE_POLLING: Byte = 0x01
    const val COMMAND_REQUEST_SERVICE: Byte = 0x02
    const val RESPONSE_REQUEST_SERVICE: Byte = 0x03
    const val COMMAND_REQUEST_RESPONSE: Byte = 0x04
    const val RESPONSE_REQUEST_RESPONSE: Byte = 0x05
    const val COMMAND_READ_WO_ENCRYPTION: Byte = 0x06
    const val RESPONSE_READ_WO_ENCRYPTION: Byte = 0x07
    const val COMMAND_WRITE_WO_ENCRYPTION: Byte = 0x08
    const val RESPONSE_WRITE_WO_ENCRYPTION: Byte = 0x09
    const val COMMAND_SEARCH_SERVICECODE: Byte = 0x0a
    const val RESPONSE_SEARCH_SERVICECODE: Byte = 0x0b
    const val COMMAND_REQUEST_SYSTEMCODE: Byte = 0x0c
    const val RESPONSE_REQUEST_SYSTEMCODE: Byte = 0x0d

    // System codes
    const val SYSTEMCODE_ANY = 0xffff
    const val SYSTEMCODE_FELICA_LITE = 0x88b4
    const val SYSTEMCODE_NDEF = 0x12fc
    const val SYSTEMCODE_COMMON = 0xfe00
    const val SYSTEMCODE_CYBERNE = 0x0003
    const val SYSTEMCODE_EDY = 0xfe00
    const val SYSTEMCODE_SZT = 0x8005
    const val SYSTEMCODE_OCTOPUS = 0x8008
    const val SYSTEMCODE_SUICA = 0x0003
    const val SYSTEMCODE_PASMO = 0x0003

    // Service codes (little endian values)
    const val SERVICE_SUICA_INOUT = 0x108f
    const val SERVICE_SUICA_HISTORY = 0x090f
    const val SERVICE_FELICA_LITE_READONLY = 0x000b
    const val SERVICE_FELICA_LITE_READWRITE = 0x0009
    const val SERVICE_OCTOPUS = 0x0117
    const val SERVICE_SZT = 0x0118

    // FeliCa Lite block addresses
    const val FELICA_LITE_BLOCK_MC = 0x88
}
