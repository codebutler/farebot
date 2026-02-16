/*
 * TagReader.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card

import android.nfc.Tag
import com.codebutler.farebot.card.nfc.NfcTechnology
import com.codebutler.farebot.key.CardKeys

abstract class TagReader<
    T : NfcTechnology,
    C : RawCard<*>,
    K : CardKeys,
>(
    private val mTagId: ByteArray,
    private val mTag: Tag,
    private val mCardKeys: K?,
) {
    @Throws(Exception::class)
    suspend fun readTag(): C {
        val tech = getTech(mTag)
        try {
            tech.connect()
            return readTag(mTagId, mTag, tech, mCardKeys)
        } finally {
            if (tech.isConnected) {
                try {
                    tech.close()
                } catch (_: Exception) {
                    // ignore
                }
            }
        }
    }

    @Throws(Exception::class)
    protected abstract suspend fun readTag(
        tagId: ByteArray,
        tag: Tag,
        tech: T,
        cardKeys: K?,
    ): C

    protected abstract fun getTech(tag: Tag): T
}
