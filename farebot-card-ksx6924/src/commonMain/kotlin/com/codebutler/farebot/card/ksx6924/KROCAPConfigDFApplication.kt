/*
 * KROCAPConfigDFApplication.kt
 *
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

package com.codebutler.farebot.card.ksx6924

import com.codebutler.farebot.card.iso7816.ISO7816Application
import com.codebutler.farebot.card.iso7816.ISO7816Card
import kotlinx.serialization.Serializable

/**
 * Represents the Config DF (Directory File) specified by One Card All Pass.
 *
 * This application is used by KR-OCAP cards to store configuration data.
 * It is **not** implemented by Snapper cards.
 *
 * This is a secondary application that may be present alongside a KSX6924 application.
 * When a card has both a KSX6924 application and a KR-OCAP Config DF, the KSX6924
 * application should be used for parsing transit data.
 *
 * @property application The underlying ISO7816 application data.
 */
@Serializable
data class KROCAPConfigDFApplication(
    val application: ISO7816Application
) {

    companion object {
        const val TYPE = "kr_ocap_configdf"
        const val NAME = "KR-OCAP"

        /**
         * Application name (AID) that identifies a KR-OCAP Config DF.
         */
        @OptIn(ExperimentalStdlibApi::class)
        val APP_NAME: ByteArray = "a0000004520001".hexToByteArray()

        /**
         * Checks if the given application is a KR-OCAP Config DF.
         */
        @OptIn(ExperimentalStdlibApi::class)
        fun isKROCAPConfigDF(app: ISO7816Application): Boolean {
            val appName = app.appName ?: return false
            return appName.contentEquals(APP_NAME)
        }

        /**
         * Checks if the ISO7816 card has a KSX6924 application.
         *
         * This is used to determine whether to use the KR-OCAP Config DF
         * or defer to the KSX6924 application for parsing.
         */
        fun hasKSX6924Application(card: ISO7816Card): Boolean {
            return card.applications.any { KSX6924Application.isKSX6924(it) }
        }
    }
}
