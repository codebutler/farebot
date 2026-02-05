/*
 * EmvData.kt
 *
 * Copyright 2019 Google
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.calypso.emv

import com.codebutler.farebot.card.iso7816.HIDDEN_TAG
import com.codebutler.farebot.card.iso7816.TagContents.ASCII
import com.codebutler.farebot.card.iso7816.TagContents.CONTENTS_DATE
import com.codebutler.farebot.card.iso7816.TagContents.CURRENCY
import com.codebutler.farebot.card.iso7816.TagContents.DUMP_LONG
import com.codebutler.farebot.card.iso7816.TagContents.DUMP_SHORT
import com.codebutler.farebot.card.iso7816.TagContents.FDDA
import com.codebutler.farebot.card.iso7816.TagDesc
import com.codebutler.farebot.card.iso7816.TagHiding.CARD_NUMBER
import com.codebutler.farebot.card.iso7816.TagHiding.DATE

internal object EmvData {

    const val TAG_NAME1 = "50"
    const val TAG_TRACK1 = "56"
    const val TAG_TRACK2_EQUIV = "57"
    const val TAG_TRACK3_EQUIV = "58"
    const val TAG_CARD_EXPIRATION_DATE = "59"
    const val TAG_PAN = "5a"
    const val TAG_EXPIRATION_DATE = "5f24"
    const val TAG_CARD_EFFECTIVE = "5f26"
    const val TAG_INTERCHANGE_PROTOCOL = "5f27"
    const val TAG_ISSUER_COUNTRY = "5f28"
    const val TAG_TRANSACTION_CURRENCY_CODE = "5f2a"
    const val TAG_TERMINAL_VERIFICATION_RESULTS = "95"
    const val TAG_TRANSACTION_DATE = "9a"
    const val TAG_TRANSACTION_TYPE = "9c"
    const val TAG_AMOUNT_AUTHORISED = "9f02"
    const val TAG_AMOUNT_OTHER = "9f03"
    const val TAG_TRANSACTION_TIME = "9f21"
    const val TAG_NAME2 = "9f12"
    const val TAG_TERMINAL_COUNTRY_CODE = "9f1a"
    const val TAG_UNPREDICTABLE_NUMBER = "9f37"
    const val TAG_PDOL = "9f38"
    const val LOG_ENTRY = "9f4d"
    const val TAG_LOG_FORMAT = "9f4f"
    const val TAG_TERMINAL_TRANSACTION_QUALIFIERS = "9f66"
    const val TAG_TRACK2 = "9f6b"

    val TAGMAP = mapOf(
        TAG_NAME1 to TagDesc("Application label", ASCII),
        TAG_TRACK1 to TagDesc("Track 1", ASCII, CARD_NUMBER),
        TAG_TRACK2_EQUIV to TagDesc("Track 2 equivalent", DUMP_SHORT, CARD_NUMBER),
        TAG_TRACK3_EQUIV to TagDesc("Track 3 equivalent", DUMP_SHORT, CARD_NUMBER),
        TAG_CARD_EXPIRATION_DATE to TagDesc("Card expiration date", CONTENTS_DATE, DATE),
        TAG_PAN to HIDDEN_TAG, // PAN, shown elsewhere
        "5f20" to TagDesc("Cardholder name", ASCII, CARD_NUMBER),
        TAG_EXPIRATION_DATE to TagDesc("Expiry date", CONTENTS_DATE, DATE),
        "5f25" to TagDesc("Issue date", CONTENTS_DATE, DATE),
        TAG_CARD_EFFECTIVE to TagDesc("Card effective date", CONTENTS_DATE, DATE),
        TAG_INTERCHANGE_PROTOCOL to TagDesc("Interchange control", DUMP_SHORT),
        TAG_ISSUER_COUNTRY to TagDesc("Issuer country", DUMP_SHORT),
        "5f2d" to TagDesc("Language preference", ASCII),
        "5f30" to TagDesc("Service code", DUMP_SHORT, CARD_NUMBER),
        "5f34" to TagDesc("PAN sequence number", DUMP_SHORT, CARD_NUMBER),
        "82" to TagDesc("Application interchange profile", DUMP_SHORT),
        "87" to TagDesc("Application priority indicator", DUMP_SHORT),
        "8c" to HIDDEN_TAG, // CDOL1
        "8d" to HIDDEN_TAG, // CDOL2
        "8e" to HIDDEN_TAG, // CVM list
        "8f" to TagDesc("CA public key index", DUMP_SHORT, CARD_NUMBER),
        "90" to TagDesc("Issuer public key certificate", DUMP_LONG, CARD_NUMBER),
        "92" to TagDesc("Issuer public key modulus", DUMP_LONG, CARD_NUMBER),
        "93" to TagDesc("Signed static application data", DUMP_LONG, CARD_NUMBER),
        "94" to TagDesc("Application file locator", DUMP_SHORT),
        "9f07" to HIDDEN_TAG, // Application Usage Control
        "9f08" to HIDDEN_TAG, // Application Version Number
        "9f0b" to TagDesc("Cardholder name", ASCII, CARD_NUMBER),
        "9f0d" to HIDDEN_TAG, // Issuer Action Code - Default
        "9f0e" to HIDDEN_TAG, // Issuer Action Code - Denial
        "9f0f" to HIDDEN_TAG, // Issuer Action Code - Online
        "9f10" to TagDesc("Issuer application data", DUMP_LONG, CARD_NUMBER),
        "9f11" to TagDesc("Issuer code table index", DUMP_SHORT, CARD_NUMBER),
        TAG_NAME2 to TagDesc("Application preferred name", ASCII),
        "9f1f" to TagDesc("Track 1 discretionary data", ASCII, CARD_NUMBER),
        "9f26" to TagDesc("Application cryptogram", DUMP_LONG, CARD_NUMBER),
        "9f27" to TagDesc("Cryptogram information data", DUMP_LONG, CARD_NUMBER),
        "9f32" to TagDesc("Issuer public key exponent", DUMP_LONG, CARD_NUMBER),
        "9f36" to TagDesc("Application transaction counter", DUMP_SHORT, CARD_NUMBER),
        TAG_PDOL to HIDDEN_TAG, // PDOL
        "9f42" to TagDesc("Application currency", CURRENCY),
        "9f44" to TagDesc("Application currency exponent", DUMP_SHORT),
        "9f46" to TagDesc("ICC public key certificate", DUMP_LONG, CARD_NUMBER),
        "9f47" to TagDesc("ICC public key exponent", DUMP_LONG, CARD_NUMBER),
        "9f48" to TagDesc("ICC public key modulus", DUMP_LONG, CARD_NUMBER),
        "9f49" to HIDDEN_TAG, // DDOL
        "9f4a" to HIDDEN_TAG, // Static Data Authentication Tag List
        LOG_ENTRY to HIDDEN_TAG, // Log entry
        "9f69" to TagDesc("Card FDDA", FDDA, CARD_NUMBER),
        TAG_TRACK2 to TagDesc("Track 2", DUMP_SHORT, CARD_NUMBER),
        "61" to HIDDEN_TAG // Subtag (discretionary data)
    )

    /**
     * AID prefixes to ignore when parsing EMV cards.
     */
    val PARSER_IGNORED_AID_PREFIX = listOf(
        // eftpos (Australia)
        "a000000384"
    )

    /**
     * ISO 4217 numeric currency code to string code mapping.
     */
    private val NUMERIC_CURRENCY_MAP = mapOf(
        36 to "AUD",
        124 to "CAD",
        156 to "CNY",
        208 to "DKK",
        344 to "HKD",
        356 to "INR",
        360 to "IDR",
        376 to "ILS",
        392 to "JPY",
        410 to "KRW",
        458 to "MYR",
        554 to "NZD",
        578 to "NOK",
        643 to "RUB",
        702 to "SGD",
        710 to "ZAR",
        752 to "SEK",
        756 to "CHF",
        764 to "THB",
        784 to "AED",
        826 to "GBP",
        840 to "USD",
        901 to "TWD",
        949 to "TRY",
        978 to "EUR",
        986 to "BRL"
    )

    fun numericCodeToString(code: Int): String? = NUMERIC_CURRENCY_MAP[code]
}
