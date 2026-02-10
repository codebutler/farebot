# Flipper Dump TODO

Card dumps serve two purposes:
1. **Integration tests** — verify Metrodroid port correctness (full pipeline: dump → RawCard → Card → TransitInfo)
2. **Sample cards in Explore tab** — tapping a card in the Explore tab loads a dump and shows parsed transit info

Dumps live in `farebot-app/src/commonTest/resources/` (test) and will also be embedded as app resources for Explore tab samples.

---

## Already have dumps

### FareBot test resources (`farebot-app/src/commonTest/resources/`)

| Card | Type | Dump file | Format | Integration test |
|------|------|-----------|--------|-----------------|
| Clipper | DESFire | `flipper/Clipper.nfc` | Flipper | Yes — 16 trips, $2.25 balance |
| ORCA | DESFire | `flipper/ORCA.nfc` | Flipper | Yes — 0 trips, $26.25 balance |
| Suica | FeliCa | `flipper/Suica.nfc` | Flipper | Yes — 20 trips, 870 JPY balance |
| PASMO | FeliCa | `flipper/PASMO.nfc` | Flipper | Yes — 11 trips, 500 JPY balance |
| ICOCA | FeliCa | `flipper/ICOCA.nfc` | Flipper | Yes — 20 trips, 827 JPY balance |
| EasyCard | Classic | `easycard/deadbeef.mfc` | Raw MFC | No |
| Ventra | Ultralight | `samples/Ventra.json` | Metrodroid JSON | Yes — $8.44 balance, 2 trips |
| Troika (Classic) | Classic | *(programmatic)* | Test code | Yes — E/3 (0 RUB) and E/5 (50 RUB) |

### Metrodroid test assets (`metrodroid/src/commonTest/assets/`)

These are Metrodroid JSON dumps. `CardImporter` already supports this format via `importMetrodroidCard()`.

| Card | Type | Path | Notes |
|------|------|------|-------|
| Opal | DESFire | `opal/opal-transit-litter.json` | "Transit litter" — most files auth-locked. Also: `-manuf.json`, `-auto.json`, `-raw.json` variants, `.xml` |
| HSL v2 | DESFire | `hsl/hslv2.json` | Also: `hslv2-manuf.json`, `hslv2-raw.json` variants |
| HSL Ultralight | Ultralight | `hsl/hslul.json` | Single-use Helsinki ticket |
| Troika | Ultralight | `troika/troikaul.json` | Troika ultralight variant |
| T-Money | ISO7816 | `tmoney/oldtmoney.json` | Old format. Also: `.xml` |
| EZ-Link/NETS | CEPAS | `cepas/legacy.json` | Legacy format. Also: `.xml` |
| Mobib (blank) | ISO7816/Calypso | `iso7816/mobib_blank.json` | Blank card — no trips/balance. Also: `.xml` |
| Holo | DESFire | `holo/unused.json` | Serialonly — card identified but no transit data |
| Selecta | Classic | `selecta/selecta.json` | Vending machine, not transit |
| EasyCard | Classic | `easycard/deadbeef.mfc` | Same as FareBot copy |

The `metrodroid/src/commonTest/assets/farebot/` directory has format-test dumps (Opal, CEPAS, FeliCa, Classic, Ultralight, DESFire) but these are for testing import compatibility, not specific transit systems.

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
| **SeqGo** | `farebot-transit-seqgo` | Medium | Brisbane. Added system code check + refills. |
| **Adelaide Metrocard** | `farebot-transit-adelaide` | Medium | Adelaide, Australia. |
| **Hafilat** | `farebot-transit-hafilat` | Low | Abu Dhabi. |
| **Clipper (locked files)** | `farebot-transit-clipper` | Low | Tests `as?` safe cast fallback. Existing dump may suffice. |
| ~~Holo~~ | serialonly | Sample only | **Have:** `metrodroid/.../holo/unused.json`. Oahu, Hawaii. |
| TriMet Hop | serialonly | Sample only | Portland, OR. |
| Myki | serialonly | Sample only | Victoria, Australia. |
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
| **LAX TAP** | `farebot-transit-lax-tap` | Medium | No | Los Angeles. Nextfare-based. |
| **MSP GoTo** | `farebot-transit-msp-goto` | Medium | No | Minneapolis. Nextfare-based. |
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
| **SeqGo (Classic)** | `farebot-transit-seqgo` | Low | Required | Brisbane Classic variant. |
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
| **Octopus** | `farebot-transit-octopus` | Medium | Hong Kong. |
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
| **Compass** | `farebot-transit-yvr-compass` | Medium | Vancouver. Nextfare UL-based. |
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

| Category | Have (FareBot) | Have (Metrodroid) | On GitHub (not downloaded) | Still need | Sample-only |
|----------|---------------|-------------------|---------------------------|------------|-------------|
| DESFire | 2 (Clipper, ORCA) | 3 (Opal, HSL v2, Holo) | 0 | 7 | 7 |
| Classic | 1 (EasyCard) + Troika Classic (test) | 0 | 1 (Zaragoza) | 24+ | 2 |
| FeliCa | 3 (Suica, PASMO, ICOCA) | 0 | 0 | 3 | 7 |
| Ultralight | 1 (Ventra) | 2 (HSL UL, Troika UL) | 2 (Venezia UL, Andante) | 2 | 0 |
| ISO7816 | 0 | 3 (T-Money, Mobib, EZ-Link†) | 2 (Riga, Mexico City) | 15 | 0 |
| CEPAS | 0 | (†counted above) | 0 | 1 | 0 |
| **Total** | **7** | **8** | **5** | **~52** | **~16** |

† EZ-Link uses CEPAS protocol but is stored as ISO7816-like JSON in Metrodroid.

## Dump format notes

- **Flipper `.nfc`** — Flipper Zero native format. Supported for DESFire, Classic, FeliCa, Ultralight.
- **`.mfc`** — Raw Mifare Classic binary dump (1K = 1024 bytes, 4K = 4096 bytes).
- **FareBot/Metrodroid JSON** — Exported from Android app. Required for ISO7816/CEPAS cards that Flipper can't read.
- All formats are supported by `CardImporter` / `FlipperNfcParser`.
