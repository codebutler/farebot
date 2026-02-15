/*
 * NdefData.kt
 *
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

package com.codebutler.farebot.transit.ndef

import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.classic.ClassicBlock
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.card.felica.FeliCaConstants
import com.codebutler.farebot.card.felica.FelicaCard
import com.codebutler.farebot.card.felica.FelicaSystem
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.card.vicinity.VicinityCard
import com.codebutler.farebot.transit.TransitInfo
import farebot.transit.ndef.generated.resources.Res
import farebot.transit.ndef.generated.resources.ndef_card_name
import kotlinx.serialization.Serializable
import com.codebutler.farebot.base.util.FormattedString

@Serializable
data class NdefData(
    val entries: List<NdefEntry>,
) : TransitInfo() {
    override val serialNumber: String?
        get() = null

    override val cardName: FormattedString
        get() = NAME

    override val info: List<ListItemInterface>?
        get() = entries.flatMap { it.info }

    fun getEntryExtType(type: ByteArray): NdefExtType? =
        entries.filterIsInstance<NdefExtType>().firstOrNull { type.contentEquals(it.type) }

    fun getEntryExtType(type: String): NdefExtType? = getEntryExtType(type.encodeToByteArray())

    companion object {
        val NAME: FormattedString
            get() = FormattedString(Res.string.ndef_card_name)

        fun checkClassic(card: ClassicCard): Boolean =
            MifareClassicAccessDirectory
                .parse(card)
                ?.contains(MifareClassicAccessDirectory.NFC_AID) == true

        fun parseClassic(
            card: ClassicCard,
            aid: Int = MifareClassicAccessDirectory.NFC_AID,
        ): NdefData? {
            val mad = MifareClassicAccessDirectory.parse(card) ?: return null
            val sectors = mad.getContiguous(aid)

            if (sectors.isEmpty()) {
                return null
            }

            val allData =
                sectors
                    .flatMap { sectorIndex ->
                        val sector =
                            card.getSector(sectorIndex) as? DataClassicSector
                                ?: return@flatMap emptyList()
                        sector.blocks
                            .filter { it.type != ClassicBlock.TYPE_TRAILER }
                            .flatMap { it.data.toList() }
                    }.toByteArray()

            return parseTLVNDEF(allData)
        }

        fun checkUltralight(card: UltralightCard): Boolean {
            try {
                val cc = card.getPage(3).data

                if (cc[0] != 0xe1.toByte()) {
                    return false
                }
                if (cc[1].toInt() !in listOf(0x10, 0x11)) {
                    return false
                }
                if (cc[3].toInt() and 0xf0 != 0) {
                    return false
                }
                return true
            } catch (e: Exception) {
                return false
            }
        }

        private fun getLenFromCCVicinity(card: VicinityCard): Pair<Int, Int>? {
            try {
                var cc = card.getPage(0).data

                if (cc[0].toInt() and 0xff !in listOf(0xe1, 0xe2)) {
                    return null
                }
                if (cc[1].toInt() and 0xfc != 0x40) {
                    return null
                }
                if (cc[2].toInt() != 0) {
                    return Pair(4, cc[2].toInt() and 0xff)
                }
                if (cc.size < 8) {
                    cc = cc + card.getPage(1).data
                }
                return Pair(8, cc.byteArrayToInt(6, 2))
            } catch (e: Exception) {
                return null
            }
        }

        fun checkVicinity(card: VicinityCard): Boolean = getLenFromCCVicinity(card) != null

        fun parseVicinity(card: VicinityCard): NdefData? {
            val l = getLenFromCCVicinity(card) ?: return null
            return parseTLVNDEF(card.readBytes(l.first, l.second))
        }

        private fun getFelicaSystem(card: FelicaCard): FelicaSystem? {
            val ndefService = card.getSystem(FeliCaConstants.SYSTEMCODE_NDEF)
            if (ndefService != null) {
                return ndefService
            }
            val liteService = card.getSystem(FeliCaConstants.SYSTEMCODE_FELICA_LITE) ?: return null
            val mc =
                liteService
                    .getService(FeliCaConstants.SERVICE_FELICA_LITE_READONLY)
                    ?.getBlock(FeliCaConstants.FELICA_LITE_BLOCK_MC) ?: return null
            if (mc.data[3] == 0x01.toByte()) {
                return liteService
            }
            return null
        }

        fun checkFelica(card: FelicaCard): Boolean {
            val service = getFelicaSystem(card)?.getService(0xb) ?: return false
            val attributes = service.getBlock(0)?.data ?: return false
            if (attributes[0].toInt() !in listOf(0x10, 0x11)) {
                return false
            }
            val checksum =
                attributes.sliceOffLen(0, 14).map { it.toInt() and 0xff }.sum()
            val storedChecksum = attributes.byteArrayToInt(14, 2)
            return checksum == storedChecksum
        }

        fun parseFelica(card: FelicaCard): NdefData? {
            val service = getFelicaSystem(card)?.getService(0xb) ?: return null
            val attributes = service.getBlock(0)?.data ?: return null
            val ln = attributes.byteArrayToInt(11, 3)
            if (ln == 0) {
                return NdefData(emptyList())
            }
            val allData =
                (1..(ln + 15) / 16)
                    .flatMap {
                        service.getBlock(it)?.data?.toList() ?: emptyList()
                    }.toByteArray()
                    .sliceOffLen(0, ln)
            return parseNDEF(allData)
        }

        fun parseUltralight(card: UltralightCard): NdefData? {
            val cc = card.getPage(3).data
            val sz = (cc[2].toInt() and 0xff) shl 1
            val dt = card.readPages(4, sz)

            return parseTLVNDEF(dt)
        }

        private fun parseTLVNDEF(data: ByteArray): NdefData? {
            var res: NdefData? = null
            for ((t, v) in iterateTLV(data)) {
                if (t == 0x03) {
                    val parsed = parseNDEF(v) ?: continue
                    res = if (res == null) parsed else res + parsed
                }
            }

            return res
        }

        private fun iterateTLV(data: ByteArray): Sequence<Pair<Int, ByteArray>> =
            sequence {
                var ptr = 0
                while (ptr < data.size) {
                    val t = data[ptr++]
                    if (t == 0xfe.toByte()) {
                        break
                    }
                    if (t == 0.toByte()) {
                        continue
                    }
                    var l = data[ptr++].toInt() and 0xff
                    if (l == 0xff) {
                        l = data.byteArrayToInt(ptr, 2)
                        ptr += 2
                    }

                    val v = data.sliceOffLen(ptr, l)
                    ptr += l

                    yield(Pair(t.toInt(), v))
                }
            }

        private fun parseNDEF(data: ByteArray): NdefData? {
            var ptr = 0
            val entries = mutableListOf<NdefEntry>()

            while (ptr < data.size) {
                val (entry, sz, isLast) = parseEntry(data, ptr)

                if (entry == null) {
                    break
                }

                entries += entry
                ptr += sz

                if (isLast) {
                    break
                }
            }

            return NdefData(entries)
        }

        private fun parseEntry(
            data: ByteArray,
            ptrStart: Int,
        ): Triple<NdefEntry?, Int, Boolean> {
            var ptr = ptrStart
            val head = NdefHead.parse(data, ptr) ?: return Triple(null, 0, true)
            ptr += head.headLen
            val type = data.sliceOffLen(ptr, head.typeLen)
            ptr += head.typeLen
            val id = if (head.idLen != null) data.sliceOffLen(ptr, head.idLen) else null
            ptr += head.idLen ?: 0
            var payload = data.sliceOffLen(ptr, head.payloadLen)
            ptr += head.payloadLen
            var me = head.me

            if (head.cf) {
                while (true) {
                    val subHead = NdefHead.parse(data, ptr) ?: return Triple(null, 0, true)
                    ptr += head.headLen
                    payload = payload + data.sliceOffLen(ptr, head.payloadLen)
                    ptr += head.payloadLen
                    me = subHead.me
                    if (!subHead.cf) {
                        break
                    }
                }
            }

            return Triple(
                payloadToEntry(head.tnf, type, id, payload),
                ptr - ptrStart,
                me,
            )
        }

        private val WIFI_MIME = "application/vnd.wfa.wsc".encodeToByteArray()
        private val ANDROID_PKG_TYPE = "android.com:pkg".encodeToByteArray()
        private val TYPE_T = "T".encodeToByteArray()
        private val TYPE_U = "U".encodeToByteArray()

        private fun payloadToEntry(
            tnf: Int,
            type: ByteArray,
            id: ByteArray?,
            payload: ByteArray,
        ): NdefEntry? =
            when (tnf) {
                0 -> NdefEmpty(tnf, type, id, payload)
                1 ->
                    when {
                        type.contentEquals(TYPE_T) -> NdefText(tnf, type, id, payload)
                        type.contentEquals(TYPE_U) -> NdefUri(tnf, type, id, payload)
                        else -> NdefUnknownRTD(tnf, type, id, payload)
                    }
                2 ->
                    when {
                        type.contentEquals(WIFI_MIME) -> NdefWifi(tnf, type, id, payload)
                        else -> NdefUnknownMIME(tnf, type, id, payload)
                    }
                3 -> NdefUriType(tnf, type, id, payload)
                4 ->
                    when {
                        type.contentEquals(ANDROID_PKG_TYPE) -> NdefAndroidPkg(tnf, type, id, payload)
                        else -> NdefUnknownExtType(tnf, type, id, payload)
                    }
                5 -> NdefBinaryType(tnf, type, id, payload)
                else -> NdefInvalidType(tnf, type, id, payload)
            }
    }

    private operator fun plus(second: NdefData) =
        NdefData(
            entries = this.entries + second.entries,
        )
}
