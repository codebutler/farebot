/*
 * KROCAPData.kt
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

import com.codebutler.farebot.card.iso7816.HIDDEN_TAG
import com.codebutler.farebot.card.iso7816.TagContents
import com.codebutler.farebot.card.iso7816.TagDesc

/**
 * Tag definitions for KR-OCAP (One Card All Pass) cards.
 *
 * KR-OCAP is a Korean transit card standard that extends KSX6924.
 * This object contains the TLV tag definitions for parsing card data.
 */
object KROCAPData {
    private const val TAG_BALANCE_COMMAND = "11"
    const val TAG_SERIAL_NUMBER = "12"
    private const val TAG_AGENCY_SERIAL_NUMBER = "13"
    private const val TAG_CARD_ISSUER = "43"
    private const val TAG_TICKET_TYPE = "45"
    private const val TAG_SUPPORTED_PROTOCOLS = "47"
    private const val TAG_ADF_AID = "4f"
    private const val TAG_CARDTYPE = "50"
    private const val TAG_EXPIRY_DATE = "5f24"
    private const val TAG_ADDITIONAL_FILE_REFERENCES = "9f10"
    private const val TAG_DISCRETIONARY_DATA = "bf0c"

    /**
     * Tag map for parsing KR-OCAP TLV data.
     */
    val TAGMAP: Map<String, TagDesc> = mapOf(
        TAG_CARDTYPE to TagDesc("Card Type", TagContents.DUMP_SHORT),
        TAG_SUPPORTED_PROTOCOLS to TagDesc("Supported Protocols", TagContents.DUMP_SHORT),
        TAG_CARD_ISSUER to TagDesc("Card Issuer", TagContents.DUMP_SHORT),
        TAG_BALANCE_COMMAND to TagDesc("Balance Command", TagContents.DUMP_SHORT),
        TAG_ADF_AID to TagDesc("ADF AID", TagContents.DUMP_SHORT),
        TAG_ADDITIONAL_FILE_REFERENCES to TagDesc("Additional File References", TagContents.DUMP_SHORT),
        TAG_TICKET_TYPE to TagDesc("Ticket Type", TagContents.DUMP_SHORT),
        TAG_EXPIRY_DATE to TagDesc("Expiry Date", TagContents.DUMP_SHORT),
        TAG_SERIAL_NUMBER to HIDDEN_TAG, // Card serial number - hidden for privacy
        TAG_AGENCY_SERIAL_NUMBER to TagDesc("Agency Card Serial Number", TagContents.DUMP_SHORT),
        TAG_DISCRETIONARY_DATA to TagDesc("Discretionary Data", TagContents.DUMP_SHORT)
    )
}
