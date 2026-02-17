/*
 * ISO7816TLV.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
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

package com.codebutler.farebot.card.iso7816

import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.ui.ListItemRecursive
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.base.util.indexOf
import com.codebutler.farebot.base.util.indexOfFirstStarting
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.base.util.sliceOffLenSafe
import com.codebutler.farebot.base.util.toHexDump
import farebot.card.iso7816.generated.resources.*

/**
 * Utilities for decoding BER-TLV values.
 *
 * Reference: https://en.wikipedia.org/wiki/X.690#BER_encoding
 */
object ISO7816TLV {
    private const val TAG = "ISO7816TLV"
    private const val MAX_TLV_FIELD_LENGTH = 0xffff

    /**
     * Gets the _length_ of a TLV tag identifier octets.
     *
     * @param buf TLV data buffer
     * @param p Offset within [buf] to read from
     * @return The number of bytes for this tag's identifier octets.
     */
    private fun getTLVIDLen(
        buf: ByteArray,
        p: Int,
    ): Int {
        // One byte version: if the lower 5 bits (tag number) != 0x1f.
        if (buf[p].toInt() and 0x1f != 0x1f) {
            return 1
        }

        // Multi-byte version: if the first byte has the lower 5 bits == 0x1f then subsequent
        // bytes contain the tag number. Bit 8 is set when there (is/are) more byte(s) for the
        // tag number.
        var len = 1
        @Suppress("ControlFlowWithEmptyBody")
        while (buf[p + len++].toInt() and 0x80 != 0) {
            // empty body - continue reading multi-byte tag
        }
        return len
    }

    /**
     * Decodes the length octets for a TLV tag.
     *
     * @param buf TLV data buffer
     * @param p Offset within [buf] to start reading the length octets from
     * @return A [Triple] of: the number of bytes for this tag's length octets, the
     * length of the _contents octets_, and the length of the _end of contents octets_.
     *
     * Returns `null` if invalid.
     */
    private fun decodeTLVLen(
        buf: ByteArray,
        p: Int,
    ): Triple<Int, Int, Int>? {
        val headByte = buf[p].toInt() and 0xff
        if (headByte shr 7 == 0) {
            // Definite, short form (1 byte)
            // Length is the lower 7 bits of the first byte
            return Triple(1, headByte and 0x7f, 0)
        }

        // Decode other forms
        val numfollowingbytes = headByte and 0x7f
        if (numfollowingbytes == 0) {
            // Indefinite form.
            // Value is terminated by two NULL bytes.
            val endPos = buf.indexOf(ByteArray(2), p + 1)
            return if (endPos == -1) {
                null
            } else {
                Triple(1, endPos - p - 1, 2)
            }
        } else if (numfollowingbytes >= 8) {
            // Definite, long form
            //
            // We got 8 or more following bytes for storing the length. We can only decode
            // this if all bytes but the last 8 are NULL, and the 8th-to-last top bit is also 0.
            val topBytes = buf.sliceOffLen(p + 1, numfollowingbytes - 8)
            if (!(topBytes.isEmpty() || topBytes.isAllZero()) ||
                buf[p + 1 + numfollowingbytes - 7].toInt() and 0x80 == 0x80
            ) {
                return null
            }
        }

        // Definite form, long form
        val length = buf.byteArrayToInt(p + 1, numfollowingbytes)

        if (length > MAX_TLV_FIELD_LENGTH) {
            return null
        } else if (length < 0) {
            return null
        }

        return Triple(1 + numfollowingbytes, length, 0)
    }

    /**
     * Iterates over BER-TLV encoded data lazily with a [Sequence].
     *
     * @param buf The BER-TLV encoded data to iterate over
     * @param multihead If true, process multiple top-level TLV containers
     * @return [Sequence] of [Triple] of `id, header, data`
     */
    fun berTlvIterate(
        buf: ByteArray,
        multihead: Boolean = false,
    ): Sequence<Triple<ByteArray, ByteArray, ByteArray>> {
        return sequence {
            var p = 0
            while (p < buf.size) {
                // Skip null bytes at start
                p = buf.indexOfFirstStarting(p) { it != 0.toByte() }

                if (p == -1) {
                    return@sequence
                }

                // Skip ID
                p += getTLVIDLen(buf, p)
                val (startoffset, alldatalen, alleoclen) = decodeTLVLen(buf, p) ?: return@sequence
                if (p < 0 || startoffset < 0 || alldatalen < 0 || alleoclen < 0) {
                    return@sequence
                }

                p += startoffset
                val fulllen = p + alldatalen

                while (p < fulllen) {
                    // Skip null bytes
                    if (buf[p] == 0.toByte()) {
                        p++
                        continue
                    }

                    val idlen = getTLVIDLen(buf, p)

                    if (p + idlen >= buf.size) return@sequence // EOF
                    val id = buf.sliceOffLenSafe(p, idlen)
                    if (id == null) {
                        return@sequence
                    }

                    val (hlen, datalen, eoclen) = decodeTLVLen(buf, p + idlen) ?: break

                    if (idlen < 0 || hlen < 0 || datalen < 0 || eoclen < 0) {
                        return@sequence
                    }

                    val header = buf.sliceOffLenSafe(p, idlen + hlen)
                    val data = buf.sliceOffLenSafe(p + idlen + hlen, datalen)

                    if (header == null || data == null) {
                        return@sequence
                    }

                    if ((id.isAllZero() || id.isEmpty()) &&
                        (header.isEmpty() || header.isAllZero()) &&
                        data.isEmpty()
                    ) {
                        // Skip empty tag
                        continue
                    }

                    yield(Triple(id, header, data))
                    p += idlen + hlen + datalen + eoclen
                }

                if (!multihead) {
                    return@sequence
                }
            }
        }
    }

    /**
     * Iterates over Processing Options Data Object List (PDOL), tag 9f38.
     *
     * This is a list of tags needed by the ICC for the GET PROCESSING OPTIONS (GPO) command.
     *
     * The lengths in this context are the expected length in the request.
     */
    fun pdolIterate(buf: ByteArray): Sequence<Pair<ByteArray, Int>> =
        sequence {
            var p = 0
            while (p < buf.size) {
                val idlen = getTLVIDLen(buf, p)
                if (idlen < 0) break
                val (lenlen, datalen, eoclen) = decodeTLVLen(buf, p + idlen) ?: break
                if (lenlen < 0 || datalen < 0 || eoclen != 0) break
                yield(Pair(buf.sliceOffLen(p, idlen), datalen))
                p += idlen + lenlen
            }
        }

    @OptIn(ExperimentalStdlibApi::class)
    fun findBERTLV(
        buf: ByteArray,
        target: String,
        keepHeader: Boolean = false,
        multihead: Boolean = false,
    ): ByteArray? = findBERTLV(buf, target.hexToByteArray(), keepHeader, multihead)

    fun findBERTLV(
        buf: ByteArray,
        target: ByteArray,
        keepHeader: Boolean = false,
        multihead: Boolean = false,
    ): ByteArray? {
        val result =
            berTlvIterate(buf, multihead).firstOrNull {
                it.first.contentEquals(target)
            } ?: return null

        return if (keepHeader) {
            result.second + result.third
        } else {
            result.third
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun findRepeatedBERTLV(
        buf: ByteArray,
        target: String,
        keepHeader: Boolean,
    ): Sequence<ByteArray> = findRepeatedBERTLV(buf, target.hexToByteArray(), keepHeader)

    fun findRepeatedBERTLV(
        buf: ByteArray,
        target: ByteArray,
        keepHeader: Boolean,
    ): Sequence<ByteArray> =
        berTlvIterate(buf).filter { it.first.contentEquals(target) }.map {
            if (keepHeader) {
                it.second + it.third
            } else {
                it.third
            }
        }

    // Backwards-compatible convenience methods (no keepHeader/multihead)

    @OptIn(ExperimentalStdlibApi::class)
    fun findAllBERTLV(
        buf: ByteArray,
        targetHex: String,
    ): List<ByteArray> {
        val target = targetHex.hexToByteArray()
        return berTlvIterate(buf)
            .filter { it.first.contentEquals(target) }
            .map { it.third }
            .toList()
    }

    /**
     * Parses BER-TLV data, and builds [ListItem] and [ListItemRecursive] for each of the tags.
     */
    fun infoBerTLV(
        buf: ByteArray,
        multihead: Boolean = false,
    ): List<ListItemInterface> =
        berTlvIterate(buf, multihead)
            .map { (id, header, data) ->
                if (id[0].toInt() and 0xe0 == 0xa0) {
                    try {
                        ListItemRecursive(
                            FormattedString(id.hex()),
                            null,
                            infoBerTLV(header + data, multihead),
                        )
                    } catch (e: Exception) {
                        println("[ISO7816TLV] Failed to build TLV items: $e")
                        ListItem(id.toHexDump(), data.toHexDump())
                    }
                } else {
                    ListItem(id.toHexDump(), data.toHexDump())
                }
            }.toList()

    fun infoWithRaw(buf: ByteArray) =
        listOfNotNull(
            ListItemRecursive.collapsedValue("Raw", buf.toHexDump()),
            try {
                ListItemRecursive(FormattedString("TLV"), null, infoBerTLV(buf))
            } catch (e: Exception) {
                println("[ISO7816TLV] Failed to decode TLV node: $e")
                null
            },
        )

    fun removeTlvHeader(buf: ByteArray): ByteArray {
        val p = getTLVIDLen(buf, 0)
        val (startoffset, datalen, _) = decodeTLVLen(buf, p) ?: return ByteArray(0)
        return buf.sliceOffLen(p + startoffset, datalen)
    }

    /**
     * Parses BER-TLV data, and builds [ListItem] for each of the tags.
     *
     * This replaces the names with human-readable names, and does not operate recursively.
     * @param includeUnknown If `true`, include tags that did not appear in [tagMap]
     */
    fun infoBerTLV(
        tlv: ByteArray,
        tagMap: Map<String, TagDesc>,
        includeUnknown: Boolean = false,
        multihead: Boolean = false,
    ) = berTlvIterate(tlv, multihead)
        .mapNotNull { (id, _, data) ->
            val idStr = id.hex()
            val d = tagMap[idStr]
            if (d == null) {
                if (includeUnknown) {
                    ListItem(idStr, data.toHexDump())
                } else {
                    null
                }
            } else {
                d.interpretTag(data)
            }
        }.toList()

    /**
     * Like [infoBerTLV], but also returns a list of IDs that were unknown in the process.
     */
    fun infoBerTLVWithUnknowns(
        tlv: ByteArray,
        tagMap: Map<String, TagDesc>,
        multihead: Boolean,
    ): Pair<List<ListItemInterface>, Set<String>> {
        val unknownIds = mutableSetOf<String>()

        return Pair(
            berTlvIterate(tlv, multihead)
                .mapNotNull { (id, _, data) ->
                    val idStr = id.hex()
                    val d = tagMap[idStr]
                    if (d == null) {
                        unknownIds.add(idStr)
                        ListItem(idStr, data.toHexDump())
                    } else {
                        d.interpretTag(data)
                    }
                }.toList(),
            unknownIds.toSet(),
        )
    }

    /**
     * Iterates over Simple-TLV encoded data lazily with a [Sequence].
     *
     * Simple-TLV format is defined in ISO7816-4.
     *
     * @param buf The Simple-TLV encoded data to iterate over
     * @return [Sequence] of [Pair] of `id, data`
     */
    fun simpleTlvIterate(buf: ByteArray): Sequence<Pair<Int, ByteArray>> {
        return sequence {
            // Skip null bytes at start
            var p = buf.indexOfFirst { it != 0.toByte() }

            if (p == -1) {
                return@sequence
            }

            while (p < buf.size) {
                val tag = buf[p++].toInt() and 0xff
                var len = buf[p++].toInt() and 0xff

                // If the length byte is FF, then the length is stored in the subsequent 2 bytes
                // (big-endian).
                if (len == 0xff) {
                    len = buf.byteArrayToInt(p, 2) and 0xffff
                    p += 2
                }

                // Skip empty tag
                if (len < 1) continue

                val d =
                    buf.sliceOffLenSafe(p, len)
                        ?: return@sequence // Invalid length

                yield(Pair(tag, d))
                p += len
            }
        }
    }

    /**
     * Iterates over Compact-TLV encoded data lazily with a [Sequence].
     *
     * @param buf The Compact-TLV encoded data to iterate over
     * @return [Sequence] of [Pair] of `id, data`
     */
    fun compactTlvIterate(buf: ByteArray): Sequence<Pair<Int, ByteArray>> {
        return sequence {
            // Skip null bytes at start
            var p = buf.indexOfFirst { it != 0.toByte() }

            if (p == -1) {
                return@sequence
            }

            while (p < buf.size) {
                val tag = buf[p].toInt() and 0xf0 shr 4
                val len = buf[p++].toInt() and 0xf

                // Skip empty tag
                if (len < 1) continue
                val d =
                    buf.sliceOffLenSafe(p, len)
                        ?: return@sequence // Invalid length

                yield(Pair(tag, d))
                p += len
            }
        }
    }

    fun infoBerTLVs(
        tlvs: List<ByteArray>,
        tagmap: Map<String, TagDesc>,
        hideThings: Boolean,
        multihead: Boolean = false,
    ): List<ListItemInterface> {
        val res =
            mutableListOf<ListItemInterface>(
                HeaderListItem(Res.string.iso7816_tlv_tags),
            )
        val unknownIds = mutableSetOf<String>()
        for (tlv in tlvs) {
            val li =
                if (hideThings) {
                    infoBerTLV(tlv, tagmap, multihead = multihead)
                } else {
                    val (parsed, unknowns) = infoBerTLVWithUnknowns(tlv, tagmap, multihead)
                    unknownIds += unknowns
                    parsed
                }
            res += li
        }

        if (unknownIds.isNotEmpty()) {
            res += ListItem(Res.string.iso7816_unknown_tags, unknownIds.joinToString(", "))
        }

        return res
    }
}
