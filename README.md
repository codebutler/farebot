<p align="center">
  <img src="https://codebutler.github.io/farebot/img/farebot_icon_huge.png" width="128" alt="FareBot">
</p>

<h1 align="center">FareBot</h1>

<p align="center">
  Read your remaining balance, recent trips, and other information from contactless public transit cards using your NFC-enabled device.
</p>

FareBot runs on:

- **Android** — built-in NFC (6.0+)
- **iOS** — built-in NFC (iPhone 7+)
- **macOS** (experimental) — PC/SC smart card readers or PN533 USB NFC readers
- **Web** (experimental) — PN533 USB NFC readers (Chrome/Edge/Opera)

## Download

<!-- TODO: Add links when published -->
- **Android:** Coming soon on Google Play
- **iOS:** Coming soon on the App Store
- **Web:** [farebot-web.vercel.app](https://farebot-web.vercel.app/)
- **Build from source:** See [Building](#building)

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

## Supported Cards

### Asia

| Card | Location | Protocol | Android | iOS | macOS | Web |
|------|----------|----------|---------|-----|-------|-----|
| [Beijing Municipal Card](https://en.wikipedia.org/wiki/Yikatong) | Beijing, China | ISO 7816 | ✅ | ✅ | ✅ | ✅ |
| [City Union](https://en.wikipedia.org/wiki/China_T-Union) | China | ISO 7816 | ✅ | ✅ | ✅ | ✅ |
| [Edy](https://en.wikipedia.org/wiki/Edy) | Japan | FeliCa | ✅ | ✅ | ✅ | ✅ |
| [EZ-Link](http://www.ezlink.com.sg/) | Singapore | CEPAS | ✅ | ✅ | ✅ | ✅ |
| [Kartu Multi Trip](https://en.wikipedia.org/wiki/Kereta_Commuter_Indonesia) | Jakarta, Indonesia | FeliCa | ✅ | ✅ | ✅ | ✅ |
| [KomuterLink](https://en.wikipedia.org/wiki/KTM_Komuter) | Malaysia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [NETS FlashPay](https://www.nets.com.sg/) | Singapore | CEPAS | ✅ | ✅ | ✅ | ✅ |
| [Octopus](https://www.octopus.com.hk/) | Hong Kong | FeliCa | ✅ | ✅ | ✅ | ✅ |
| [One Card All Pass](https://en.wikipedia.org/wiki/One_Card_All_Pass) | South Korea | ISO 7816 | ✅ | ✅ | ✅ | ✅ |
| [Shanghai Public Transportation Card](https://en.wikipedia.org/wiki/Shanghai_Public_Transportation_Card) | Shanghai, China | ISO 7816 | ✅ | ✅ | ✅ | ✅ |
| [Shenzhen Tong](https://en.wikipedia.org/wiki/Shenzhen_Tong) | Shenzhen, China | ISO 7816 | ✅ | ✅ | ✅ | ✅ |
| [Suica](https://en.wikipedia.org/wiki/Suica) / ICOCA / PASMO | Japan | FeliCa | ✅ | ✅ | ✅ | ✅ |
| [T-money](https://en.wikipedia.org/wiki/T-money) | South Korea | ISO 7816 | ✅ | ✅ | ✅ | ✅ |
| [T-Union](https://en.wikipedia.org/wiki/China_T-Union) | China | ISO 7816 | ✅ | ✅ | ✅ | ✅ |
| [Touch 'n Go](https://www.touchngo.com.my/) | Malaysia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Wuhan Tong](https://en.wikipedia.org/wiki/Wuhan_Metro) | Wuhan, China | ISO 7816 | ✅ | ✅ | ✅ | ✅ |

### Australia & New Zealand

| Card | Location | Protocol | Android | iOS | macOS | Web |
|------|----------|----------|---------|-----|-------|-----|
| [Adelaide Metrocard](https://www.adelaidemetro.com.au/) | Adelaide, SA | DESFire | ✅ | ✅ | ✅ | ✅ |
| [BUSIT](https://www.busit.co.nz/) | Waikato, NZ | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Manly Fast Ferry](http://www.manlyfastferry.com.au/) | Sydney, NSW | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Metrocard](https://www.metroinfo.co.nz/) | Christchurch, NZ | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Myki](https://www.ptv.vic.gov.au/tickets/myki/) | Melbourne, VIC | DESFire | ✅ | ✅ | ✅ | ✅ |
| [Opal](https://www.opal.com.au/) | Sydney, NSW | DESFire | ✅ | ✅ | ✅ | ✅ |
| [Otago GoCard](https://www.orc.govt.nz/) | Otago, NZ | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [SeqGo](https://translink.com.au/) | Queensland | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [SmartRide](https://www.busit.co.nz/) | Rotorua, NZ | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [SmartRider](https://www.transperth.wa.gov.au/) | Perth, WA | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Snapper](https://www.snapper.co.nz/) | Wellington, NZ | ISO 7816 | ✅ | ✅ | ✅ | ✅ |

### Europe

| Card | Location | Protocol | Android | iOS | macOS | Web |
|------|----------|----------|---------|-----|-------|-----|
| [Bonobus](https://www.bonobus.es/) | Cadiz, Spain | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Carta Mobile](https://www.at-bus.it/) | Pisa, Italy | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [Envibus](https://www.envibus.fr/) | Sophia Antipolis, France | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [HSL](https://www.hsl.fi/) | Helsinki, Finland | DESFire | ✅ | ✅ | ✅ | ✅ |
| [KorriGo](https://www.star.fr/) | Brittany, France | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [Leap](https://www.leapcard.ie/) | Dublin, Ireland | DESFire | ✅ | ✅ | ✅ | ✅ |
| [Lisboa Viva](https://www.portalviva.pt/) | Lisbon, Portugal | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [Mobib](https://mobib.be/) | Brussels, Belgium | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [Navigo](https://www.iledefrance-mobilites.fr/) | Paris, France | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [OuRA](https://www.oura.com/) | Grenoble, France | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [OV-chipkaart](https://www.ov-chipkaart.nl/) | Netherlands | Classic 🔒 / Ultralight | ✅ | ✅³ | ✅ | ✅ |
| [Oyster](https://oyster.tfl.gov.uk/) | London, UK | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Pass Pass](https://www.passpass.fr/) | Hauts-de-France, France | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [Pastel](https://www.tisseo.fr/) | Toulouse, France | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [Rejsekort](https://www.rejsekort.dk/) | Denmark | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [RicaricaMi](https://www.atm.it/) | Milan, Italy | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [SLaccess](https://sl.se/) | Stockholm, Sweden | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [TaM](https://www.tam-voyages.com/) | Montpellier, France | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [Tampere](https://www.nysse.fi/) | Tampere, Finland | DESFire | ✅ | ✅ | ✅ | ✅ |
| [Tartu Bus](https://www.tartu.ee/) | Tartu, Estonia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [TransGironde](https://transgironde.fr/) | Gironde, France | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [Västtrafik](https://www.vasttrafik.se/) | Gothenburg, Sweden | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Venezia Unica](https://actv.avmspa.it/) | Venice, Italy | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [Waltti](https://waltti.fi/) | Finland | DESFire | ✅ | ✅ | ✅ | ✅ |
| [Warsaw](https://www.ztm.waw.pl/) | Warsaw, Poland | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |

### Middle East & Africa

| Card | Location | Protocol | Android | iOS | macOS | Web |
|------|----------|----------|---------|-----|-------|-----|
| [Gautrain](https://www.gautrain.co.za/) | Gauteng, South Africa | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Hafilat](https://www.dot.abudhabi/) | Abu Dhabi, UAE | DESFire | ✅ | ✅ | ✅ | ✅ |
| [Metro Q](https://www.qr.com.qa/) | Qatar | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [RavKav](https://ravkav.co.il/) | Israel | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |

### North America

| Card | Location | Protocol | Android | iOS | macOS | Web |
|------|----------|----------|---------|-----|-------|-----|
| [Charlie Card](https://www.mbta.com/fares/charliecard) | Boston, MA | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Clipper](https://www.clippercard.com/) | San Francisco, CA | DESFire / Ultralight | ✅ | ✅ | ✅ | ✅ |
| [Compass](https://www.compasscard.ca/) | Vancouver, Canada | Ultralight | ✅ | ✅ | ✅ | ✅ |
| [LAX TAP](https://www.taptogo.net/) | Los Angeles, CA | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [MSP GoTo](https://www.metrotransit.org/) | Minneapolis, MN | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Opus](https://www.stm.info/) | Montreal, Canada | ISO 7816 (Calypso) | ✅ | ✅ | ✅ | ✅ |
| [ORCA](https://www.orcacard.com/) | Seattle, WA | DESFire | ✅ | ✅ | ✅ | ✅ |
| [Ventra](https://www.ventrachicago.com/) | Chicago, IL | Ultralight | ✅ | ✅ | ✅ | ✅ |

### Russia & Former Soviet Union

| Card | Location | Protocol | Android | iOS | macOS | Web |
|------|----------|----------|---------|-----|-------|-----|
| [Crimea Trolleybus Card](https://www.korona.net/) | Crimea | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Ekarta](https://www.korona.net/) | Yekaterinburg, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Electronic Barnaul](https://umarsh.com/) | Barnaul, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Kazan](https://en.wikipedia.org/wiki/Kazan_Metro) | Kazan, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Kirov transport card](https://umarsh.com/) | Kirov, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Krasnodar ETK](https://www.korona.net/) | Krasnodar, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Kyiv Digital](https://www.eway.in.ua/) | Kyiv, Ukraine | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Kyiv Metro](https://www.eway.in.ua/) | Kyiv, Ukraine | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [MetroMoney](https://www.tbilisi.gov.ge/) | Tbilisi, Georgia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [OMKA](https://umarsh.com/) | Omsk, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Orenburg EKG](https://www.korona.net/) | Orenburg, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Parus school card](https://www.korona.net/) | Crimea | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Penza transport card](https://umarsh.com/) | Penza, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Podorozhnik](https://podorozhnik.spb.ru/) | St. Petersburg, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Samara ETK](https://www.korona.net/) | Samara, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [SitiCard](https://umarsh.com/) | Nizhniy Novgorod, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [SitiCard (Vladimir)](https://umarsh.com/) | Vladimir, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Strizh](https://umarsh.com/) | Izhevsk, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Troika](https://troika.mos.ru/) | Moscow, Russia | Classic 🔒 / Ultralight | ✅ | ✅³ | ✅ | ✅ |
| [YarGor](https://yargor.ru/) | Yaroslavl, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Yaroslavl ETK](https://www.korona.net/) | Yaroslavl, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Yoshkar-Ola transport card](https://umarsh.com/) | Yoshkar-Ola, Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Zolotaya Korona](https://www.korona.net/) | Russia | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |

### South America

| Card | Location | Protocol | Android | iOS | macOS | Web |
|------|----------|----------|---------|-----|-------|-----|
| [Bilhete Único](http://www.sptrans.com.br/bilhete_unico/) | São Paulo, Brazil | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |
| [Bip!](https://www.red.cl/tarjeta-bip) | Santiago, Chile | Classic 🔒 | ✅¹ | ❌ | ✅ | ✅ |

### Taiwan

| Card | Location | Protocol | Android | iOS | macOS | Web |
|------|----------|----------|---------|-----|-------|-----|
| [EasyCard](https://www.easycard.com.tw/) | Taipei | Classic 🔒 / DESFire | ✅ | ✅⁴ | ✅ | ✅ |

### Identification Only (Serial Number)

These cards can be detected and identified, but their data is locked or not stored on-card:

| Card | Location | Protocol | Reason | Android | iOS | macOS | Web |
|------|----------|----------|--------|---------|-----|-------|-----|
| [AT HOP](https://at.govt.nz/bus-train-ferry/at-hop-card/) | Auckland, NZ | DESFire | Locked | ✅ | ✅ | ✅ | ✅ |
| [Holo](https://www.holocard.net/) | Oahu, HI | DESFire | Not stored on card | ✅ | ✅ | ✅ | ✅ |
| [Istanbul Kart](https://www.istanbulkart.istanbul/) | Istanbul, Turkey | DESFire | Locked | ✅ | ✅ | ✅ | ✅ |
| [Nextfare DESFire](https://en.wikipedia.org/wiki/Cubic_Transportation_Systems) | Various | DESFire | Locked | ✅ | ✅ | ✅ | ✅ |
| [Nol](https://www.nol.ae/) | Dubai, UAE | DESFire | Locked | ✅ | ✅ | ✅ | ✅ |
| [Nortic](https://rfrend.no/) | Scandinavia | DESFire | Locked | ✅ | ✅ | ✅ | ✅ |
| [Presto](https://www.prestocard.ca/) | Ontario, Canada | DESFire | Locked | ✅ | ✅ | ✅ | ✅ |
| [Strelka](https://strelkacard.ru/) | Moscow Region, Russia | Classic 🔒 | Locked | ✅¹ | ❌ | ✅ | ✅ |
| [Sun Card](https://sunrail.com/) | Orlando, FL | Classic 🔒 | Locked | ✅¹ | ❌ | ✅ | ✅ |
| [TPF](https://www.tpf.ch/) | Fribourg, Switzerland | DESFire | Locked | ✅ | ✅ | ✅ | ✅ |
| [TriMet Hop](https://myhopcard.com/) | Portland, OR | DESFire | Not stored on card | ✅ | ✅ | ✅ | ✅ |

## Platform Compatibility

| Protocol | Android | iOS | macOS | Web |
|----------|---------|-----|-------|-----|
| [CEPAS](https://en.wikipedia.org/wiki/CEPAS) | ✅ | ✅ | ✅ | ✅ |
| [FeliCa](https://en.wikipedia.org/wiki/FeliCa) | ✅ | ✅ | ✅ | ✅ |
| [ISO 7816](https://en.wikipedia.org/wiki/ISO/IEC_7816) | ✅ | ✅ | ✅ | ✅ |
| [MIFARE Classic](https://en.wikipedia.org/wiki/MIFARE#MIFARE_Classic) | ✅¹ | ❌ | ✅ | ✅ |
| [MIFARE DESFire](https://en.wikipedia.org/wiki/MIFARE#MIFARE_DESFire) | ✅ | ✅ | ✅ | ✅ |
| [MIFARE Ultralight](https://en.wikipedia.org/wiki/MIFARE#MIFARE_Ultralight_and_MIFARE_Ultralight_EV1) | ✅ | ✅ | ✅ | ✅ |
| [NFC-V / Vicinity](https://en.wikipedia.org/wiki/Near-field_communication#Standards) | ✅ | ✅ | ✅² | ❌ |

¹ Requires NXP NFC chip — most Samsung and some other Android devices use non-NXP controllers and cannot read MIFARE Classic.
² PC/SC readers only. PN533-based USB readers do not support NFC-V.
³ Ultralight variant only.
⁴ DESFire variant only.
🔒 Requires encryption keys — see [Cards Requiring Keys](#cards-requiring-keys).

## Cards Requiring Keys

Some MIFARE Classic cards require encryption keys to read. You can obtain keys using a [Flipper Zero](https://docs.flipper.net/nfc/mf-classic), [Proxmark3](https://github.com/Proxmark/proxmark3/wiki/Mifare-HowTo), or [MFOC](https://github.com/nfc-tools/mfoc). These include:

* Bilhete Único
* Charlie Card
* EasyCard (older MIFARE Classic variant)
* OV-chipkaart
* Oyster
* And most other MIFARE Classic-based cards

## Flipper Zero Integration

FareBot supports connecting to a [Flipper Zero](https://flipperzero.one/) to browse and import NFC card dumps and MIFARE Classic key dictionaries.

| Platform | USB | Bluetooth |
|----------|-----|-----------|
| Android  | Yes | Yes       |
| iOS      | —   | Yes       |
| macOS    | Yes | —         |
| Web      | Yes | Yes       |

From the home screen menu, tap **Flipper Zero** to connect via USB serial or Bluetooth Low Energy, browse the `/ext/nfc` file system, select card dump files (`.nfc`), and import them into your card history. You can also import the Flipper user key dictionary (`mf_classic_dict_user.nfc`) into the app's global key store, which is used as a fallback when reading MIFARE Classic cards.

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
| `make web` | Build web app (experimental, WebAssembly) |
| `make web-run` | Run web app dev server with hot reload |
| `make test` | Run all tests |
| `make clean` | Clean all build artifacts |

A [development container](.devcontainer/README.md) is available for sandboxed development with Claude Code.

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
- `flipper/` — Flipper Zero integration (RPC client, transport abstractions, parsers)
- `app/` — KMP app framework (UI, ViewModels, DI, platform code)
- `app/android/` — Android app shell (Activities, manifest, resources)
- `app/ios/` — iOS app shell (Swift entry point, assets, config)
- `app/desktop/` — macOS desktop app (experimental, PC/SC + PN533 + RC-S956 USB NFC)
- `app/web/` — Web app (experimental, WebAssembly via Kotlin/Wasm)

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
