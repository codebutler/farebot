/*
 * FareBotApplicationComponent.kt
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

package com.codebutler.farebot.app.core.app

import android.content.SharedPreferences
import com.codebutler.farebot.app.core.nfc.TagReaderFactory
import com.codebutler.farebot.app.core.serialize.CardKeysSerializer
import com.codebutler.farebot.app.core.transit.TransitFactoryRegistry
import com.codebutler.farebot.app.core.util.ExportHelper
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.CardPersister
import dagger.BindsInstance
import dagger.Component

@Component(modules = arrayOf(FareBotApplicationModule::class))
interface FareBotApplicationComponent {

    fun application(): FareBotApplication

    fun cardPersister(): CardPersister

    fun cardSerializer(): CardSerializer

    fun cardKeysPersister(): CardKeysPersister

    fun cardKeysSerializer(): CardKeysSerializer

    fun exportHelper(): ExportHelper

    fun sharedPreferences(): SharedPreferences

    fun tagReaderFactory(): TagReaderFactory

    fun transitFactoryRegistry(): TransitFactoryRegistry

    fun inject(application: FareBotApplication)

    @Component.Builder
    interface Builder {

        fun module(module: FareBotApplicationModule): Builder

        @BindsInstance
        fun application(application: FareBotApplication): Builder

        fun build(): FareBotApplicationComponent
    }
}
