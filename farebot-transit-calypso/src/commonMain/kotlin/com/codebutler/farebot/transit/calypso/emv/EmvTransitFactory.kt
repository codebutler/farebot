/*
 * EmvTransitFactory.kt
 *
 * Copyright 2019-2022 Google
 * Copyright 2019-2022 Michael Farrell <micolous+git@gmail.com>
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

import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.base.util.readASCII
import com.codebutler.farebot.card.iso7816.ISO7816Application
import farebot.farebot_transit_calypso.generated.resources.Res
import farebot.farebot_transit_calypso.generated.resources.emv_expiry_date
import farebot.farebot_transit_calypso.generated.resources.emv_pin_attempts_remaining
import farebot.farebot_transit_calypso.generated.resources.emv_service_code
import farebot.farebot_transit_calypso.generated.resources.emv_transaction_counter
import com.codebutler.farebot.card.iso7816.ISO7816Card
import com.codebutler.farebot.card.iso7816.ISO7816TLV
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip

/**
 * Identifies and parses EMV contactless payment cards (Visa, Mastercard, etc.).
 */
object EmvTransitFactory : TransitFactory<ISO7816Card, EmvTransitInfo> {

    override val allCards: List<CardInfo> = emptyList()

    // Common EMV application AIDs
    private val EMV_AIDS = mapOf(
        "a0000000031010" to "Visa",
        "a0000000032010" to "Visa Electron",
        "a0000000041010" to "Mastercard",
        "a0000000042010" to "Mastercard Maestro",
        "a00000002501" to "American Express",
        "a0000000651010" to "JCB",
        "a0000003241010" to "Discover",
        "a0000003710001" to "Interac",
        "a0000000043060" to "Mastercard Maestro",
        "a000000004101001" to "Mastercard",
        "d5780000021010" to "Bankaxept",
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun check(card: ISO7816Card): Boolean {
        return card.applications.any { app ->
            val aidHex = app.appName?.toHexString()?.lowercase()
            aidHex != null && EMV_AIDS.keys.any { aidHex.startsWith(it) }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun parseIdentity(card: ISO7816Card): TransitIdentity {
        val app = findEmvApp(card) ?: return TransitIdentity.create("EMV", null)
        val allTlv = getAllTlv(app)
        val name = findName(allTlv)
        val t2 = findT2Data(allTlv)
        val pan = getPan(t2)
        return TransitIdentity.create(name, splitby4(pan))
    }

    override fun parseInfo(card: ISO7816Card): EmvTransitInfo {
        val app = findEmvApp(card) ?: return EmvTransitInfo(
            name = "EMV", mSerialNumber = null, tlvs = emptyList(),
            pinTriesRemaining = null, transactionCounter = null,
            logEntries = null, t2 = null
        )

        val allTlv = getAllTlv(app)
        val name = findName(allTlv)
        val t2 = findT2Data(allTlv)

        // Parse log entries if available
        val logEntryTag = getTag(allTlv, EmvData.LOG_ENTRY)
        val logFormat = findLogFormat(app)
        val logEntries = if (logEntryTag != null && logFormat != null && logEntryTag.isNotEmpty()) {
            val logSfi = logEntryTag[0].toInt() and 0xff
            val logFile = app.getSfiFile(logSfi)
            logFile?.recordList?.mapNotNull { EmvLogEntry.parseEmvTrip(it, logFormat) }
        } else null

        // Parse PIN tries remaining (tag 9f17)
        val pinTriesRemaining = getTag(allTlv, "9f17")?.let {
            ISO7816TLV.removeTlvHeader(it).byteArrayToInt()
        }

        // Parse transaction counter (tag 9f36)
        val transactionCounter = getTag(allTlv, "9f36")?.let {
            ISO7816TLV.removeTlvHeader(it).byteArrayToInt()
        }

        return EmvTransitInfo(
            name = name,
            mSerialNumber = splitby4(getPan(t2)),
            tlvs = allTlv,
            pinTriesRemaining = pinTriesRemaining,
            transactionCounter = transactionCounter,
            logEntries = logEntries,
            t2 = t2
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun findEmvApp(card: ISO7816Card): ISO7816Application? {
        for (app in card.applications) {
            val aidHex = app.appName?.toHexString()?.lowercase() ?: continue
            // Skip ignored AIDs
            if (EmvData.PARSER_IGNORED_AID_PREFIX.any { aidHex.startsWith(it) }) continue
            if (EMV_AIDS.keys.any { aidHex.startsWith(it) }) return app
        }
        return null
    }

    private fun getAllTlv(app: ISO7816Application): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        // FCI data
        app.appFci?.let { result.add(it) }
        // SFI files (1-10 are typical for EMV data)
        for (sfi in 1..31) {
            val file = app.getSfiFile(sfi) ?: continue
            file.binaryData?.let { result.add(it) }
            for (record in file.recordList) {
                result.add(record)
            }
        }
        return result
    }

    private fun findLogFormat(app: ISO7816Application): ByteArray? {
        // Log format is typically stored in a specific file or TLV tag
        // In Metrodroid, it's card.logFormat - we look in the FCI and SFI data
        val allTlv = getAllTlv(app)
        // The log format tag (9f4f) specifies the format of transaction log entries
        return getTag(allTlv, EmvData.TAG_LOG_FORMAT)
    }

    private fun getTag(tlvs: List<ByteArray>, id: String): ByteArray? {
        for (tlv in tlvs) {
            return ISO7816TLV.findBERTLV(tlv, id, false) ?: continue
        }
        return null
    }

    private fun findT2Data(tlvs: List<ByteArray>): ByteArray? {
        for (tlv in tlvs) {
            val t2e = ISO7816TLV.findBERTLV(tlv, EmvData.TAG_TRACK2_EQUIV, false)
            if (t2e != null) return t2e
            val t2 = ISO7816TLV.findBERTLV(tlv, EmvData.TAG_TRACK2, false)
            if (t2 != null) return t2
        }
        return null
    }

    private fun findName(tlvs: List<ByteArray>): String {
        for (tag in listOf(EmvData.TAG_NAME2, EmvData.TAG_NAME1)) {
            val variant = getTag(tlvs, tag) ?: continue
            return variant.readASCII()
        }
        return "EMV"
    }

    private fun splitby4(input: String?): String? {
        if (input == null) return null
        return (0..input.length step 4).joinToString(" ") {
            input.substring(it, minOf(it + 4, input.length))
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getPan(t2: ByteArray?): String? {
        val t2s = t2?.toHexString() ?: return null
        return t2s.substringBefore('d', t2s)
    }
}

/**
 * EMV payment card transit info with full TLV data, PAN, and transaction log.
 */
class EmvTransitInfo(
    private val name: String,
    private val mSerialNumber: String?,
    private val tlvs: List<ByteArray>,
    private val pinTriesRemaining: Int?,
    private val transactionCounter: Int?,
    private val logEntries: List<EmvLogEntry>?,
    private val t2: ByteArray?
) : TransitInfo() {

    override val cardName: String = name

    override val serialNumber: String? = mSerialNumber

    override val trips: List<Trip>? = logEntries

    @OptIn(ExperimentalStdlibApi::class)
    override val info: List<ListItemInterface>?
        get() {
            val res = mutableListOf<ListItemInterface>()

            if (t2 != null) {
                val t2s = t2.toHexString()
                val postPan = t2s.substringAfter('d', "")
                if (postPan.length >= 4) {
                    res += ListItem(Res.string.emv_expiry_date, "${postPan.substring(2, 4)}/${postPan.substring(0, 2)}")
                    if (postPan.length >= 7) {
                        val serviceCode = postPan.substring(4, 7)
                        res += ListItem(Res.string.emv_service_code, serviceCode)
                    }
                }
            }

            if (pinTriesRemaining != null) {
                res += ListItem(Res.string.emv_pin_attempts_remaining, pinTriesRemaining.toString())
            }
            if (transactionCounter != null) {
                res += ListItem(Res.string.emv_transaction_counter, transactionCounter.toString())
            }

            res += ISO7816TLV.infoBerTLVs(tlvs, EmvData.TAGMAP, hideThings = false)

            return res.ifEmpty { null }
        }
}
