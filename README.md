# FareBot

Read your remaining balance, recent trips, and other information from contactless public transit cards using your NFC-enabled Android or iOS device.

FareBot is a [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) app built with [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/), targeting Android (NFC), iOS (CoreNFC), and macOS (experimental, via PC/SC smart card readers or PN533 raw USB NFC controllers).

## Platform Compatibility

| Protocol | Android | iOS |
|----------|---------|-----|
| [CEPAS](https://en.wikipedia.org/wiki/CEPAS) | Yes | Yes |
| [FeliCa](https://en.wikipedia.org/wiki/FeliCa) | Yes | Yes |
| [ISO 7816](https://en.wikipedia.org/wiki/ISO/IEC_7816) | Yes | Yes |
| [MIFARE Classic](https://en.wikipedia.org/wiki/MIFARE#MIFARE_Classic) | NXP NFC chips only | No |
| [MIFARE DESFire](https://en.wikipedia.org/wiki/MIFARE#MIFARE_DESFire) | Yes | Yes |
| [MIFARE Ultralight](https://en.wikipedia.org/wiki/MIFARE#MIFARE_Ultralight_and_MIFARE_Ultralight_EV1) | Yes | Yes |
| [NFC-V / Vicinity](https://en.wikipedia.org/wiki/Near-field_communication#Standards) | Yes | Yes |

MIFARE Classic requires proprietary NXP hardware and is not supported on iOS or on Android devices with non-NXP NFC controllers (e.g. most Samsung and some other devices). All other protocols work on both platforms. Cards marked **Android only** in the tables below use MIFARE Classic.

## Supported Cards

### Asia

| Card | Location | Protocol | Platform |
|------|----------|----------|----------|
| [Beijing Municipal Card](https://en.wikipedia.org/wiki/Yikatong) | Beijing, China | ISO 7816 | Android, iOS |
| [City Union](https://en.wikipedia.org/wiki/China_T-Union) | China | ISO 7816 | Android, iOS |
| [Edy](https://en.wikipedia.org/wiki/Edy) | Japan | FeliCa | Android, iOS |
| [EZ-Link](http://www.ezlink.com.sg/) | Singapore | CEPAS | Android, iOS |
| [Kartu Multi Trip](https://en.wikipedia.org/wiki/Kereta_Commuter_Indonesia) | Jakarta, Indonesia | FeliCa | Android, iOS |
| [KomuterLink](https://en.wikipedia.org/wiki/KTM_Komuter) | Malaysia | Classic | Android only |
| [NETS FlashPay](https://www.nets.com.sg/) | Singapore | CEPAS | Android, iOS |
| [Octopus](https://www.octopus.com.hk/) | Hong Kong | FeliCa | Android, iOS |
| [One Card All Pass](https://en.wikipedia.org/wiki/One_Card_All_Pass) | South Korea | ISO 7816 | Android, iOS |
| [Shanghai Public Transportation Card](https://en.wikipedia.org/wiki/Shanghai_Public_Transportation_Card) | Shanghai, China | ISO 7816 | Android, iOS |
| [Shenzhen Tong](https://en.wikipedia.org/wiki/Shenzhen_Tong) | Shenzhen, China | ISO 7816 | Android, iOS |
| [Suica](https://en.wikipedia.org/wiki/Suica) / ICOCA / PASMO | Japan | FeliCa | Android, iOS |
| [T-money](https://en.wikipedia.org/wiki/T-money) | South Korea | ISO 7816 | Android, iOS |
| [T-Union](https://en.wikipedia.org/wiki/China_T-Union) | China | ISO 7816 | Android, iOS |
| [Touch 'n Go](https://www.touchngo.com.my/) | Malaysia | Classic | Android only |
| [Wuhan Tong](https://en.wikipedia.org/wiki/Wuhan_Metro) | Wuhan, China | ISO 7816 | Android, iOS |

### Australia & New Zealand

| Card | Location | Protocol | Platform |
|------|----------|----------|----------|
| [Adelaide Metrocard](https://www.adelaidemetro.com.au/) | Adelaide, SA | DESFire | Android, iOS |
| [BUSIT](https://www.busit.co.nz/) | Waikato, NZ | Classic | Android only |
| [Manly Fast Ferry](http://www.manlyfastferry.com.au/) | Sydney, NSW | Classic | Android only |
| [Metrocard](https://www.metroinfo.co.nz/) | Christchurch, NZ | Classic | Android only |
| [Myki](https://www.ptv.vic.gov.au/tickets/myki/) | Melbourne, VIC | DESFire | Android, iOS |
| [Opal](https://www.opal.com.au/) | Sydney, NSW | DESFire | Android, iOS |
| [Otago GoCard](https://www.orc.govt.nz/) | Otago, NZ | Classic | Android only |
| [SeqGo](https://translink.com.au/) | Queensland | Classic | Android only |
| [SmartRide](https://www.busit.co.nz/) | Rotorua, NZ | Classic | Android only |
| [SmartRider](https://www.transperth.wa.gov.au/) | Perth, WA | Classic | Android only |
| [Snapper](https://www.snapper.co.nz/) | Wellington, NZ | ISO 7816 | Android, iOS |

### Europe

| Card | Location | Protocol | Platform |
|------|----------|----------|----------|
| [Bonobus](https://www.bonobus.es/) | Cadiz, Spain | Classic | Android only |
| [Carta Mobile](https://www.at-bus.it/) | Pisa, Italy | ISO 7816 (Calypso) | Android, iOS |
| [Envibus](https://www.envibus.fr/) | Sophia Antipolis, France | ISO 7816 (Calypso) | Android, iOS |
| [HSL](https://www.hsl.fi/) | Helsinki, Finland | DESFire | Android, iOS |
| [KorriGo](https://www.star.fr/) | Brittany, France | ISO 7816 (Calypso) | Android, iOS |
| [Leap](https://www.leapcard.ie/) | Dublin, Ireland | DESFire | Android, iOS |
| [Lisboa Viva](https://www.portalviva.pt/) | Lisbon, Portugal | ISO 7816 (Calypso) | Android, iOS |
| [Mobib](https://mobib.be/) | Brussels, Belgium | ISO 7816 (Calypso) | Android, iOS |
| [Navigo](https://www.iledefrance-mobilites.fr/) | Paris, France | ISO 7816 (Calypso) | Android, iOS |
| [OuRA](https://www.oura.com/) | Grenoble, France | ISO 7816 (Calypso) | Android, iOS |
| [OV-chipkaart](https://www.ov-chipkaart.nl/) | Netherlands | Classic / Ultralight | Android only (Classic), Android + iOS (Ultralight) |
| [Oyster](https://oyster.tfl.gov.uk/) | London, UK | Classic | Android only |
| [Pass Pass](https://www.passpass.fr/) | Hauts-de-France, France | ISO 7816 (Calypso) | Android, iOS |
| [Pastel](https://www.tisseo.fr/) | Toulouse, France | ISO 7816 (Calypso) | Android, iOS |
| [Rejsekort](https://www.rejsekort.dk/) | Denmark | Classic | Android only |
| [RicaricaMi](https://www.atm.it/) | Milan, Italy | Classic | Android only |
| [SLaccess](https://sl.se/) | Stockholm, Sweden | Classic | Android only |
| [TaM](https://www.tam-voyages.com/) | Montpellier, France | ISO 7816 (Calypso) | Android, iOS |
| [Tampere](https://www.nysse.fi/) | Tampere, Finland | DESFire | Android, iOS |
| [Tartu Bus](https://www.tartu.ee/) | Tartu, Estonia | Classic | Android only |
| [TransGironde](https://transgironde.fr/) | Gironde, France | ISO 7816 (Calypso) | Android, iOS |
| [Västtrafik](https://www.vasttrafik.se/) | Gothenburg, Sweden | Classic | Android only |
| [Venezia Unica](https://actv.avmspa.it/) | Venice, Italy | ISO 7816 (Calypso) | Android, iOS |
| [Waltti](https://waltti.fi/) | Finland | DESFire | Android, iOS |
| [Warsaw](https://www.ztm.waw.pl/) | Warsaw, Poland | Classic | Android only |

### Middle East & Africa

| Card | Location | Protocol | Platform |
|------|----------|----------|----------|
| [Gautrain](https://www.gautrain.co.za/) | Gauteng, South Africa | Classic | Android only |
| [Hafilat](https://www.dot.abudhabi/) | Abu Dhabi, UAE | DESFire | Android, iOS |
| [Metro Q](https://www.qr.com.qa/) | Qatar | Classic | Android only |
| [RavKav](https://ravkav.co.il/) | Israel | ISO 7816 (Calypso) | Android, iOS |

### North America

| Card | Location | Protocol | Platform |
|------|----------|----------|----------|
| [Charlie Card](https://www.mbta.com/fares/charliecard) | Boston, MA | Classic | Android only |
| [Clipper](https://www.clippercard.com/) | San Francisco, CA | DESFire / Ultralight | Android, iOS |
| [Compass](https://www.compasscard.ca/) | Vancouver, Canada | Ultralight | Android, iOS |
| [LAX TAP](https://www.taptogo.net/) | Los Angeles, CA | Classic | Android only |
| [MSP GoTo](https://www.metrotransit.org/) | Minneapolis, MN | Classic | Android only |
| [Opus](https://www.stm.info/) | Montreal, Canada | ISO 7816 (Calypso) | Android, iOS |
| [ORCA](https://www.orcacard.com/) | Seattle, WA | DESFire | Android, iOS |
| [Ventra](https://www.ventrachicago.com/) | Chicago, IL | Ultralight | Android, iOS |

### Russia & Former Soviet Union

| Card | Location | Protocol | Platform |
|------|----------|----------|----------|
| [Crimea Trolleybus Card](https://www.korona.net/) | Crimea | Classic | Android only |
| [Ekarta](https://www.korona.net/) | Yekaterinburg, Russia | Classic | Android only |
| [Electronic Barnaul](https://umarsh.com/) | Barnaul, Russia | Classic | Android only |
| [Kazan](https://en.wikipedia.org/wiki/Kazan_Metro) | Kazan, Russia | Classic | Android only |
| [Kirov transport card](https://umarsh.com/) | Kirov, Russia | Classic | Android only |
| [Krasnodar ETK](https://www.korona.net/) | Krasnodar, Russia | Classic | Android only |
| [Kyiv Digital](https://www.eway.in.ua/) | Kyiv, Ukraine | Classic | Android only |
| [Kyiv Metro](https://www.eway.in.ua/) | Kyiv, Ukraine | Classic | Android only |
| [MetroMoney](https://www.tbilisi.gov.ge/) | Tbilisi, Georgia | Classic | Android only |
| [OMKA](https://umarsh.com/) | Omsk, Russia | Classic | Android only |
| [Orenburg EKG](https://www.korona.net/) | Orenburg, Russia | Classic | Android only |
| [Parus school card](https://www.korona.net/) | Crimea | Classic | Android only |
| [Penza transport card](https://umarsh.com/) | Penza, Russia | Classic | Android only |
| [Podorozhnik](https://podorozhnik.spb.ru/) | St. Petersburg, Russia | Classic | Android only |
| [Samara ETK](https://www.korona.net/) | Samara, Russia | Classic | Android only |
| [SitiCard](https://umarsh.com/) | Nizhniy Novgorod, Russia | Classic | Android only |
| [SitiCard (Vladimir)](https://umarsh.com/) | Vladimir, Russia | Classic | Android only |
| [Strizh](https://umarsh.com/) | Izhevsk, Russia | Classic | Android only |
| [Troika](https://troika.mos.ru/) | Moscow, Russia | Classic / Ultralight | Android only (Classic), Android + iOS (Ultralight) |
| [YarGor](https://yargor.ru/) | Yaroslavl, Russia | Classic | Android only |
| [Yaroslavl ETK](https://www.korona.net/) | Yaroslavl, Russia | Classic | Android only |
| [Yoshkar-Ola transport card](https://umarsh.com/) | Yoshkar-Ola, Russia | Classic | Android only |
| [Zolotaya Korona](https://www.korona.net/) | Russia | Classic | Android only |

### South America

| Card | Location | Protocol | Platform |
|------|----------|----------|----------|
| [Bilhete Único](http://www.sptrans.com.br/bilhete_unico/) | São Paulo, Brazil | Classic | Android only |
| [Bip!](https://www.red.cl/tarjeta-bip) | Santiago, Chile | Classic | Android only |

### Taiwan

| Card | Location | Protocol | Platform |
|------|----------|----------|----------|
| [EasyCard](https://www.easycard.com.tw/) | Taipei | Classic / DESFire | Android only (Classic), Android + iOS (DESFire) |

### Identification Only (Serial Number)

These cards can be detected and identified, but their data is locked or not stored on-card:

| Card | Location | Protocol | Platform | Reason |
|------|----------|----------|----------|--------|
| [AT HOP](https://at.govt.nz/bus-train-ferry/at-hop-card/) | Auckland, NZ | DESFire | Android, iOS | Locked |
| [Holo](https://www.holocard.net/) | Oahu, HI | DESFire | Android, iOS | Not stored on card |
| [Istanbul Kart](https://www.istanbulkart.istanbul/) | Istanbul, Turkey | DESFire | Android, iOS | Locked |
| [Nextfare DESFire](https://en.wikipedia.org/wiki/Cubic_Transportation_Systems) | Various | DESFire | Android, iOS | Locked |
| [Nol](https://www.nol.ae/) | Dubai, UAE | DESFire | Android, iOS | Locked |
| [Nortic](https://rfrend.no/) | Scandinavia | DESFire | Android, iOS | Locked |
| [Presto](https://www.prestocard.ca/) | Ontario, Canada | DESFire | Android, iOS | Locked |
| [Strelka](https://strelkacard.ru/) | Moscow Region, Russia | Classic | Android only | Locked |
| [Sun Card](https://sunrail.com/) | Orlando, FL | Classic | Android only | Locked |
| [TPF](https://www.tpf.ch/) | Fribourg, Switzerland | DESFire | Android, iOS | Locked |
| [TriMet Hop](https://myhopcard.com/) | Portland, OR | DESFire | Android, iOS | Not stored on card |

## Cards Requiring Keys

Some MIFARE Classic cards require encryption keys to read. You can obtain keys using a [Proxmark3](https://github.com/Proxmark/proxmark3/wiki/Mifare-HowTo) or [MFOC](https://github.com/nfc-tools/mfoc). These include:

* Bilhete Único
* Charlie Card
* EasyCard (older MIFARE Classic variant)
* OV-chipkaart
* Oyster
* And most other MIFARE Classic-based cards

## Requirements

* **Android:** NFC-enabled device running Android 6.0 (API 23) or later
* **iOS:** iPhone 7 or later with iOS support for CoreNFC
* **macOS** (experimental): Mac with a PC/SC-compatible NFC smart card reader (e.g., ACR122U), a PN533-based USB NFC controller (e.g., SCL3711), or a Sony RC-S956 (PaSoRi) USB NFC reader

## Building

```
$ git clone https://github.com/codebutler/farebot.git
$ cd farebot
$ make              # show all targets
```

| Command | Description |
|---------|-------------|
| `make android` | Build Android debug APK |
| `make android-install` | Build and install on connected Android device (via adb) |
| `make ios` | Build iOS app for physical device |
| `make ios-sim` | Build iOS app for simulator |
| `make ios-install` | Build and install on connected iOS device (auto-detects device) |
| `make desktop` | Run macOS desktop app (experimental) |
| `make test` | Run all tests |
| `make clean` | Clean all build artifacts |

## Tech Stack

* [Kotlin](https://kotlinlang.org/) 2.3.0 (Multiplatform)
* [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) (shared UI)
* [Koin](https://insert-koin.io/) (dependency injection)
* [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) (serialization)
* [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime) (date/time)
* [SQLDelight](https://github.com/cashapp/sqldelight) (database)

## Project Structure

- `base/` — Core utilities, MDST reader, ByteArray extensions
- `card/` — Shared card abstractions
- `card/*/` — Card protocol implementations (classic, desfire, felica, etc.)
- `transit/` — Shared transit abstractions (Trip, Station, TransitInfo, etc.)
- `transit/*/` — Transit system implementations (one per system)
- `app/` — KMP app framework (UI, ViewModels, DI, platform code)
- `app/android/` — Android app shell (Activities, manifest, resources)
- `app/ios/` — iOS app shell (Swift entry point, assets, config)
- `app/desktop/` — macOS desktop app (experimental, PC/SC + PN533 + RC-S956 USB NFC)

## Written By

* [Eric Butler](https://x.com/codebutler) <eric@codebutler.com>

## Thanks To

> [!NOTE]
> Huge thanks to [the Metrodroid project](https://github.com/metrodroid/metrodroid), a fork of FareBot that added support for many additional transit systems. All features as of [v3.1.0 (`04a603ba`)](https://github.com/metrodroid/metrodroid/commit/04a603ba639f) have been backported.

* [Karl Koscher](https://x.com/supersat) (ORCA)
* [Sean Cross](https://x.com/xobs) (CEPAS/EZ-Link)
* Anonymous Contributor (Clipper)
* [nfc-felica](http://code.google.com/p/nfc-felica/) and [IC SFCard Fan](http://www014.upp.so-net.ne.jp/SFCardFan/) projects (Suica)
* [Wilbert Duijvenvoorde](https://github.com/wandcode) (MIFARE Classic/OV-chipkaart)
* [tbonang](https://github.com/tbonang) (NETS FlashPay)
* [Marcelo Liberato](https://github.com/mliberato) (Bilhete Unico)
* [Lauri Andler](https://github.com/landler/) (HSL)
* [Michael Farrell](https://github.com/micolous/) (Opal, Manly Fast Ferry, Go card, Myki, Octopus)
* [Rob O'Regan](http://www.robx1.net/nswtkt/private/manlyff/manlyff.htm) (Manly Fast Ferry card image)
* [b33f](http://www.fuzzysecurity.com/tutorials/rfid/4.html) (EasyCard)
* [Bondan Sumbodo](http://sybond.web.id) (Kartu Multi Trip, COMMET)

## License

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
