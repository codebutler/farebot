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

## Dumps still needed — full implementations

Cards with actual trip/balance/subscription parsing. These are the most valuable to get dump data for.

### DESFire (Flipper can read directly)

| Card | Module | Priority | Needs scan? | Notes |
|------|--------|----------|-------------|-------|
| **HSL v1** | `farebot-transit-hsl` | High | Yes — Flipper | Helsinki, old format APP_ID 0x1120ef. Full rewrite, no test coverage. |
| **Waltti** | `farebot-transit-hsl` | High | Yes — Flipper | Oulu/Lahti/etc, APP_ID 0x10ab. Shares HSL module. |
| **Tampere** | `farebot-transit-tampere` | High | Yes — Flipper | Shares HSL-family code. |
| **Leap** | `farebot-transit-tfi-leap` | Medium | Yes — Flipper | Dublin, Ireland. EN1545-based. |
| **Adelaide Metrocard** | `farebot-transit-adelaide` | Medium | Yes — Flipper | Adelaide, Australia. |
| **Hafilat** | `farebot-transit-hafilat` | Low | Yes — Flipper | Abu Dhabi. |

### Mifare Classic — no keys needed (Flipper can read directly)

These Classic cards don't use encrypted sectors for their transit data, so Flipper can read them like any other card.

| Card | Module | Needs scan? | Notes |
|------|--------|-------------|-------|
| **Bip** | `farebot-transit-bip` | Yes — Flipper | Santiago, Chile. |
| **Bonobus** | `farebot-transit-bonobus` | Yes — Flipper | Cadiz, Spain. |
| **Ricaricami** | `farebot-transit-ricaricami` | Yes — Flipper | Milan, Italy. |
| **Metromoney** | `farebot-transit-metromoney` | Yes — Flipper | Tbilisi, Georgia. |
| **Kyiv Metro** | `farebot-transit-kiev` | Yes — Flipper | Kyiv, Ukraine. |
| **Kyiv Digital** | `farebot-transit-kiev` | Yes — Flipper | Kyiv, Ukraine. Variant. |
| **Metro Q** | `farebot-transit-metroq` | Yes — Flipper | Qatar. |
| **Gautrain** | `farebot-transit-gautrain` | Yes — Flipper | Gauteng, South Africa. |
| **Touch n Go** | `farebot-transit-touchngo` | Yes — Flipper | Malaysia. |
| **KomuterLink** | `farebot-transit-komuterlink` | Yes — Flipper | Malaysia. |
| **SmartRider** | `farebot-transit-smartrider` | Yes — Flipper | Perth, Australia. |
| **Otago GoCard** | `farebot-transit-otago` | Yes — Flipper | Otago, NZ. |
| **Tartu Bus** | `farebot-transit-pilet` | Yes — Flipper | Tartu, Estonia. |
| **YarGor** | `farebot-transit-yargor` | Yes — Flipper | Yaroslavl, Russia. |

### Mifare Classic — keys required (Flipper needs key dictionary)

These cards encrypt their transit sectors. Flipper can crack some keys with `mfkey32` or use a known dictionary, but it's more effort.

**Note on Charlie Card:** `check()` uses salted MD5 key hashes. Our JSON/Flipper parsers don't currently extract keys from the trailer block, so `DataClassicSector.keyA`/`keyB` are always null. Fix: either add key extraction to `RawClassicSector.parse()` (reads bytes 0-5 and 10-15 from trailer block), or add explicit key fields to the JSON format. Once fixed, a Flipper scan with MBTA keys (publicly documented) would work.

| Card | Module | Needs scan? | Notes |
|------|--------|-------------|-------|
| **OV-chipkaart** | `farebot-transit-ovc` | Yes — Flipper + keys | Full EN1545 rewrite, trip dedup, subscriptions, autocharge. 4K card. |
| **Oyster** | `farebot-transit-oyster` | Yes — Flipper + keys | London. Complex trip parsing. |
| **Charlie Card** | `farebot-transit-charlie` | Yes — Flipper + keys + key extraction fix | Boston. MBTA keys are public. See note above. |
| **Podorozhnik** | `farebot-transit-podorozhnik` | Yes — Flipper + keys | Saint Petersburg. |
| **Manly Fast Ferry** | `farebot-transit-manly` | Yes — Flipper + keys | Sydney, Australia. |
| **Warsaw** | `farebot-transit-warsaw` | Yes — Flipper + keys | Warsaw, Poland. |
| **Kazan** | `farebot-transit-kazan` | Yes — Flipper + keys | Kazan, Russia. |
| **Christchurch Metrocard** | `farebot-transit-chc-metrocard` | Yes — Flipper + keys | Christchurch, NZ. |

### FeliCa (Flipper can read directly)

| Card | Module | Priority | Needs scan? | Notes |
|------|--------|----------|-------------|-------|
| **Edy** | `farebot-transit-edy` | Medium | Yes — Flipper | Japan e-money. |
| **KMT** | `farebot-transit-kmt` | Medium | Yes — Flipper | Jakarta. FeliCa variant. |

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

## Dumps still needed — serial-only and preview (low priority)

These cards only show a card name and serial number (no trip/balance parsing), or are `preview = true` (keysRequired, not fully functional). A dump is nice for Explore screen completeness but doesn't exercise much parsing logic.

### Serial-only (identification only)

| Card | Type | Module | Needs scan? | Notes |
|------|------|--------|-------------|-------|
| Nol | DESFire | serialonly | Yes — Flipper | Dubai, UAE. |
| Istanbul Kart | DESFire | serialonly | Yes — Flipper | Istanbul, Turkey. |
| AT HOP | DESFire | serialonly | Yes — Flipper | Auckland, NZ. |
| Presto | DESFire | serialonly | Yes — Flipper | Ontario, Canada. |
| TPF | DESFire | serialonly | Yes — Flipper | Fribourg, Switzerland. |
| Sun Card | Classic | serialonly | Yes — Flipper | Orlando, FL. |
| Strelka | Classic | serialonly | Yes — Flipper | Moscow region. |

### Preview cards (keysRequired + preview, not fully functional)

| Card | Type | Module | Needs scan? | Notes |
|------|------|--------|-------------|-------|
| SLAccess | Classic | `farebot-transit-rkf` | Yes — Flipper + keys | Stockholm. |
| Rejsekort | Classic | `farebot-transit-rkf` | Yes — Flipper + keys | Denmark. |
| Vasttrafik | Classic | `farebot-transit-rkf` | Yes — Flipper + keys | Gothenburg. |
| Umarsh variants (8) | Classic | `farebot-transit-umarsh` | Yes — Flipper + keys | Yoshkar-Ola, Strizh, Barnaul, Vladimir, Kirov, Siticard, Omka, Penza. |
| Zolotaya Korona variants (5) | Classic | `farebot-transit-zolotayakorona` | Yes — Flipper + keys | Krasnodar, Orenburg, Samara, Yaroslavl. |
| Ekarta | Classic | `farebot-transit-zolotayakorona` | Yes — Flipper + keys | Yekaterinburg. |
| Crimea variants (2) | Classic | — | Yes — Flipper + keys | Trolleybus, Parus school. |
| Pastel | ISO7816 | `farebot-transit-calypso` | Yes — Android phone | Toulouse. |
| Pass Pass | ISO7816 | `farebot-transit-calypso` | Yes — Android phone | Hauts-de-France. |
| TransGironde | ISO7816 | `farebot-transit-calypso` | Yes — Android phone | Gironde. |
| BusIt | Classic | `farebot-transit-nextfare` | Yes — Flipper + keys | Waikato, NZ. |
| SmartRide | Classic | `farebot-transit-nextfare` | Yes — Flipper + keys | Rotorua, NZ. |

### Suica-compatible IC cards (same parser, different branding)

These all use the Suica FeliCa parser — a scan just confirms detection, doesn't test new parsing logic.

| Card | Needs scan? | Notes |
|------|-------------|-------|
| TOICA | Yes — Flipper | Nagoya. |
| manaca | Yes — Flipper | Nagoya. |
| PiTaPa | Yes — Flipper | Kansai. |
| Kitaca | Yes — Flipper | Hokkaido. |
| SUGOCA | Yes — Flipper | Fukuoka. |
| nimoca | Yes — Flipper | Fukuoka. |
| hayakaken | Yes — Flipper | Fukuoka City. |

### Calypso/Intercode low-priority (full impl but less common)

| Card | Needs scan? | Notes |
|------|-------------|-------|
| Oura | Yes — Android phone | Grenoble. |
| TaM | Yes — Android phone | Montpellier. |
| Korrigo | Yes — Android phone | Brittany. |
| Envibus | Yes — Android phone | Sophia Antipolis. |
| Carta Mobile | Yes — Android phone | Pisa. |


---

## Summary

| Category | Have (with tests) | Synthetic (need real scan) | On GitHub (not downloaded) | Need scan: no keys | Need scan: keys required | Need scan: serial/preview |
|----------|------------------|---------------------------|---------------------------|--------------------|--------------------------|---------------------------|
| DESFire | 8 | 2 (Myki, TriMet Hop) | 0 | 6 | — | 5 |
| Classic (no keys) | 7 | 4 (SEQ Go, LAX TAP, MSP GoTo, Bilhete Unico) | 1 (Zaragoza) | 14 | — | 2 serial |
| Classic (keys) | — | — | — | — | 8 | ~18 preview |
| FeliCa | 4 | 1 (Octopus) | 0 | 2 | — | 7 Suica variants |
| Ultralight | 4 | 1 (Compass) | 2 (Venezia UL, Andante) | 1 | — | 0 |
| ISO7816 | 2 | 0 | 2 (Riga, Mexico City) | 12 | — | 8 |
| CEPAS | 1 | 0 | 0 | 1 | — | 0 |
| **Total** | **23** (15 real + 8 synthetic) | **8** | **5** | **36 easy** | **8 need keys** | **~40 low-pri** |

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
