package com.codebutler.farebot.shared.di

import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.flipper.FlipperTransportFactory
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.shared.core.NavDataHolder
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.platform.Analytics
import com.codebutler.farebot.shared.platform.AppPreferences
import com.codebutler.farebot.shared.plugin.KeyManagerPlugin
import com.codebutler.farebot.shared.serialize.CardImporter
import com.codebutler.farebot.shared.transit.TransitFactoryRegistry
import com.codebutler.farebot.shared.viewmodel.CardViewModel
import com.codebutler.farebot.shared.viewmodel.FlipperViewModel
import com.codebutler.farebot.shared.viewmodel.HistoryViewModel
import com.codebutler.farebot.shared.viewmodel.HomeViewModel
import kotlinx.serialization.json.Json

interface AppGraph {
    val navDataHolder: NavDataHolder
    val cardImporter: CardImporter
    val analytics: Analytics
    val appPreferences: AppPreferences
    val json: Json
    val cardSerializer: CardSerializer
    val cardPersister: CardPersister
    val cardKeysPersister: CardKeysPersister
    val transitFactoryRegistry: TransitFactoryRegistry
    val cardScanner: CardScanner
    val flipperTransportFactory: FlipperTransportFactory
    val keyManagerPlugin: KeyManagerPlugin?

    val homeViewModel: HomeViewModel
    val cardViewModel: CardViewModel
    val historyViewModel: HistoryViewModel
    val flipperViewModel: FlipperViewModel
}
