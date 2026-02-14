/*
 * TagDesc.kt
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

package com.codebutler.farebot.card.iso7816

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.ui.ListItemRecursive
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.convertBCDtoInteger
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.base.util.readASCII
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.base.util.toHexDump
import farebot.farebot_card_iso7816.generated.resources.*

data class TagDesc(
    val name: String,
    val contents: TagContents,
    val hiding: TagHiding = TagHiding.NONE,
) {
    fun interpretTag(data: ByteArray): ListItemInterface? = contents.interpretTag(name, data)

    fun interpretTagString(data: ByteArray): String = contents.interpretTagString(data)
}

interface TagContentsInterface {
    fun interpretTagString(data: ByteArray): String

    fun interpretTag(
        name: String,
        data: ByteArray,
    ): ListItemInterface? =
        if (data.isEmpty()) {
            null
        } else {
            ListItem(name, interpretTagString(data))
        }
}

enum class TagContents : TagContentsInterface {
    DUMP_SHORT {
        override fun interpretTagString(data: ByteArray): String = data.hex()
    },
    DUMP_LONG {
        override fun interpretTagString(data: ByteArray): String = data.hex()

        override fun interpretTag(
            name: String,
            data: ByteArray,
        ): ListItemInterface? = if (data.isEmpty()) null else ListItem(name, data.toHexDump())
    },
    ASCII {
        override fun interpretTagString(data: ByteArray): String = data.readASCII()
    },
    DUMP_UNKNOWN {
        override fun interpretTagString(data: ByteArray): String = data.hex()

        override fun interpretTag(
            name: String,
            data: ByteArray,
        ): ListItemInterface? = if (data.isEmpty()) null else ListItem(name, data.toHexDump())
    },
    HIDE {
        override fun interpretTagString(data: ByteArray): String = ""

        override fun interpretTag(
            name: String,
            data: ByteArray,
        ): ListItemInterface? = null
    },
    CURRENCY {
        override fun interpretTagString(data: ByteArray): String {
            val n = data.byteArrayToInt().convertBCDtoInteger()
            return n.toString()
        }
    },
    FDDA {
        private fun subList(data: ByteArray): List<ListItem> {
            val sl =
                mutableListOf(
                    ListItem(Res.string.iso7816_fdda_version, data.byteArrayToInt(0, 1).toString()),
                    ListItem(Res.string.iso7816_unpredictable_number, data.getHexString(1, 4)),
                    ListItem(Res.string.iso7816_transaction_qualifiers, data.getHexString(5, 2)),
                    ListItem(Res.string.iso7816_rfu, data.getHexString(7, 1)),
                )
            if (data.size > 8) {
                sl.add(ListItem(Res.string.iso7816_fdda_tail, data.sliceOffLen(8, data.size - 8).toHexDump()))
            }
            return sl
        }

        override fun interpretTagString(data: ByteArray): String =
            if (data.size < 8) {
                data.hex()
            } else {
                subList(data).map { "${it.text1.orEmpty()}: ${it.text2.orEmpty()}" }.joinToString(", ")
            }

        override fun interpretTag(
            name: String,
            data: ByteArray,
        ): ListItemInterface {
            if (data.size < 8) {
                return ListItem(name, data.getHexString(0, data.size))
            }
            return ListItemRecursive(name, null, subList(data))
        }
    },
    CONTENTS_DATE {
        private fun adjustYear(yy: Int): Int {
            if (yy < 80) return 2000 + yy
            if (yy in 81..99) return 1900 + yy
            return yy
        }

        override fun interpretTagString(data: ByteArray): String =
            when (data.size) {
                3 -> {
                    val year = adjustYear(data.convertBCDtoInteger(0, 1))
                    val month = data.convertBCDtoInteger(1, 1)
                    val day = data.convertBCDtoInteger(2, 1)
                    "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                }
                2 -> {
                    val year = adjustYear(data.convertBCDtoInteger(0, 1))
                    val month = data.convertBCDtoInteger(1, 1)
                    "$month/$year"
                }
                else -> data.hex()
            }
    },
}

enum class TagHiding {
    NONE,
    CARD_NUMBER,
    DATE,
}

val HIDDEN_TAG = TagDesc("Unknown", TagContents.HIDE)
val UNKNOWN_TAG = TagDesc("Unknown", TagContents.DUMP_UNKNOWN)

/**
 * Extension to convert a BCD integer value to its decimal equivalent.
 */
private fun Int.convertBCDtoInteger(): Int {
    var res = 0
    for (i in 0..7) {
        res = res * 10 + ((this shr (4 * (7 - i))) and 0xf)
    }
    return res
}
