/*
 * ISO7816Card.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Represents an ISO 7816-4 smart card.
 *
 * ISO 7816 cards use APDU (Application Protocol Data Unit) commands for communication.
 * They can contain multiple applications, each identified by an AID (Application Identifier).
 * This is the base card type for Calypso, T-Money, EMV, and other ISO 7816-based cards.
 */
@Serializable
data class ISO7816Card(
    @Contextual override val tagId: ByteArray,
    override val scannedAt: Instant,
    val applications: List<ISO7816Application>,
) : Card() {
    override val cardType: CardType = CardType.ISO7816

    fun getApplication(type: String): ISO7816Application? = applications.firstOrNull { it.type == type }

    @OptIn(ExperimentalStdlibApi::class)
    fun getApplicationByName(appName: ByteArray): ISO7816Application? =
        applications.firstOrNull { it.appName?.toHexString() == appName.toHexString() }

    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree {
        val cardUiBuilder = FareBotUiTree.builder(stringResource)

        val appsUiBuilder = cardUiBuilder.item().title("Applications")
        for (app in applications) {
            val appUiBuilder = appsUiBuilder.item()
            val appNameStr = app.appName?.let { formatAID(it) } ?: "Unknown"
            appUiBuilder.title("Application: $appNameStr (${app.type})")

            // Show files
            if (app.files.isNotEmpty()) {
                val filesUiBuilder = appUiBuilder.item().title("Files")
                for ((selector, file) in app.files) {
                    val fileUiBuilder = filesUiBuilder.item().title("File: $selector")
                    if (file.binaryData != null) {
                        fileUiBuilder.item().title("Binary Data").value(file.binaryData)
                    }
                    for ((index, record) in file.records.entries.sortedBy { it.key }) {
                        fileUiBuilder.item().title("Record $index").value(record)
                    }
                }
            }

            // Show SFI files
            if (app.sfiFiles.isNotEmpty()) {
                val sfiUiBuilder = appUiBuilder.item().title("SFI Files")
                for ((sfi, file) in app.sfiFiles.entries.sortedBy { it.key }) {
                    val fileUiBuilder = sfiUiBuilder.item().title("SFI 0x${sfi.toString(16)}")
                    if (file.binaryData != null) {
                        fileUiBuilder.item().title("Binary Data").value(file.binaryData)
                    }
                    for ((index, record) in file.records.entries.sortedBy { it.key }) {
                        fileUiBuilder.item().title("Record $index").value(record)
                    }
                }
            }
        }

        return cardUiBuilder.build()
    }

    companion object {
        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            applications: List<ISO7816Application>,
        ): ISO7816Card = ISO7816Card(tagId, scannedAt, applications)

        @OptIn(ExperimentalStdlibApi::class)
        private fun formatAID(aid: ByteArray): String =
            aid
                .toHexString()
                .uppercase()
                .chunked(2)
                .joinToString(":")
    }
}
