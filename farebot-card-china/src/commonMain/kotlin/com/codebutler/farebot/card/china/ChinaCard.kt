/*
 * ChinaCard.kt
 *
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

package com.codebutler.farebot.card.china

import com.codebutler.farebot.card.iso7816.ISO7816Application
import com.codebutler.farebot.card.iso7816.ISO7816Card
import com.codebutler.farebot.card.iso7816.ISO7816File
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Represents a China transit card based on ISO 7816.
 *
 * China cards include Beijing, Shenzhen, Wuhan Tong, City Union, and T-Union systems.
 * They are identified by specific Application Identifiers (AIDs) and contain balance
 * information that can be read via the GET BALANCE command.
 *
 * @param appName The Application Identifier (AID) as raw bytes.
 * @param appFci File Control Information returned when the application was selected.
 * @param files Map of file selector string to ISO7816File.
 * @param sfiFiles Map of Short File Identifier to ISO7816File.
 * @param balances Map of balance index (0-3) to balance data bytes.
 */
@Serializable
data class ChinaCard(
    @Contextual val appName: ByteArray? = null,
    @Contextual val appFci: ByteArray? = null,
    val files: Map<String, ISO7816File> = emptyMap(),
    val sfiFiles: Map<Int, ISO7816File> = emptyMap(),
    val balances: Map<Int, @Contextual ByteArray> = emptyMap()
) {
    /**
     * Extracts the proprietary BER-TLV data (tag A5) from the FCI template.
     * In ISO 7816, the FCI (tag 6F) contains a proprietary template (tag A5)
     * with application-specific data.
     */
    val appProprietaryBerTlv: ByteArray?
        get() {
            val fci = appFci ?: return null
            return com.codebutler.farebot.card.iso7816.ISO7816TLV.findBERTLV(fci, "a5")
        }

    /**
     * Get a file by selector string.
     */
    fun getFile(selector: String): ISO7816File? = files[selector]

    /**
     * Get a file by Short File Identifier.
     */
    fun getSfiFile(sfi: Int): ISO7816File? = sfiFiles[sfi]

    /**
     * Get balance data by index (0-3).
     */
    fun getBalance(idx: Int): ByteArray? = balances[idx]

    companion object {
        const val TYPE = "china"

        /**
         * Creates a ChinaCard from an ISO7816Application and balance data.
         */
        fun fromISO7816Application(
            app: ISO7816Application,
            balances: Map<Int, ByteArray>
        ): ChinaCard = ChinaCard(
            appName = app.appName,
            appFci = app.appFci,
            files = app.files,
            sfiFiles = app.sfiFiles,
            balances = balances
        )

        /**
         * Extracts a ChinaCard from an ISO7816Card if present.
         * Returns null if no China application is found.
         */
        fun fromISO7816Card(card: ISO7816Card): ChinaCard? {
            // Look for an application with type "china"
            val chinaApp = card.getApplication(TYPE)
            if (chinaApp != null) {
                return fromApplication(chinaApp)
            }

            // Try to find by known China AIDs
            for (factory in ChinaRegistry.allFactories) {
                for (appNameBytes in factory.appNames) {
                    val app = card.getApplicationByName(appNameBytes)
                    if (app != null) {
                        return fromApplication(app)
                    }
                }
            }

            return null
        }

        private fun fromApplication(app: ISO7816Application): ChinaCard {
            // Extract balance data stored with special "balance/N" keys
            val balances = mutableMapOf<Int, ByteArray>()
            val regularFiles = mutableMapOf<String, ISO7816File>()
            for ((key, file) in app.files) {
                if (key.startsWith("balance/")) {
                    val idx = key.removePrefix("balance/").toIntOrNull()
                    val data = file.binaryData
                    if (idx != null && data != null) {
                        balances[idx] = data
                    }
                } else {
                    regularFiles[key] = file
                }
            }
            return ChinaCard(
                appName = app.appName,
                appFci = app.appFci,
                files = regularFiles,
                sfiFiles = app.sfiFiles,
                balances = balances
            )
        }
    }
}
