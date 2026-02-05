/*
 * NdefEntry.kt
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

import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.ui.ListItemRecursive
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.isASCII
import com.codebutler.farebot.base.util.readASCII
import com.codebutler.farebot.base.util.readLatin1
import com.codebutler.farebot.base.util.readUTF16BOM
import com.codebutler.farebot.base.util.readUTF8
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.base.util.toHexDump
import farebot.farebot_transit_ndef.generated.resources.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@Serializable
sealed class NdefEntry {
    abstract val tnf: Int
    abstract val type: ByteArray
    abstract val id: ByteArray?
    abstract val payload: ByteArray

    private val headInfo: List<ListItemInterface>
        get() = listOfNotNull(
            HeaderListItem(runBlocking { getString(name) }),
            id?.let {
                ListItem(
                    Res.string.ndef_id,
                    if (it.isASCII()) it.readASCII() else it.toHexDump()
                )
            }
        )

    val info: List<ListItemInterface>
        get() = headInfo + payloadInfo

    open val payloadInfo: List<ListItemInterface>
        get() = listOf(
            ListItem(
                Res.string.ndef_type,
                if (type.isASCII()) type.readASCII() else type.toHexDump()
            ),
            ListItem(
                Res.string.ndef_payload,
                payload.toHexDump()
            ),
        )

    protected abstract val name: StringResource

    override fun toString(): String = "[name=$name, id=$id, tnf=$tnf, type=${type.toHexDump()}, payload=${payload.toHexDump()}]"
}

@Serializable
data class NdefEmpty(
    override val tnf: Int,
    override val type: ByteArray,
    override val id: ByteArray?,
    override val payload: ByteArray
) : NdefEntry() {
    override val name: StringResource
        get() = Res.string.ndef_empty_record
}

@Serializable
sealed class NdefRTD : NdefEntry()

@Serializable
data class NdefUnknownRTD(
    override val tnf: Int,
    override val type: ByteArray,
    override val id: ByteArray?,
    override val payload: ByteArray
) : NdefRTD() {
    override val name: StringResource
        get() = Res.string.ndef_rtd_record
}

@Serializable
data class NdefUri(
    override val tnf: Int,
    override val type: ByteArray,
    override val id: ByteArray?,
    override val payload: ByteArray
) : NdefRTD() {
    override val name: StringResource
        get() = Res.string.ndef_uri_record

    override val payloadInfo: List<ListItemInterface>
        get() = listOf(
            ListItem(Res.string.ndef_uri, uri)
        )

    private val uriSuffix: String
        get() = payload.readUTF8(start = 1)

    private val uriPrefix: String
        get() = when (payload[0].toInt() and 0xff) {
            0x00 -> ""
            0x01 -> "http://www."
            0x02 -> "https://www."
            0x03 -> "http://"
            0x04 -> "https://"
            0x05 -> "tel:"
            0x06 -> "mailto:"
            0x07 -> "ftp://anonymous:anonymous@"
            0x08 -> "ftp://ftp."
            0x09 -> "ftps://"
            0x0A -> "sftp://"
            0x0B -> "smb://"
            0x0C -> "nfs://"
            0x0D -> "ftp://"
            0x0E -> "dav://"
            0x0F -> "news:"
            0x10 -> "telnet://"
            0x11 -> "imap:"
            0x12 -> "rtsp://"
            0x13 -> "urn:"
            0x14 -> "pop:"
            0x15 -> "sip:"
            0x16 -> "sips:"
            0x17 -> "tftp:"
            0x18 -> "btspp://"
            0x19 -> "btl2cap://"
            0x1A -> "btgoep://"
            0x1B -> "tcpobex://"
            0x1C -> "irdaobex://"
            0x1D -> "file://"
            0x1E -> "urn:epc:id:"
            0x1F -> "urn:epc:tag:"
            0x20 -> "urn:epc:pat:"
            0x21 -> "urn:epc:raw:"
            0x22 -> "urn:epc:"
            0x23 -> "urn:nfc:"
            else -> "[${payload[0].toInt()}]:"
        }

    val uri: String
        get() = "$uriPrefix$uriSuffix"

    override fun toString(): String = "[URL: id=$id, value=$uri]"
}

@Serializable
data class NdefText(
    override val tnf: Int,
    override val type: ByteArray,
    override val id: ByteArray?,
    override val payload: ByteArray
) : NdefRTD() {
    override val name: StringResource
        get() = Res.string.ndef_text_record

    override val payloadInfo: List<ListItemInterface>
        get() = listOf(
            ListItem(
                Res.string.ndef_text_encoding,
                if (isUTF16) "UTF-16" else "UTF-8"
            ),
            ListItem(
                Res.string.ndef_text_language,
                language
            ),
            ListItem(
                Res.string.ndef_text,
                text
            )
        )

    val isUTF16: Boolean
        get() = payload[0].toInt() and 0x80 != 0

    val languageCode: String
        get() = payload.sliceOffLen(1, langLen).readASCII()

    val language: String
        get() = languageCodeToName(languageCode) ?: languageCode

    private val langLen get() = payload[0].toInt() and 0x3f

    val text: String
        get() = if (isUTF16) payload.readUTF16BOM(
            isLittleEndianDefault = false,
            start = langLen + 1
        ) else payload.readUTF8(langLen + 1)

    override fun toString(): String =
        "[Text: id=$id, language=$language, isUTF16=$isUTF16, value=$text]"
}

@Serializable
sealed class NdefMIME : NdefEntry()

@Serializable
data class NdefUnknownMIME(
    override val tnf: Int,
    override val type: ByteArray,
    override val id: ByteArray?,
    override val payload: ByteArray
) : NdefMIME() {
    override val name: StringResource
        get() = Res.string.ndef_mime_record
}

@Serializable
data class NdefWifi(
    override val tnf: Int,
    override val type: ByteArray,
    override val id: ByteArray?,
    override val payload: ByteArray
) : NdefMIME() {
    override val name: StringResource
        get() = Res.string.ndef_wifi_record

    data class Record(val type: Int, val value: ByteArray)

    override val payloadInfo
        get() = flatEntries.map {
            when (it.type) {
                0x1001 -> ListItem(
                    Res.string.ndef_wifi_ap_channel,
                    it.value.byteArrayToInt(0, 2).toString()
                )
                0x1003 -> ListItem(
                    Res.string.ndef_wifi_auth_types,
                    formatBitmap(it, authTypes, 2)
                )
                0x100f -> ListItem(
                    Res.string.ndef_wifi_enc_types,
                    formatBitmap(it, encTypes, 2)
                )
                0x1011 -> ListItem(
                    Res.string.ndef_wifi_device_name,
                    it.value.readUTF8()
                )
                0x1020 -> ListItem(
                    Res.string.ndef_wifi_mac_address,
                    it.value.joinToString(":") { it2 ->
                        NumberUtils.zeroPad((it2.toInt() and 0xff).toString(16), 2)
                    }
                )
                0x1021 -> ListItem(
                    Res.string.ndef_wifi_manufacturer,
                    it.value.readLatin1()
                )
                0x1023 -> ListItem(
                    Res.string.ndef_wifi_model_name,
                    it.value.readLatin1()
                )
                0x1024 -> ListItem(
                    Res.string.ndef_wifi_model_number,
                    it.value.readLatin1()
                )
                0x1026 -> ListItem(
                    Res.string.ndef_wifi_network_index,
                    it.value.byteArrayToInt(0, 1).toString()
                )
                0x1027 -> ListItem(
                    Res.string.ndef_wifi_password,
                    it.value.readLatin1()
                )
                0x103c -> ListItem(
                    Res.string.ndef_wifi_bands,
                    formatBitmap(it, bands, 1)
                )
                0x1042 -> ListItem(
                    Res.string.ndef_wifi_serial_number,
                    it.value.readLatin1()
                )
                0x1045 -> ListItem(
                    Res.string.ndef_wifi_ssid,
                    it.value.readLatin1()
                )
                0x1047 -> ListItem(
                    Res.string.ndef_wifi_uuid_enrollee,
                    formatUUID(it)
                )
                0x1048 -> ListItem(
                    Res.string.ndef_wifi_uuid_registrar,
                    formatUUID(it)
                )
                0x1049 -> if (it.value.size >= 5
                    && it.value.byteArrayToInt(0, 3) == 0x372A) {
                    ListItemRecursive(
                        runBlocking { getString(Res.string.ndef_wifi_wfa_extension) },
                        null,
                        infoWfaExtension(it.value)
                    )
                } else {
                    ListItem(
                        Res.string.ndef_wifi_unknown_vendor_extension,
                        it.value.toHexDump()
                    )
                }
                0x104a -> ListItem(
                    Res.string.ndef_wifi_version1,
                    "${(it.value[0].toInt() and 0xf0) shr 4}.${it.value[0].toInt() and 0xf}"
                )
                0x1061 -> ListItem(
                    Res.string.ndef_wifi_key_provided_automatically,
                    if (it.value[0] != 0.toByte())
                        Res.string.ndef_wifi_key_provided_automatically_yes
                    else
                        Res.string.ndef_wifi_key_provided_automatically_no
                )
                else -> ListItem(
                    runBlocking { getString(Res.string.ndef_wifi_unknown, it.type.toString(16)) },
                    it.value.toHexDump()
                )
            }
        }.toList()

    val entries: Sequence<Record>
        get() = entriesFromBytes(payload)

    val flatEntries: Sequence<Record>
        get() = entries.flatMap { parent ->
            if (parent.type == 0x100e) {
                entriesFromBytes(parent.value)
            } else {
                listOf(parent).asSequence()
            }
        }

    companion object {
        private fun infoWfaExtension(payload: ByteArray): List<ListItemInterface> =
            entriesFromBytes(payload.sliceOffLen(3, payload.size - 3), 1).map {
                when (it.type) {
                    0 -> ListItem(
                        Res.string.ndef_wifi_version2,
                        "${(it.value[0].toInt() and 0xf0) shr 4}.${it.value[0].toInt() and 0xf}"
                    )
                    2 -> ListItem(
                        Res.string.ndef_wifi_key_is_shareable,
                        if (it.value[0].toInt() != 0)
                            Res.string.ndef_wifi_key_is_shareable_yes
                        else
                            Res.string.ndef_wifi_key_is_shareable_no
                    )
                    else -> ListItem(
                        runBlocking { getString(Res.string.ndef_wifi_unknown, it.type.toString(16)) },
                        it.value.toHexDump()
                    )
                }
            }.toList()

        private fun entriesFromBytes(bytes: ByteArray, fieldLen: Int = 2): Sequence<Record> = sequence {
            var ptr = 0
            while (ptr + 2 * fieldLen <= bytes.size) {
                val l = bytes.byteArrayToInt(ptr + fieldLen, fieldLen)
                if (ptr + l + 2 * fieldLen > bytes.size)
                    break
                yield(
                    Record(
                        type = bytes.byteArrayToInt(ptr, fieldLen),
                        value = bytes.sliceOffLen(ptr + 2 * fieldLen, l)
                    )
                )
                ptr += 2 * fieldLen + l
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        private fun formatUUID(record: Record): String =
            NumberUtils.groupString(
                record.value.toHexString(),
                "-", 8, 4, 4, 4
            )

        private fun formatBitmap(
            record: Record,
            bitmapDefinition: List<StringResource>,
            len: Int
        ): String {
            val builder = StringBuilder()
            val bitmap = record.value.byteArrayToInt(0, len)

            for (i in bitmapDefinition.indices) {
                if (bitmap and (1 shl i) == 0) {
                    continue
                }
                if (builder.isNotEmpty()) {
                    builder.append(", ")
                }
                builder.append(runBlocking { getString(bitmapDefinition[i]) })
            }

            for (i in bitmapDefinition.size until (8 * len)) {
                if (bitmap and (1 shl i) == 0) {
                    continue
                }
                if (builder.isNotEmpty()) {
                    builder.append(", ")
                }
                builder.append(runBlocking { getString(Res.string.ndef_wifi_bitmap_unknown, i) })
            }

            return builder.toString()
        }

        private val authTypes = listOf(
            Res.string.ndef_wifi_authtype_open,
            Res.string.ndef_wifi_authtype_wpa_personal,
            Res.string.ndef_wifi_authtype_wep_shared,
            Res.string.ndef_wifi_authtype_wpa_enterprise,
            Res.string.ndef_wifi_authtype_wpa2_enterprise,
            Res.string.ndef_wifi_authtype_wpa2_personal
        )

        private val encTypes = listOf(
            Res.string.ndef_wifi_enctype_unencrypted,
            Res.string.ndef_wifi_enctype_wep,
            Res.string.ndef_wifi_enctype_tkip,
            Res.string.ndef_wifi_enctype_aes
        )

        private val bands = listOf(
            Res.string.ndef_wifi_band_2_4,
            Res.string.ndef_wifi_band_5,
            Res.string.ndef_wifi_band_60
        )
    }
}

@Serializable
data class NdefUriType(
    override val tnf: Int,
    override val type: ByteArray,
    override val id: ByteArray?,
    override val payload: ByteArray
) : NdefEntry() {
    override val name: StringResource
        get() = Res.string.ndef_uri_typed_record
}

@Serializable
sealed class NdefExtType : NdefEntry()

@Serializable
data class NdefUnknownExtType(
    override val tnf: Int,
    override val type: ByteArray,
    override val id: ByteArray?,
    override val payload: ByteArray
) : NdefExtType() {
    override val name: StringResource
        get() = Res.string.ndef_ext_typed_record
}

@Serializable
data class NdefAndroidPkg(
    override val tnf: Int,
    override val type: ByteArray,
    override val id: ByteArray?,
    override val payload: ByteArray
) : NdefExtType() {
    override val name: StringResource
        get() = Res.string.ndef_android_pkg_record

    override val payloadInfo: List<ListItemInterface>
        get() = listOf(
            ListItem(Res.string.ndef_android_pkg_value, pkgName)
        )

    val pkgName: String
        get() = payload.readUTF8()

    override fun toString(): String = "[Android pkg: $pkgName]"
}

@Serializable
data class NdefBinaryType(
    override val tnf: Int,
    override val type: ByteArray,
    override val id: ByteArray?,
    override val payload: ByteArray
) : NdefEntry() {
    override val name: StringResource
        get() = Res.string.ndef_binary_record
}

@Serializable
data class NdefInvalidType(
    override val tnf: Int,
    override val type: ByteArray,
    override val id: ByteArray?,
    override val payload: ByteArray
) : NdefEntry() {
    override val name: StringResource
        get() = Res.string.ndef_invalid_record

    override val payloadInfo: List<ListItemInterface>
        get() = listOf(ListItem(Res.string.ndef_tnf, "$tnf")) + super.payloadInfo
}

/**
 * Converts a language code (e.g., "en", "ja") to a human-readable name.
 * Returns null if the language code is not recognized.
 */
internal fun languageCodeToName(code: String): String? {
    // Common language codes
    return when (code.lowercase()) {
        "en" -> "English"
        "en-us" -> "English (US)"
        "en-gb" -> "English (UK)"
        "ja" -> "Japanese"
        "zh" -> "Chinese"
        "zh-cn" -> "Chinese (Simplified)"
        "zh-tw" -> "Chinese (Traditional)"
        "ko" -> "Korean"
        "de" -> "German"
        "fr" -> "French"
        "es" -> "Spanish"
        "it" -> "Italian"
        "pt" -> "Portuguese"
        "ru" -> "Russian"
        "ar" -> "Arabic"
        "nl" -> "Dutch"
        "sv" -> "Swedish"
        "fi" -> "Finnish"
        "no" -> "Norwegian"
        "da" -> "Danish"
        "pl" -> "Polish"
        "cs" -> "Czech"
        "hu" -> "Hungarian"
        "tr" -> "Turkish"
        "el" -> "Greek"
        "he" -> "Hebrew"
        "th" -> "Thai"
        "vi" -> "Vietnamese"
        "id" -> "Indonesian"
        "ms" -> "Malay"
        else -> null
    }
}
