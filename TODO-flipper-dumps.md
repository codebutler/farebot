# Flipper Dump TODO

Card dumps serve two purposes:
1. **Integration tests** — verify Metrodroid port correctness (full pipeline: dump → RawCard → Card → TransitInfo)
2. **Sample cards in Explore tab** — tapping a card in the Explore tab loads a dump and shows parsed transit info

Dumps live in `farebot-app/src/commonTest/resources/` (test) and will also be embedded as app resources for Explore tab samples.

---

## Already have dumps with integration tests

### Samples + Tests (`farebot-app/src/commonTest/resources/` and `composeResources/files/samples/`)

All cards below have both Explore screen samples and `SampleDumpIntegrationTest` coverage.

| Card | Type | Test resource | Explore sample | Format | Test assertions |
|------|------|--------------|----------------|--------|----------------|
| Clipper | DESFire | `flipper/Clipper.nfc` | *(Flipper)* | Flipper | 16 trips, $2.25 balance |
| ORCA | DESFire | `flipper/ORCA.nfc` | *(Flipper)* | Flipper | 0 trips, $26.25 balance |
| Suica | FeliCa | `flipper/Suica.nfc` | *(Flipper)* | Flipper | 20 trips, 870 JPY balance |
| PASMO | FeliCa | `flipper/PASMO.nfc` | *(Flipper)* | Flipper | 11 trips, 500 JPY balance |
| ICOCA | FeliCa | `flipper/ICOCA.nfc` | *(Flipper)* | Flipper | 20 trips, 827 JPY balance |
| Opal | DESFire | `opal/Opal.json` | `Opal.json` | Metrodroid JSON | -$1.82 AUD, serial 3085 2200 7856 2242 |
| HSL v2 | DESFire | `hsl/HSLv2.json` | `HSL.json` | Metrodroid JSON | €0.40, 2 trips, 2 subscriptions |
| HSL UL | Ultralight | `hsl/HSL_UL.json` | `HSL_UL.json` | Metrodroid JSON | 1 trip, 1 subscription |
| Troika UL | Ultralight | `troika/TroikaUL.json` | `Troika.json` | Metrodroid JSON | trips + subscriptions |
| T-Money | ISO7816 | `tmoney/TMoney.json` | `TMoney.json` | Metrodroid JSON | 17,650 KRW, 5 trips |
| EZ-Link | CEPAS | `cepas/EZLink.json` | `EZLink.json` | Metrodroid JSON | $8.97 SGD, trips |
| Holo | DESFire | `holo/Holo.json` | `Holo.json` | Metrodroid JSON | serial-only |
| Mobib | ISO7816 | `mobib/Mobib.json` | `Mobib.json` | Metrodroid JSON | blank card, 0 trips |
| Ventra | Ultralight | *(from Explore)* | `Ventra.json` | Metrodroid JSON | $8.44, 2 trips |
| EasyCard | Classic | `easycard/deadbeef.mfc` | *(MFC)* | Raw MFC | 245 TWD, 3 trips (bus/metro/refill) |
| Compass | Ultralight | `compass/Compass.json` | `Compass.json` | Metrodroid JSON | serial, trips |
| SEQ Go | Classic | `seqgo/SeqGo.json` | `SeqGo.json` | Metrodroid JSON | serial, AUD balance |
| LAX TAP | Classic | `laxtap/LaxTap.json` | `LaxTap.json` | Metrodroid JSON | serial, USD balance |
| MSP GoTo | Classic | `mspgoto/MspGoTo.json` | `MspGoTo.json` | Metrodroid JSON | serial, USD balance |
| Myki | DESFire | `myki/Myki.json` | `Myki.json` | Metrodroid JSON | serial 308425123456780 |
| Octopus | FeliCa | `octopus/Octopus.json` | `Octopus.json` | Metrodroid JSON | -HKD 14.40 balance |

### Metrodroid test assets (reference only — `metrodroid/src/commonTest/assets/`)

| Card | Type | Path | Notes |
|------|------|------|-------|
| Selecta | Classic | `selecta/selecta.json` | Vending machine, not transit |

The `metrodroid/src/commonTest/assets/farebot/` directory has format-test dumps (Opal, CEPAS, FeliCa, Classic, Ultralight, DESFire) for testing import compatibility.

The `metrodroid/src/commonTest/assets/parsed/` directory has expected *parse results* (not raw dumps): Rejsekort, Bilhete Unico, EasyCard, HSL v2, HSL UL, Opal, Troika UL, T-Money, CEPAS, Mobib, Holo, Selecta.

---

## Dumps available on GitHub (not yet downloaded)

These dumps were found in Metrodroid/FareBot issue trackers and can be downloaded.

### High priority — complete Metrodroid JSON dumps, directly downloadable

| Card | Type | Source | Files | Notes |
|------|------|--------|-------|-------|
| **Venezia Unica UL** | Ultralight | [metrodroid PR#869](https://github.com/metrodroid/metrodroid/pull/869) | 12 JSON files (4 cards × 3 reads) | Before/after transaction snapshots. UID pattern `05xxxxxxxx64e9`. |
| **Andante Blue** | Ultralight | [metrodroid#887](https://github.com/metrodroid/metrodroid/issues/887) | 4 JSON files (4 different cards) | Porto, Portugal. 20-page MFU. New system — not yet in FareBot. |
| **Riga E-talons** | Calypso/ISO7816 | [metrodroid#896](https://github.com/metrodroid/metrodroid/issues/896) | 2 JSON files (active + expired) | Latvia. Period tickets and 90-min tickets. New system — not yet in FareBot. |
| **Mexico City Movilidad Integrada** | Calypso/ISO7816 | [metrodroid#707](https://github.com/metrodroid/metrodroid/issues/707) | ZIP with 3 JSON files | Calypso, country code 0x484. New system — not yet in FareBot. |

### Medium priority — partial data or non-standard format

| Card | Type | Source | Data | Notes |
|------|------|--------|------|-------|
| **Zaragoza Tarjeta Bus** | Classic | [metrodroid#756](https://github.com/metrodroid/metrodroid/issues/756) | Google Drive link with MCT dumps | Spain. 16-sector MFC, static keys, before/after each trip. New system — not yet in FareBot. External link may be dead. |
| **Pittsburgh ConnecTix** | Ultralight | [farebot#64](https://github.com/codebutler/farebot/issues/64) | Inline hex (16 pages) | Ten Trip ticket, 2 admissions remaining. 2013 data. New system — not in FareBot. |

### Low priority — insufficient data or serial-only

| Card | Type | Source | Notes |
|------|------|--------|-------|
| TriMet Hop Fastpass | DESFire EV1 | [farebot#147](https://github.com/codebutler/farebot/issues/147) | Full NFC TagInfo XML. Serial-only — balance/trips not stored on card. |
| NY/NJ PATH SmartLink | DESFire | [farebot#63](https://github.com/codebutler/farebot/issues/63) | Card fully locked, no readable data. |
| E-Go Luxembourg | ISO7816 (VDV) | [farebot#72](https://github.com/codebutler/farebot/issues/72) | Only scan metadata, no file contents. |

### Dumps offered privately (not publicly downloadable)

| Card | Type | Source | Notes |
|------|------|--------|-------|
| Tehran Ezpay | Classic | [metrodroid#660](https://github.com/metrodroid/metrodroid/issues/660) | Full 16-sector, static keys. Sent to devs privately. |
| GoExplore (Gold Coast) | Classic | [metrodroid#813](https://github.com/metrodroid/metrodroid/issues/813) | Sent privately. |
| OPUS Quebec disposable | Ultralight | [metrodroid#754](https://github.com/metrodroid/metrodroid/issues/754) | Sent privately. |
| CharlieCard | Classic | [farebot#68](https://github.com/codebutler/farebot/issues/68) | Some data emailed privately. |
| KoriGo / Bibus (Brittany) | Calypso | [metrodroid#837](https://github.com/metrodroid/metrodroid/issues/837) | Offered but not posted. |

---

## Dumps needed — by card type

### DESFire (Flipper can read directly)

| Card | Module | Priority | Notes |
|------|--------|----------|-------|
| **HSL v1** | `farebot-transit-hsl` | High | Helsinki, old format APP_ID 0x1120ef. Full rewrite, no test coverage. |
| ~~HSL v2~~ | `farebot-transit-hsl` | High | **Have:** `metrodroid/.../hsl/hslv2.json`. Helsinki, new file structure. |
| **Waltti** | `farebot-transit-hsl` | High | Oulu/Lahti/etc, APP_ID 0x10ab. Shares HSL module. |
| **Tampere** | `farebot-transit-tampere` | High | Shares HSL-family code. |
| ~~Opal~~ | `farebot-transit-opal` | Medium | **Have:** `metrodroid/.../opal/opal-transit-litter.json`. Most files auth-locked. |
| **Leap** | `farebot-transit-tfi-leap` | Medium | Dublin, Ireland. EN1545-based. |
| ~~SeqGo~~ | `farebot-transit-seqgo` | Medium | **Have:** `seqgo/SeqGo.json` + integration test. Serial, AUD balance. |
| **Adelaide Metrocard** | `farebot-transit-adelaide` | Medium | Adelaide, Australia. |
| **Hafilat** | `farebot-transit-hafilat` | Low | Abu Dhabi. |
| **Clipper (locked files)** | `farebot-transit-clipper` | Low | Tests `as?` safe cast fallback. Existing dump may suffice. |
| ~~Holo~~ | serialonly | Sample only | **Have:** `metrodroid/.../holo/unused.json`. Oahu, Hawaii. |
| TriMet Hop | serialonly | Sample only | Portland, OR. |
| ~~Myki~~ | serialonly | Sample only | **Have:** `myki/Myki.json` + integration test. Serial 308425123456780. |
| Nol | serialonly | Sample only | Dubai, UAE. |
| Istanbul Kart | serialonly | Sample only | Istanbul, Turkey. |
| AT HOP | serialonly | Sample only | Auckland, NZ. |
| Presto | serialonly | Sample only | Ontario, Canada. |
| TPF | serialonly | Sample only | Fribourg, Switzerland. |

### Mifare Classic (Flipper can read — keys required for encrypted sectors)

Most Classic cards need sector keys to read useful data. Flipper supports key dictionaries.

| Card | Module | Priority | Keys | Notes |
|------|--------|----------|------|-------|
| **OV-chipkaart** | `farebot-transit-ovc` | High | Required | Full EN1545 rewrite, trip dedup, subscriptions, autocharge. 4K card. |
| **Oyster** | `farebot-transit-oyster` | Medium | Required | London. Complex trip parsing. |
| ~~Troika (Classic)~~ | `farebot-transit-troika` | Medium | Required | **Have:** integration tests (E/3, E/5 from metrodroid#735). Moscow. Also have UL variant (`metrodroid/.../troika/troikaul.json`). |
| **Podorozhnik** | `farebot-transit-podorozhnik` | Medium | Required | Saint Petersburg. |
| **Charlie Card** | `farebot-transit-charlie` | Medium | No | Boston. |
| ~~LAX TAP~~ | `farebot-transit-lax-tap` | Medium | No | **Have:** `laxtap/LaxTap.json` + integration test. Serial, USD balance. |
| ~~MSP GoTo~~ | `farebot-transit-msp-goto` | Medium | No | **Have:** `mspgoto/MspGoTo.json` + integration test. Serial, USD balance. |
| **Bilhete Unico** | `farebot-transit-bilhete` | Medium | No | Sao Paulo. |
| **Bip** | `farebot-transit-bip` | Medium | No | Santiago, Chile. |
| **Bonobus** | `farebot-transit-bonobus` | Medium | No | Cadiz, Spain. |
| **Ricaricami** | `farebot-transit-ricaricami` | Medium | No | Milan, Italy. |
| **Metromoney** | `farebot-transit-metromoney` | Medium | No | Tbilisi, Georgia. |
| **Kyiv Metro** | `farebot-transit-kiev` | Medium | No | Kyiv, Ukraine. |
| **Kyiv Digital** | `farebot-transit-kiev` | Medium | No | Kyiv, Ukraine. Variant. |
| **Metro Q** | `farebot-transit-metroq` | Medium | No | Qatar. |
| **Gautrain** | `farebot-transit-gautrain` | Medium | No | Gauteng, South Africa. |
| **Touch n Go** | `farebot-transit-touchngo` | Medium | No | Malaysia. |
| **KomuterLink** | `farebot-transit-komuterlink` | Medium | No | Malaysia. |
| **SmartRider** | `farebot-transit-smartrider` | Medium | No | Perth, Australia. |
| **Manly Fast Ferry** | `farebot-transit-manly` | Medium | Required | Sydney, Australia. |
| **Otago GoCard** | `farebot-transit-otago` | Medium | No | Otago, NZ. |
| **Tartu Bus** | `farebot-transit-pilet` | Medium | No | Tartu, Estonia. |
| **Warsaw** | `farebot-transit-warsaw` | Medium | Required | Warsaw, Poland. |
| **Kazan** | `farebot-transit-kazan` | Low | Required | Kazan, Russia. |
| **YarGor** | `farebot-transit-yargor` | Low | No | Yaroslavl, Russia. |
| ~~SeqGo (Classic)~~ | `farebot-transit-seqgo` | Low | Required | **Have:** `seqgo/SeqGo.json` + integration test. Brisbane Classic variant. |
| **Christchurch Metrocard** | `farebot-transit-chc-metrocard` | Low | Required | Christchurch, NZ. |
| SLAccess | `farebot-transit-rkf` | Low | Required | Stockholm. Preview. |
| Rejsekort | `farebot-transit-rkf` | Low | Required | Denmark. Preview. |
| Vasttrafik | `farebot-transit-rkf` | Low | Required | Gothenburg. Preview. |
| Sun Card | serialonly | Sample only | Orlando, FL. |
| Strelka | serialonly | Sample only | Moscow region. |
| Umarsh variants (8) | `farebot-transit-umarsh` | Low | Required | All preview. Yoshkar-Ola, Strizh, Barnaul, Vladimir, Kirov, Siticard, Omka, Penza. |
| Zolotaya Korona variants (5) | `farebot-transit-zolotayakorona` | Low | Required | All preview. Krasnodar, Orenburg, Samara, Yaroslavl. |
| Crimea variants (2) | — | Low | Required | Preview. Trolleybus, Parus school. |

### FeliCa (Flipper can read directly)

| Card | Module | Priority | Notes |
|------|--------|----------|-------|
| **Edy** | `farebot-transit-edy` | Medium | Japan e-money. |
| ~~Octopus~~ | `farebot-transit-octopus` | Medium | **Have:** `octopus/Octopus.json` + integration test. -HKD 14.40 balance. |
| **KMT** | `farebot-transit-kmt` | Medium | Jakarta. Added transaction counter + last amount. FeliCa variant. |
| TOICA | `farebot-transit-suica` | Sample only | Nagoya. Same parser as Suica, just different card name. |
| manaca | `farebot-transit-suica` | Sample only | Nagoya. Same parser as Suica. |
| PiTaPa | `farebot-transit-suica` | Sample only | Kansai. Same parser as Suica. |
| Kitaca | `farebot-transit-suica` | Sample only | Hokkaido. Same parser as Suica. |
| SUGOCA | `farebot-transit-suica` | Sample only | Fukuoka. Same parser as Suica. |
| nimoca | `farebot-transit-suica` | Sample only | Fukuoka. Same parser as Suica. |
| hayakaken | `farebot-transit-suica` | Sample only | Fukuoka City. Same parser as Suica. |

### Ultralight (Flipper can read directly)

| Card | Module | Priority | Notes |
|------|--------|----------|-------|
| ~~Ventra~~ | `farebot-transit-ventra` | Medium | **Have:** `samples/Ventra.json` + integration test. Chicago. Nextfare UL-based. |
| ~~Compass~~ | `farebot-transit-yvr-compass` | Medium | **Have:** `compass/Compass.json` + integration test. Serial, trips. |
| **OV-chipkaart UL** | `farebot-transit-ovc` | High | Dutch disposable. Part of OVC rewrite. |
| ~~HSL Ultralight~~ | `farebot-transit-hsl` | High | **Have:** `metrodroid/.../hsl/hslul.json`. Helsinki single-use tickets. |
| ~~Troika UL~~ | `farebot-transit-troika` | Medium | **Have:** `metrodroid/.../troika/troikaul.json`. Moscow ultralight variant. |

### ISO7816 / Calypso (Flipper CANNOT read — need Android NFC dump)

Flipper Zero does not support ISO 14443-4 / ISO 7816 protocol reads. These require an Android phone running FareBot/Metrodroid to capture the dump, then export as JSON.

| Card | Module | Priority | Notes |
|------|--------|----------|-------|
| **Navigo** | `farebot-transit-calypso` | Medium | Paris. EN1545/Intercode. |
| **Opus** | `farebot-transit-calypso` | Medium | Montreal. EN1545/Intercode. |
| ~~Mobib~~ | `farebot-transit-calypso` | Medium | **Have:** `metrodroid/.../iso7816/mobib_blank.json`. Blank card (no trips). |
| **RavKav** | `farebot-transit-calypso` | Medium | Israel. EN1545. |
| **Lisboa Viva** | `farebot-transit-calypso` | Medium | Lisbon. EN1545. |
| **Venezia Unica** | `farebot-transit-calypso` | Medium | Venice. EN1545. Note: UL variant dumps available in metrodroid PR#869 — needs UL factory. |
| Oura | `farebot-transit-calypso` | Low | Grenoble. EN1545. |
| TaM | `farebot-transit-calypso` | Low | Montpellier. EN1545. |
| Korrigo | `farebot-transit-calypso` | Low | Brittany. EN1545. |
| Envibus | `farebot-transit-calypso` | Low | Sophia Antipolis. EN1545. |
| Carta Mobile | `farebot-transit-calypso` | Low | Pisa. EN1545. |
| Pastel | `farebot-transit-calypso` | Low | Toulouse. Preview. |
| Pass Pass | `farebot-transit-calypso` | Low | Hauts-de-France. Preview. |
| TransGironde | `farebot-transit-calypso` | Low | Gironde. Preview. |
| ~~T-Money~~ | `farebot-transit-tmoney` | Medium | **Have:** `metrodroid/.../tmoney/oldtmoney.json`. Old format. |
| **Snapper** | `farebot-transit-snapper` | Medium | Wellington, NZ. KSX6924. |
| **Beijing** | `farebot-transit-china` | Low | China T-Union. |
| **Shanghai** | `farebot-transit-china` | Low | China T-Union. |
| **Shenzhen Tong** | `farebot-transit-china` | Low | China T-Union. |
| **Wuhan Tong** | `farebot-transit-china` | Low | China T-Union. |
| **T-Union** | `farebot-transit-china` | Low | China T-Union. |
| **City Union** | `farebot-transit-china` | Low | China T-Union. |

### CEPAS (Flipper CANNOT read — need Android NFC dump)

| Card | Module | Priority | Notes |
|------|--------|----------|-------|
| ~~EZ-Link~~ | `farebot-transit-ezlink` | Medium | **Have:** `metrodroid/.../cepas/legacy.json`. Legacy format. |
| **NETS FlashPay** | `farebot-transit-ezlink` | Medium | Singapore. Shares EZ-Link module. |


---

## Summary

| Category | Have (with tests) | On GitHub (not downloaded) | Still need | Sample-only |
|----------|------------------|---------------------------|------------|-------------|
| DESFire | 6 (Clipper, ORCA, Opal, HSL v2, Holo, Myki) | 0 | 5 | 5 |
| Classic | 5 (EasyCard, SEQ Go, LAX TAP, MSP GoTo, Troika Classic†) | 1 (Zaragoza) | 20+ | 2 |
| FeliCa | 4 (Suica, PASMO, ICOCA, Octopus) | 0 | 2 | 7 |
| Ultralight | 4 (Ventra, HSL UL, Troika UL, Compass) | 2 (Venezia UL, Andante) | 1 | 0 |
| ISO7816 | 3 (T-Money, Mobib, EZ-Link‡) | 2 (Riga, Mexico City) | 15 | 0 |
| CEPAS | (‡counted above) | 0 | 1 | 0 |
| **Total** | **21** | **5** | **~44** | **~14** |

† Troika Classic has programmatic test data only (not a sample file).
‡ EZ-Link uses CEPAS protocol but is stored as ISO7816-like JSON in Metrodroid.

## Dump format notes

- **Flipper `.nfc`** — Flipper Zero native format. Supported for DESFire, Classic, FeliCa, Ultralight.
- **`.mfc`** — Raw Mifare Classic binary dump (1K = 1024 bytes, 4K = 4096 bytes).
- **FareBot/Metrodroid JSON** — Exported from Android app. Required for ISO7816/CEPAS cards that Flipper can't read.
- All formats are supported by `CardImporter` / `FlipperNfcParser`.
