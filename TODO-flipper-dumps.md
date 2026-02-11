# Flipper Dump TODO

Card dumps serve two purposes:
1. **Integration tests** — verify Metrodroid port correctness (full pipeline: dump → RawCard → Card → TransitInfo)
2. **Sample cards in Explore tab** — tapping a card in the Explore tab loads a dump and shows parsed transit info

Dumps live in `farebot-app/src/commonTest/resources/` (test) and will also be embedded as app resources for Explore tab samples.

---

## Already have dumps with integration tests

### Samples + Tests (`farebot-app/src/commonTest/resources/` and `composeResources/files/samples/`)

All cards below have both Explore screen samples and `SampleDumpIntegrationTest` coverage.

**Data source key:**
- **Real** = from an actual card scan (Flipper, Metrodroid export, or MFC binary)
- **Synthetic** = hand-constructed from unit test data or code constants
- **Needs scan** = a real Flipper/phone scan would improve the sample (more realistic data, actual trips/balances)

| Card | Type | Format | Data source | Needs scan? | Test assertions |
|------|------|--------|-------------|-------------|----------------|
| Clipper | DESFire | Flipper | Real | No | 16 trips, $2.25 balance |
| ORCA | DESFire | Flipper | Real | No | 0 trips, $26.25 balance |
| Suica | FeliCa | Flipper | Real | No | 20 trips, 870 JPY balance |
| PASMO | FeliCa | Flipper | Real | No | 11 trips, 500 JPY balance |
| ICOCA | FeliCa | Flipper | Real | No | 20 trips, 827 JPY balance |
| Opal | DESFire | Metrodroid JSON | Real | No | -$1.82 AUD, serial |
| HSL v2 | DESFire | Metrodroid JSON | Real | No | €0.40, 2 trips, 2 subs |
| HSL UL | Ultralight | Metrodroid JSON | Real | No | 1 trip, 1 subscription |
| Troika UL | Ultralight | Metrodroid JSON | Real | No | trips + subscriptions |
| T-Money | ISO7816 | Metrodroid JSON | Real | No | 17,650 KRW, 5 trips |
| EZ-Link | CEPAS | Metrodroid JSON | Real | No | $8.97 SGD, trips |
| Holo | DESFire | Metrodroid JSON | Real | No | serial-only |
| Mobib | ISO7816 | Metrodroid JSON | Real | No | blank card, 0 trips |
| Ventra | Ultralight | Metrodroid JSON | Real | No | $8.44, 2 trips |
| EasyCard | Classic | Raw MFC | Real | No | 245 TWD, 3 trips |
| Compass | Ultralight | Metrodroid JSON | Synthetic | **Yes** — Flipper UL scan | serial, trips |
| SEQ Go | Classic | Metrodroid JSON | Synthetic | **Yes** — Flipper Classic scan (needs keys) | serial, AUD balance |
| LAX TAP | Classic | Metrodroid JSON | Synthetic | **Yes** — Flipper Classic scan (needs keys) | serial, USD balance |
| MSP GoTo | Classic | Metrodroid JSON | Synthetic | **Yes** — Flipper Classic scan (needs keys) | serial, USD balance |
| Myki | DESFire | Metrodroid JSON | Synthetic | **Yes** — Flipper DESFire scan | serial 308425123456780 |
| Octopus | FeliCa | Metrodroid JSON | Synthetic | **Yes** — Flipper FeliCa scan | -HKD 14.40 balance |
| TriMet Hop | DESFire | Metrodroid JSON | Synthetic | **Yes** — Flipper DESFire scan | serial-only, serial + issue date |
| Bilhete Unico | Classic | Metrodroid JSON | Synthetic | **Yes** — needs proper scan (no trips, zero counters) | R$24.00 balance, no trips |

**Real scans: 15** | **Synthetic (could use real scan): 8** | **Total: 23**

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

## Dumps still needed — by card type

### DESFire (Flipper can read directly)

| Card | Module | Priority | Needs scan? | Notes |
|------|--------|----------|-------------|-------|
| **HSL v1** | `farebot-transit-hsl` | High | Yes — Flipper | Helsinki, old format APP_ID 0x1120ef. Full rewrite, no test coverage. |
| **Waltti** | `farebot-transit-hsl` | High | Yes — Flipper | Oulu/Lahti/etc, APP_ID 0x10ab. Shares HSL module. |
| **Tampere** | `farebot-transit-tampere` | High | Yes — Flipper | Shares HSL-family code. |
| **Leap** | `farebot-transit-tfi-leap` | Medium | Yes — Flipper | Dublin, Ireland. EN1545-based. |
| **Adelaide Metrocard** | `farebot-transit-adelaide` | Medium | Yes — Flipper | Adelaide, Australia. |
| **Hafilat** | `farebot-transit-hafilat` | Low | Yes — Flipper | Abu Dhabi. |
| Nol | serialonly | Sample only | Yes — Flipper | Dubai, UAE. |
| Istanbul Kart | serialonly | Sample only | Yes — Flipper | Istanbul, Turkey. |
| AT HOP | serialonly | Sample only | Yes — Flipper | Auckland, NZ. |
| Presto | serialonly | Sample only | Yes — Flipper | Ontario, Canada. |
| TPF | serialonly | Sample only | Yes — Flipper | Fribourg, Switzerland. |

### Mifare Classic (Flipper can read — keys required for encrypted sectors)

Most Classic cards need sector keys to read useful data. Flipper supports key dictionaries.

**Note on Charlie Card:** `check()` uses salted MD5 key hashes. Our JSON/Flipper parsers don't currently extract keys from the trailer block, so `DataClassicSector.keyA`/`keyB` are always null. Fix: either add key extraction to `RawClassicSector.parse()` (reads bytes 0-5 and 10-15 from trailer block), or add explicit key fields to the JSON format. Once fixed, a Flipper scan with MBTA keys (publicly documented) would work.

| Card | Module | Priority | Keys | Needs scan? | Notes |
|------|--------|----------|------|-------------|-------|
| **OV-chipkaart** | `farebot-transit-ovc` | High | Required | Yes — Flipper + keys | Full EN1545 rewrite, trip dedup, subscriptions, autocharge. 4K card. |
| **Oyster** | `farebot-transit-oyster` | Medium | Required | Yes — Flipper + keys | London. Complex trip parsing. |
| **Charlie Card** | `farebot-transit-charlie` | Medium | MBTA keys (public) | Yes — Flipper + keys + key extraction fix | Boston. See note above. |
| **Podorozhnik** | `farebot-transit-podorozhnik` | Medium | Required | Yes — Flipper + keys | Saint Petersburg. |
| **Bip** | `farebot-transit-bip` | Medium | No | Yes — Flipper | Santiago, Chile. |
| **Bonobus** | `farebot-transit-bonobus` | Medium | No | Yes — Flipper | Cadiz, Spain. |
| **Ricaricami** | `farebot-transit-ricaricami` | Medium | No | Yes — Flipper | Milan, Italy. |
| **Metromoney** | `farebot-transit-metromoney` | Medium | No | Yes — Flipper | Tbilisi, Georgia. |
| **Kyiv Metro** | `farebot-transit-kiev` | Medium | No | Yes — Flipper | Kyiv, Ukraine. |
| **Kyiv Digital** | `farebot-transit-kiev` | Medium | No | Yes — Flipper | Kyiv, Ukraine. Variant. |
| **Metro Q** | `farebot-transit-metroq` | Medium | No | Yes — Flipper | Qatar. |
| **Gautrain** | `farebot-transit-gautrain` | Medium | No | Yes — Flipper | Gauteng, South Africa. |
| **Touch n Go** | `farebot-transit-touchngo` | Medium | No | Yes — Flipper | Malaysia. |
| **KomuterLink** | `farebot-transit-komuterlink` | Medium | No | Yes — Flipper | Malaysia. |
| **SmartRider** | `farebot-transit-smartrider` | Medium | No | Yes — Flipper | Perth, Australia. |
| **Manly Fast Ferry** | `farebot-transit-manly` | Medium | Required | Yes — Flipper + keys | Sydney, Australia. |
| **Otago GoCard** | `farebot-transit-otago` | Medium | No | Yes — Flipper | Otago, NZ. |
| **Tartu Bus** | `farebot-transit-pilet` | Medium | No | Yes — Flipper | Tartu, Estonia. |
| **Warsaw** | `farebot-transit-warsaw` | Medium | Required | Yes — Flipper + keys | Warsaw, Poland. |
| **Kazan** | `farebot-transit-kazan` | Low | Required | Yes — Flipper + keys | Kazan, Russia. |
| **YarGor** | `farebot-transit-yargor` | Low | No | Yes — Flipper | Yaroslavl, Russia. |
| **Christchurch Metrocard** | `farebot-transit-chc-metrocard` | Low | Required | Yes — Flipper + keys | Christchurch, NZ. |
| SLAccess | `farebot-transit-rkf` | Low | Required | Yes — Flipper + keys | Stockholm. Preview. |
| Rejsekort | `farebot-transit-rkf` | Low | Required | Yes — Flipper + keys | Denmark. Preview. |
| Vasttrafik | `farebot-transit-rkf` | Low | Required | Yes — Flipper + keys | Gothenburg. Preview. |
| Sun Card | serialonly | Sample only | No | Yes — Flipper | Orlando, FL. |
| Strelka | serialonly | Sample only | No | Yes — Flipper | Moscow region. |
| Umarsh variants (8) | `farebot-transit-umarsh` | Low | Required | Yes — Flipper + keys | All preview. Yoshkar-Ola, Strizh, Barnaul, Vladimir, Kirov, Siticard, Omka, Penza. |
| Zolotaya Korona variants (5) | `farebot-transit-zolotayakorona` | Low | Required | Yes — Flipper + keys | All preview. Krasnodar, Orenburg, Samara, Yaroslavl. |
| Crimea variants (2) | — | Low | Required | Yes — Flipper + keys | Preview. Trolleybus, Parus school. |

### FeliCa (Flipper can read directly)

| Card | Module | Priority | Needs scan? | Notes |
|------|--------|----------|-------------|-------|
| **Edy** | `farebot-transit-edy` | Medium | Yes — Flipper | Japan e-money. |
| **KMT** | `farebot-transit-kmt` | Medium | Yes — Flipper | Jakarta. FeliCa variant. |
| TOICA | `farebot-transit-suica` | Sample only | Yes — Flipper | Nagoya. Same parser as Suica. |
| manaca | `farebot-transit-suica` | Sample only | Yes — Flipper | Nagoya. Same parser as Suica. |
| PiTaPa | `farebot-transit-suica` | Sample only | Yes — Flipper | Kansai. Same parser as Suica. |
| Kitaca | `farebot-transit-suica` | Sample only | Yes — Flipper | Hokkaido. Same parser as Suica. |
| SUGOCA | `farebot-transit-suica` | Sample only | Yes — Flipper | Fukuoka. Same parser as Suica. |
| nimoca | `farebot-transit-suica` | Sample only | Yes — Flipper | Fukuoka. Same parser as Suica. |
| hayakaken | `farebot-transit-suica` | Sample only | Yes — Flipper | Fukuoka City. Same parser as Suica. |

### Ultralight (Flipper can read directly)

| Card | Module | Priority | Needs scan? | Notes |
|------|--------|----------|-------------|-------|
| **OV-chipkaart UL** | `farebot-transit-ovc` | High | Yes — Flipper | Dutch disposable. Part of OVC rewrite. |

### ISO7816 / Calypso (Flipper CANNOT read — need Android NFC dump)

Flipper Zero does not support ISO 14443-4 / ISO 7816 protocol reads. These require an Android phone running FareBot/Metrodroid to capture the dump, then export as JSON.

| Card | Module | Priority | Needs scan? | Notes |
|------|--------|----------|-------------|-------|
| **Navigo** | `farebot-transit-calypso` | Medium | Yes — Android phone | Paris. EN1545/Intercode. |
| **Opus** | `farebot-transit-calypso` | Medium | Yes — Android phone | Montreal. EN1545/Intercode. |
| **RavKav** | `farebot-transit-calypso` | Medium | Yes — Android phone | Israel. EN1545. |
| **Lisboa Viva** | `farebot-transit-calypso` | Medium | Yes — Android phone | Lisbon. EN1545. |
| **Venezia Unica** | `farebot-transit-calypso` | Medium | Yes — Android phone | Venice. EN1545. Note: UL variant dumps available in metrodroid PR#869. |
| Oura | `farebot-transit-calypso` | Low | Yes — Android phone | Grenoble. EN1545. |
| TaM | `farebot-transit-calypso` | Low | Yes — Android phone | Montpellier. EN1545. |
| Korrigo | `farebot-transit-calypso` | Low | Yes — Android phone | Brittany. EN1545. |
| Envibus | `farebot-transit-calypso` | Low | Yes — Android phone | Sophia Antipolis. EN1545. |
| Carta Mobile | `farebot-transit-calypso` | Low | Yes — Android phone | Pisa. EN1545. |
| Pastel | `farebot-transit-calypso` | Low | Yes — Android phone | Toulouse. Preview. |
| Pass Pass | `farebot-transit-calypso` | Low | Yes — Android phone | Hauts-de-France. Preview. |
| TransGironde | `farebot-transit-calypso` | Low | Yes — Android phone | Gironde. Preview. |
| **Snapper** | `farebot-transit-snapper` | Medium | Yes — Android phone | Wellington, NZ. KSX6924. |
| **Beijing** | `farebot-transit-china` | Low | Yes — Android phone | China T-Union. |
| **Shanghai** | `farebot-transit-china` | Low | Yes — Android phone | China T-Union. |
| **Shenzhen Tong** | `farebot-transit-china` | Low | Yes — Android phone | China T-Union. |
| **Wuhan Tong** | `farebot-transit-china` | Low | Yes — Android phone | China T-Union. |
| **T-Union** | `farebot-transit-china` | Low | Yes — Android phone | China T-Union. |
| **City Union** | `farebot-transit-china` | Low | Yes — Android phone | China T-Union. |

### CEPAS (Flipper CANNOT read — need Android NFC dump)

| Card | Module | Priority | Needs scan? | Notes |
|------|--------|----------|-------------|-------|
| **NETS FlashPay** | `farebot-transit-ezlink` | Medium | Yes — Android phone | Singapore. Shares EZ-Link module. |


---

## Summary

| Category | Have (with tests) | Synthetic (need real scan) | On GitHub (not downloaded) | Still need scan |
|----------|------------------|---------------------------|---------------------------|-----------------|
| DESFire | 8 (Clipper, ORCA, Opal, HSL v2, Holo, Myki†, TriMet Hop†) | 2 (Myki, TriMet Hop) | 0 | 6 full + 5 serial-only |
| Classic | 7 (EasyCard, SEQ Go†, LAX TAP†, MSP GoTo†, Troika*, Bilhete Unico†) | 4 (SEQ Go, LAX TAP, MSP GoTo, Bilhete Unico) | 1 (Zaragoza) | ~25 |
| FeliCa | 4 (Suica, PASMO, ICOCA, Octopus†) | 1 (Octopus) | 0 | 2 full + 7 Suica variants |
| Ultralight | 4 (Ventra, HSL UL, Troika UL, Compass†) | 1 (Compass) | 2 (Venezia UL, Andante) | 1 |
| ISO7816 | 2 (T-Money, Mobib) | 0 | 2 (Riga, Mexico City) | ~20 |
| CEPAS | 1 (EZ-Link) | 0 | 0 | 1 |
| **Total** | **23** (15 real + 8 synthetic) | **8** | **5** | **~67** |

† Synthetic dump — works for tests but a real Flipper/phone scan would provide more realistic data.
\* Troika Classic has programmatic test data only (not a sample file).

## Dump format notes

- **Flipper `.nfc`** — Flipper Zero native format. Supported for DESFire, Classic, FeliCa, Ultralight.
- **`.mfc`** — Raw Mifare Classic binary dump (1K = 1024 bytes, 4K = 4096 bytes).
- **FareBot/Metrodroid JSON** — Exported from Android app. Required for ISO7816/CEPAS cards that Flipper can't read.
- All formats are supported by `CardImporter` / `FlipperNfcParser`.

## Known issues

### Classic card key extraction
Our JSON and Flipper parsers don't extract keyA/keyB from the MIFARE Classic trailer block when parsing. This means `DataClassicSector.keyA` and `keyB` are always null. Cards that use `checkKeyHash()` for detection (e.g., Charlie Card) won't work with dumps until this is fixed.

**Fix:** In `RawClassicSector.parse()`, detect the trailer block (last block of the sector) and extract bytes 0-5 as keyA and bytes 10-15 as keyB, then pass to `DataClassicSector.create()`.
