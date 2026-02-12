/*
 * CalypsoTransitFactory.kt
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

package com.codebutler.farebot.transit.calypso

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.iso7816.ISO7816Application
import com.codebutler.farebot.card.iso7816.ISO7816Card
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.en1545.CalypsoConstants
import com.codebutler.farebot.transit.en1545.getBitsFromBuffer

/**
 * Base class for Calypso ISO 7816 transit system factories.
 * Subclasses implement [checkTenv] to match on the ticket environment data.
 */
abstract class CalypsoTransitFactory(protected val stringResource: StringResource) : TransitFactory<ISO7816Card, TransitInfo> {

    override val allCards: List<CardInfo> = emptyList()

    abstract val name: String

    abstract fun checkTenv(tenv: ByteArray): Boolean

    abstract fun parseTransitInfo(app: ISO7816Application, serial: String?): TransitInfo

    open fun getSerial(app: ISO7816Application): String? = null

    protected fun findCalypsoApp(card: ISO7816Card): ISO7816Application? {
        return card.getApplication("calypso")
            ?: card.applications.firstOrNull { it.sfiFiles.isNotEmpty() }
    }

    override fun check(card: ISO7816Card): Boolean {
        val app = findCalypsoApp(card) ?: return false
        val file = app.sfiFiles[CalypsoConstants.SFI_TICKETING_ENVIRONMENT] ?: return false
        val tenv = file.records.entries.sortedBy { it.key }.firstOrNull()?.value ?: return false
        return try {
            checkTenv(tenv)
        } catch (_: Exception) {
            false
        }
    }

    override fun parseIdentity(card: ISO7816Card): TransitIdentity {
        val app = findCalypsoApp(card)!!
        return TransitIdentity.create(name, getSerial(app))
    }

    override fun parseInfo(card: ISO7816Card): TransitInfo {
        val app = findCalypsoApp(card)!!
        return parseTransitInfo(app, getSerial(app))
    }
}
