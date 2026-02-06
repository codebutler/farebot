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
import com.codebutler.farebot.shared.sample.SampleTransitFactory
import com.codebutler.farebot.app.core.util.AndroidStringResource
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.shared.transit.TransitFactoryRegistry
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
import com.codebutler.farebot.transit.suica.SuicaTransitFactory
import com.codebutler.farebot.transit.kmt.KMTTransitFactory
import com.codebutler.farebot.transit.mrtj.MRTJTransitFactory
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
import com.codebutler.farebot.transit.msp_goto.MspGotoTransitFactory
import com.codebutler.farebot.transit.waikato.WaikatoCardTransitFactory
import com.codebutler.farebot.transit.troika.TroikaTransitFactory
import com.codebutler.farebot.transit.oyster.OysterTransitFactory
import com.codebutler.farebot.transit.charlie.CharlieCardTransitFactory
import com.codebutler.farebot.transit.gautrain.GautrainTransitFactory
import com.codebutler.farebot.transit.smartrider.SmartRiderTransitFactory
import com.codebutler.farebot.transit.podorozhnik.PodorozhnikTransitFactory
import com.codebutler.farebot.transit.touchngo.TouchnGoTransitFactory
import com.codebutler.farebot.transit.tfi_leap.LeapTransitFactory
import com.codebutler.farebot.transit.lax_tap.LaxTapTransitFactory
import com.codebutler.farebot.transit.ricaricami.RicaricaMiTransitFactory
import com.codebutler.farebot.transit.yargor.YarGorTransitFactory
import com.codebutler.farebot.transit.chc_metrocard.ChcMetrocardTransitFactory
import com.codebutler.farebot.transit.erg.ErgTransitInfo
import com.codebutler.farebot.transit.nextfare.NextfareTransitInfo
import com.codebutler.farebot.transit.bip.BipTransitFactory
import com.codebutler.farebot.transit.bonobus.BonobusTransitFactory
import com.codebutler.farebot.transit.cifial.CifialTransitFactory
import com.codebutler.farebot.transit.kazan.KazanTransitFactory
import com.codebutler.farebot.transit.kiev.KievTransitFactory
import com.codebutler.farebot.transit.komuterlink.KomuterLinkTransitFactory
import com.codebutler.farebot.transit.metromoney.MetroMoneyTransitFactory
import com.codebutler.farebot.transit.metroq.MetroQTransitFactory
import com.codebutler.farebot.transit.otago.OtagoGoCardTransitFactory
import com.codebutler.farebot.transit.pilet.KievDigitalTransitFactory
import com.codebutler.farebot.transit.pilet.TartuTransitFactory
import com.codebutler.farebot.transit.selecta.SelectaFranceTransitFactory
import com.codebutler.farebot.transit.umarsh.UmarshTransitFactory
import com.codebutler.farebot.transit.warsaw.WarsawTransitFactory
import com.codebutler.farebot.transit.zolotayakorona.ZolotayaKoronaTransitFactory
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
import com.codebutler.farebot.transit.serialonly.TrimetHopTransitFactory
import com.codebutler.farebot.transit.serialonly.NextfareDesfireTransitFactory
import com.codebutler.farebot.transit.serialonly.StrelkaTransitFactory
import com.codebutler.farebot.transit.serialonly.SunCardTransitFactory
import com.codebutler.farebot.transit.serialonly.TPFCardTransitFactory
import com.codebutler.farebot.transit.krocap.KROCAPTransitFactory
import com.codebutler.farebot.transit.snapper.SnapperTransitFactory
import com.codebutler.farebot.transit.ndef.NdefClassicTransitFactory
import com.codebutler.farebot.transit.ndef.NdefFelicaTransitFactory
import com.codebutler.farebot.transit.ndef.NdefUltralightTransitFactory
import com.codebutler.farebot.transit.ndef.NdefVicinityTransitFactory
import com.codebutler.farebot.transit.rkf.RkfTransitFactory
import com.codebutler.farebot.transit.serialonly.BlankClassicTransitFactory
import com.codebutler.farebot.transit.serialonly.BlankDesfireTransitFactory
import com.codebutler.farebot.transit.serialonly.UnauthorizedClassicTransitFactory
import com.codebutler.farebot.transit.serialonly.UnauthorizedDesfireTransitFactory
import com.codebutler.farebot.transit.china.ChinaTransitRegistry

fun createAndroidTransitFactoryRegistry(context: Context): TransitFactoryRegistry {
    // Register China transit factories
    ChinaTransitRegistry.registerAll()
    val registry = TransitFactoryRegistry()
    val stringResource = AndroidStringResource()

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

    // Classic factories
    registry.registerFactory(CardType.MifareClassic, OVChipTransitFactory(stringResource))
    registry.registerFactory(CardType.MifareClassic, BilheteUnicoSPTransitFactory())
    registry.registerFactory(CardType.MifareClassic, ManlyFastFerryTransitFactory())
    registry.registerFactory(CardType.MifareClassic, SeqGoTransitFactory())
    registry.registerFactory(CardType.MifareClassic, EasyCardTransitFactory(stringResource))
    registry.registerFactory(CardType.MifareClassic, TroikaTransitFactory())
    registry.registerFactory(CardType.MifareClassic, OysterTransitFactory())
    registry.registerFactory(CardType.MifareClassic, CharlieCardTransitFactory())
    registry.registerFactory(CardType.MifareClassic, GautrainTransitFactory())
    registry.registerFactory(CardType.MifareClassic, SmartRiderTransitFactory(stringResource))
    registry.registerFactory(CardType.MifareClassic, NextfareTransitInfo.NextfareTransitFactory())
    registry.registerFactory(CardType.MifareClassic, PodorozhnikTransitFactory(stringResource))
    registry.registerFactory(CardType.MifareClassic, TouchnGoTransitFactory())
    registry.registerFactory(CardType.MifareClassic, LaxTapTransitFactory())
    registry.registerFactory(CardType.MifareClassic, RicaricaMiTransitFactory())
    registry.registerFactory(CardType.MifareClassic, YarGorTransitFactory())
    registry.registerFactory(CardType.MifareClassic, ChcMetrocardTransitFactory())
    registry.registerFactory(CardType.MifareClassic, ErgTransitInfo.ErgTransitFactory())
    registry.registerFactory(CardType.MifareClassic, KomuterLinkTransitFactory())
    registry.registerFactory(CardType.MifareClassic, BonobusTransitFactory())
    registry.registerFactory(CardType.MifareClassic, CifialTransitFactory())
    registry.registerFactory(CardType.MifareClassic, KazanTransitFactory())
    registry.registerFactory(CardType.MifareClassic, KievTransitFactory())
    registry.registerFactory(CardType.MifareClassic, KievDigitalTransitFactory())
    registry.registerFactory(CardType.MifareClassic, TartuTransitFactory())
    registry.registerFactory(CardType.MifareClassic, MetroMoneyTransitFactory())
    registry.registerFactory(CardType.MifareClassic, MetroQTransitFactory())
    registry.registerFactory(CardType.MifareClassic, OtagoGoCardTransitFactory())
    registry.registerFactory(CardType.MifareClassic, SelectaFranceTransitFactory())
    registry.registerFactory(CardType.MifareClassic, UmarshTransitFactory())
    registry.registerFactory(CardType.MifareClassic, WarsawTransitFactory())
    registry.registerFactory(CardType.MifareClassic, ZolotayaKoronaTransitFactory())
    registry.registerFactory(CardType.MifareClassic, BipTransitFactory())
    registry.registerFactory(CardType.MifareClassic, MspGotoTransitFactory())
    registry.registerFactory(CardType.MifareClassic, WaikatoCardTransitFactory())
    registry.registerFactory(CardType.MifareClassic, StrelkaTransitFactory())
    registry.registerFactory(CardType.MifareClassic, SunCardTransitFactory())
    registry.registerFactory(CardType.MifareClassic, RkfTransitFactory())
    registry.registerFactory(CardType.MifareClassic, NdefClassicTransitFactory())
    // Classic catch-all handlers (must be LAST for Classic)
    registry.registerFactory(CardType.MifareClassic, BlankClassicTransitFactory())
    registry.registerFactory(CardType.MifareClassic, UnauthorizedClassicTransitFactory())

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
