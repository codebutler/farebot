/*
 * FelicaCard.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2013 Chris Norden <thisiscnn@gmail.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Contains improvements ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.card.felica

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class FelicaCard(
    @Contextual override val tagId: ByteArray,
    override val scannedAt: Instant,
    @Contextual val idm: FeliCaIdm,
    @Contextual val pmm: FeliCaPmm,
    val systems: List<FelicaSystem>,
    val isPartialRead: Boolean = false
) : Card() {

    override val cardType: CardType = CardType.FeliCa

    private val systemsByCode: Map<Int, FelicaSystem> by lazy {
        systems.associateBy { it.code }
    }

    fun getSystem(systemCode: Int): FelicaSystem? = systemsByCode[systemCode]

    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree {
        val cardUiBuilder = FareBotUiTree.builder(stringResource)
        cardUiBuilder.item().title("IDm").value(idm)
        cardUiBuilder.item().title("PMm").value(pmm)
        val systemsUiBuilder = cardUiBuilder.item().title("Systems")
        for (system in systems) {
            val systemUiBuilder = systemsUiBuilder.item()
                .title("System: ${system.code.toString(16)}")
            for (service in system.services) {
                val serviceUiBuilder = systemUiBuilder.item()
                    .title(
                        "Service: 0x${service.serviceCode.toString(16)} (${FelicaUtils.getFriendlyServiceName(system.code, service.serviceCode)})"
                    )
                for (block in service.blocks) {
                    serviceUiBuilder.item()
                        .title("Block ${block.address.toString().padStart(2, '0')}")
                        .value(block.data)
                }
            }
        }
        return cardUiBuilder.build()
    }

    companion object {
        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            idm: FeliCaIdm,
            pmm: FeliCaPmm,
            systems: List<FelicaSystem>,
            isPartialRead: Boolean = false
        ): FelicaCard {
            return FelicaCard(tagId, scannedAt, idm, pmm, systems, isPartialRead)
        }
    }
}
