package com.codebutler.farebot.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.codebutler.farebot.base.util.BundledDatabaseDriverFactory
import com.codebutler.farebot.base.util.DefaultStringResource
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.shared.serialize.FareBotSerializersModule
import com.codebutler.farebot.shared.serialize.KotlinxCardSerializer
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.DbCardKeysPersister
import com.codebutler.farebot.persist.db.DbCardPersister
import com.codebutler.farebot.persist.db.FareBotDb
import com.codebutler.farebot.shared.di.sharedModule
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.nfc.IosNfcScanner
import com.codebutler.farebot.shared.platform.IosPlatformActions
import com.codebutler.farebot.shared.sample.SampleTransitFactory
import com.codebutler.farebot.shared.serialize.CardImporter
import com.codebutler.farebot.shared.transit.TransitFactoryRegistry
import com.codebutler.farebot.shared.ui.screen.ALL_SUPPORTED_CARDS
import com.codebutler.farebot.transit.clipper.ClipperTransitFactory
import com.codebutler.farebot.transit.edy.EdyTransitFactory
import com.codebutler.farebot.transit.ezlink.EZLinkTransitFactory
import com.codebutler.farebot.transit.hsl.HSLTransitFactory
import com.codebutler.farebot.transit.kmt.KMTTransitFactory
import com.codebutler.farebot.transit.mrtj.MRTJTransitFactory
import com.codebutler.farebot.transit.myki.MykiTransitFactory
import com.codebutler.farebot.transit.octopus.OctopusTransitFactory
import com.codebutler.farebot.transit.opal.OpalTransitFactory
import com.codebutler.farebot.transit.orca.OrcaTransitFactory
import com.codebutler.farebot.transit.suica.SuicaTransitFactory
import com.codebutler.farebot.transit.clipper.ClipperUltralightTransitFactory
import com.codebutler.farebot.transit.troika.TroikaUltralightTransitFactory
import com.codebutler.farebot.transit.calypso.venezia.VeneziaUltralightTransitFactory
import com.codebutler.farebot.transit.calypso.pisa.PisaUltralightTransitFactory
import com.codebutler.farebot.transit.ovc.OVChipUltralightTransitFactory
import com.codebutler.farebot.transit.hsl.HSLUltralightTransitFactory
import com.codebutler.farebot.transit.serialonly.MRTUltralightTransitFactory
import com.codebutler.farebot.transit.amiibo.AmiiboTransitFactory
import com.codebutler.farebot.transit.serialonly.BlankUltralightTransitFactory
import com.codebutler.farebot.transit.serialonly.LockedUltralightTransitFactory
import com.codebutler.farebot.transit.vicinity.BlankVicinityTransitFactory
import com.codebutler.farebot.transit.vicinity.UnknownVicinityTransitFactory
import com.codebutler.farebot.transit.calypso.opus.OpusTransitFactory
import com.codebutler.farebot.transit.calypso.ravkav.RavKavTransitFactory
import com.codebutler.farebot.transit.calypso.mobib.MobibTransitInfo
import com.codebutler.farebot.transit.calypso.venezia.VeneziaTransitFactory
import com.codebutler.farebot.transit.calypso.pisa.PisaTransitFactory
import com.codebutler.farebot.transit.calypso.lisboaviva.LisboaVivaTransitInfo
import com.codebutler.farebot.transit.calypso.emv.EmvTransitFactory
import com.codebutler.farebot.transit.calypso.intercode.IntercodeTransitFactory
import com.codebutler.farebot.transit.tmoney.TMoneyTransitFactory
import com.codebutler.farebot.transit.nextfareul.NextfareUnknownUltralightTransitInfo
import com.codebutler.farebot.transit.ventra.VentraUltralightTransitInfo
import com.codebutler.farebot.transit.yvr_compass.CompassUltralightTransitInfo
import com.codebutler.farebot.transit.tfi_leap.LeapTransitFactory
import com.codebutler.farebot.transit.adelaide.AdelaideTransitFactory
import com.codebutler.farebot.transit.hafilat.HafilatTransitFactory
import com.codebutler.farebot.transit.intercard.IntercardTransitFactory
import com.codebutler.farebot.transit.magnacarta.MagnaCartaTransitFactory
import com.codebutler.farebot.transit.tampere.TampereTransitFactory
import com.codebutler.farebot.transit.serialonly.AtHopTransitFactory
import com.codebutler.farebot.transit.serialonly.HoloTransitFactory
import com.codebutler.farebot.transit.serialonly.IstanbulKartTransitFactory
import com.codebutler.farebot.transit.serialonly.NolTransitFactory
import com.codebutler.farebot.transit.serialonly.NorticTransitFactory
import com.codebutler.farebot.transit.serialonly.PrestoTransitFactory
import com.codebutler.farebot.transit.serialonly.NextfareDesfireTransitFactory
import com.codebutler.farebot.transit.serialonly.TPFCardTransitFactory
import com.codebutler.farebot.transit.serialonly.TrimetHopTransitFactory
import com.codebutler.farebot.transit.krocap.KROCAPTransitFactory
import com.codebutler.farebot.transit.snapper.SnapperTransitFactory
import com.codebutler.farebot.transit.ndef.NdefFelicaTransitFactory
import com.codebutler.farebot.transit.ndef.NdefUltralightTransitFactory
import com.codebutler.farebot.transit.ndef.NdefVicinityTransitFactory
import com.codebutler.farebot.transit.serialonly.BlankDesfireTransitFactory
import com.codebutler.farebot.transit.serialonly.UnauthorizedDesfireTransitFactory
import com.codebutler.farebot.transit.china.ChinaTransitRegistry
import com.codebutler.farebot.shared.platform.PlatformActions
import com.codebutler.farebot.shared.settings.AppSettings
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun MainViewController() = ComposeUIViewController {
    val platformActions = IosPlatformActions()

    FareBotApp(
        platformActions = platformActions,
        supportedCards = ALL_SUPPORTED_CARDS,
        isMifareClassicSupported = false,
    )
}

fun handleImportedFileContent(content: String) {
    org.koin.mp.KoinPlatform.getKoin().get<CardImporter>().submitImport(content)
}

fun initKoin() {
    startKoin {
        modules(sharedModule, iosModule)
    }
}

private val iosModule = module {
    single<StringResource> { DefaultStringResource() }

    single {
        Json {
            serializersModule = FareBotSerializersModule
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    single<CardSerializer> { KotlinxCardSerializer(get()) }

    single {
        val driver = BundledDatabaseDriverFactory().createDriver("farebot.db", FareBotDb.Schema)
        FareBotDb(driver)
    }

    single<CardPersister> { DbCardPersister(get()) }

    single<CardKeysPersister> { DbCardKeysPersister(get()) }

    single<TransitFactoryRegistry> { createIosTransitFactoryRegistry() }

    single<CardScanner> { IosNfcScanner() }

    single { AppSettings() }

    single<PlatformActions> { IosPlatformActions() }
}

private fun createIosTransitFactoryRegistry(): TransitFactoryRegistry {
    // Register China transit factories
    ChinaTransitRegistry.registerAll()

    val registry = TransitFactoryRegistry()
    val stringResource = DefaultStringResource()

    // FeliCa factories
    registry.registerFactory(CardType.FeliCa, SuicaTransitFactory(stringResource))
    registry.registerFactory(CardType.FeliCa, EdyTransitFactory(stringResource))
    registry.registerFactory(CardType.FeliCa, OctopusTransitFactory())
    registry.registerFactory(CardType.FeliCa, KMTTransitFactory())
    registry.registerFactory(CardType.FeliCa, MRTJTransitFactory())
    registry.registerFactory(CardType.FeliCa, NdefFelicaTransitFactory())

    // DESFire factories
    registry.registerFactory(CardType.MifareDesfire, OrcaTransitFactory(stringResource))
    registry.registerFactory(CardType.MifareDesfire, ClipperTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, HSLTransitFactory(stringResource))
    registry.registerFactory(CardType.MifareDesfire, OpalTransitFactory(stringResource))
    registry.registerFactory(CardType.MifareDesfire, MykiTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, LeapTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, AdelaideTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, HafilatTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, IntercardTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, MagnaCartaTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, TampereTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, AtHopTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, HoloTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, IstanbulKartTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, NolTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, NorticTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, PrestoTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, TrimetHopTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, NextfareDesfireTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, TPFCardTransitFactory())
    // DESFire catch-all handlers (must be LAST for DESFire)
    registry.registerFactory(CardType.MifareDesfire, BlankDesfireTransitFactory())
    registry.registerFactory(CardType.MifareDesfire, UnauthorizedDesfireTransitFactory())

    // ISO7816 / Calypso factories
    registry.registerFactory(CardType.ISO7816, OpusTransitFactory(stringResource))
    registry.registerFactory(CardType.ISO7816, RavKavTransitFactory(stringResource))
    registry.registerFactory(CardType.ISO7816, MobibTransitInfo.Factory(stringResource))
    registry.registerFactory(CardType.ISO7816, VeneziaTransitFactory(stringResource))
    registry.registerFactory(CardType.ISO7816, PisaTransitFactory(stringResource))
    registry.registerFactory(CardType.ISO7816, LisboaVivaTransitInfo.Factory(stringResource))
    registry.registerFactory(CardType.ISO7816, IntercodeTransitFactory(stringResource))
    registry.registerFactory(CardType.ISO7816, TMoneyTransitFactory())
    registry.registerFactory(CardType.ISO7816, KROCAPTransitFactory())
    registry.registerFactory(CardType.ISO7816, SnapperTransitFactory())

    // EMV contactless payment cards
    registry.registerFactory(CardType.ISO7816, EmvTransitFactory)

    // CEPAS factories
    registry.registerFactory(CardType.CEPAS, EZLinkTransitFactory(stringResource))

    // Note: MIFARE Classic is not supported on iOS (no hardware support)

    // Ultralight factories (order matters - specific checks first, catch-alls last)
    registry.registerFactory(CardType.MifareUltralight, TroikaUltralightTransitFactory())
    registry.registerFactory(CardType.MifareUltralight, ClipperUltralightTransitFactory())
    registry.registerFactory(CardType.MifareUltralight, OVChipUltralightTransitFactory())
    registry.registerFactory(CardType.MifareUltralight, MRTUltralightTransitFactory())
    registry.registerFactory(CardType.MifareUltralight, VeneziaUltralightTransitFactory())
    registry.registerFactory(CardType.MifareUltralight, PisaUltralightTransitFactory())
    registry.registerFactory(CardType.MifareUltralight, AmiiboTransitFactory())
    registry.registerFactory(CardType.MifareUltralight, HSLUltralightTransitFactory())
    registry.registerFactory(CardType.MifareUltralight, VentraUltralightTransitInfo.FACTORY)
    registry.registerFactory(CardType.MifareUltralight, CompassUltralightTransitInfo.FACTORY)
    registry.registerFactory(CardType.MifareUltralight, NextfareUnknownUltralightTransitInfo.FACTORY)
    registry.registerFactory(CardType.MifareUltralight, NdefUltralightTransitFactory())
    registry.registerFactory(CardType.MifareUltralight, BlankUltralightTransitFactory())
    registry.registerFactory(CardType.MifareUltralight, LockedUltralightTransitFactory())

    // Vicinity / NFC-V factories
    registry.registerFactory(CardType.Vicinity, NdefVicinityTransitFactory())
    registry.registerFactory(CardType.Vicinity, BlankVicinityTransitFactory())
    registry.registerFactory(CardType.Vicinity, UnknownVicinityTransitFactory())

    registry.registerFactory(CardType.Sample, SampleTransitFactory())

    return registry
}
