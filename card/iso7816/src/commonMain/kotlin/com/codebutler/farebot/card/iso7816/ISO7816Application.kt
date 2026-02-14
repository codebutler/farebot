/*
 * ISO7816Application.kt
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

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Represents an application on an ISO 7816 smart card.
 *
 * An ISO 7816 card may contain multiple applications, each identified by an
 * Application Identifier (AID). Each application contains files that can be
 * accessed by path (selector) or Short File Identifier (SFI).
 *
 * @param appName The Application Identifier (AID) as raw bytes.
 * @param appFci File Control Information returned when the application was selected.
 * @param files Map of file selector string to ISO7816File.
 * @param sfiFiles Map of Short File Identifier to ISO7816File.
 * @param type Application type identifier for polymorphic serialization.
 */
@Serializable
data class ISO7816Application(
    @Contextual val appName: ByteArray? = null,
    @Contextual val appFci: ByteArray? = null,
    val files: Map<String, ISO7816File> = emptyMap(),
    val sfiFiles: Map<Int, ISO7816File> = emptyMap(),
    val type: String = "generic",
) {
    /**
     * Extracts the proprietary BER-TLV data (tag A5) from the FCI template.
     * In ISO 7816, the FCI (tag 6F) contains a proprietary template (tag A5)
     * with application-specific data.
     */
    val appProprietaryBerTlv: ByteArray?
        get() {
            val fci = appFci ?: return null
            return ISO7816TLV.findBERTLV(fci, "a5")
        }

    fun getFile(selector: String): ISO7816File? = files[selector]

    fun getSfiFile(sfi: Int): ISO7816File? = sfiFiles[sfi]

    companion object {
        fun create(
            appName: ByteArray? = null,
            appFci: ByteArray? = null,
            files: Map<String, ISO7816File> = emptyMap(),
            sfiFiles: Map<Int, ISO7816File> = emptyMap(),
            type: String = "generic",
        ): ISO7816Application = ISO7816Application(appName, appFci, files, sfiFiles, type)
    }
}
