/*
 * TransitFactoryRegistry.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.app.core.transit

import android.content.Context
import com.codebutler.farebot.app.core.sample.SampleCard
import com.codebutler.farebot.app.core.sample.SampleTransitFactory
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.cepas.CEPASCard
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.felica.FelicaCard
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.bilhete_unico.BilheteUnicoSPTransitFactory
import com.codebutler.farebot.transit.clipper.ClipperTransitFactory
import com.codebutler.farebot.transit.easycard.EasyCardTransitFactory
import com.codebutler.farebot.transit.edy.EdyTransitFactory
import com.codebutler.farebot.transit.ezlink.EZLinkTransitFactory
import com.codebutler.farebot.transit.hsl.HSLTransitFactory
import com.codebutler.farebot.transit.manly_fast_ferry.ManlyFastFerryTransitFactory
import com.codebutler.farebot.transit.myki.MykiTransitFactory
import com.codebutler.farebot.transit.octopus.OctopusTransitFactory
import com.codebutler.farebot.transit.opal.OpalTransitFactory
import com.codebutler.farebot.transit.orca.OrcaTransitFactory
import com.codebutler.farebot.transit.ovc.OVChipTransitFactory
import com.codebutler.farebot.transit.seq_go.SeqGoTransitFactory
import com.codebutler.farebot.transit.stub.AdelaideMetrocardStubTransitFactory
import com.codebutler.farebot.transit.stub.AtHopStubTransitFactory
import com.codebutler.farebot.transit.suica.SuicaTransitFactory

class TransitFactoryRegistry(context: Context) {

    private val registry = mutableMapOf<Class<out Card>, MutableList<TransitFactory<Card, TransitInfo>>>()

    init {
        registerFactory(FelicaCard::class.java, SuicaTransitFactory(context))
        registerFactory(FelicaCard::class.java, EdyTransitFactory())
        registerFactory(FelicaCard::class.java, OctopusTransitFactory())

        registerFactory(DesfireCard::class.java, OrcaTransitFactory())
        registerFactory(DesfireCard::class.java, ClipperTransitFactory())
        registerFactory(DesfireCard::class.java, HSLTransitFactory())
        registerFactory(DesfireCard::class.java, OpalTransitFactory())
        registerFactory(DesfireCard::class.java, MykiTransitFactory())
        registerFactory(DesfireCard::class.java, AdelaideMetrocardStubTransitFactory())
        registerFactory(DesfireCard::class.java, AtHopStubTransitFactory())

        registerFactory(ClassicCard::class.java, OVChipTransitFactory(context))
        registerFactory(ClassicCard::class.java, BilheteUnicoSPTransitFactory())
        registerFactory(ClassicCard::class.java, ManlyFastFerryTransitFactory())
        registerFactory(ClassicCard::class.java, SeqGoTransitFactory(context))
        registerFactory(ClassicCard::class.java, EasyCardTransitFactory(context))

        registerFactory(CEPASCard::class.java, EZLinkTransitFactory())

        registerFactory(SampleCard::class.java, SampleTransitFactory())
    }

    fun parseTransitIdentity(card: Card): TransitIdentity? = findFactory(card)?.parseIdentity(card)

    fun parseTransitInfo(card: Card): TransitInfo? = findFactory(card)?.parseInfo(card)

    @Suppress("UNCHECKED_CAST")
    private fun registerFactory(cardClass: Class<out Card>, factory: TransitFactory<*, *>) {
        var factories = registry[cardClass]
        if (factories == null) {
            factories = mutableListOf()
            registry[cardClass] = factories
        }
        factories.add(factory as TransitFactory<Card, TransitInfo>)
    }

    private fun findFactory(card: Card): TransitFactory<Card, out TransitInfo>?
            = registry[card.parentClass]?.find { it.check(card) }
}
