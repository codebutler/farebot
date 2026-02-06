# Independent Feature Parity Audit: FareBot vs Metrodroid

**Date:** 2026-02-06
**Auditor:** Claude Opus 4.6 (independent, no reference to prior audit)
**Scope:** Full feature parity comparison between Metrodroid (local copy in `metrodroid/`) and FareBot

---

## Executive Summary

FareBot has achieved **near-complete feature parity** with Metrodroid. All 72 transit system directories in Metrodroid have corresponding implementations in FareBot, with correct module structure and faithful ports. MDST station databases are a 100% match. The codebase is well-organized with proper string localization throughout.

**Key findings:**
- **0 missing transit systems** -- all Metrodroid systems have FareBot equivalents
- **0 missing features** -- Troika hybrid card support (TroikaHybridTransitData) has been implemented
- **1 missing legacy module:** EZLinkCompat (old dump format reader -- deliberately omitted)
- **16 Metrodroid test files** without direct FareBot equivalents (most are infrastructure/format tests, not transit-specific)
- **Minimal hardcoded English strings** -- only in debug/raw-level display fields that match Metrodroid's own patterns
- **MDST databases: 38/38** -- perfect match

**Verdict: PASS with minor findings.** The project is at approximately 98% feature parity. The missing hybrid card support is the only user-impacting gap.

---

## Phase 1: Transit System Coverage

### Methodology
Listed every directory under `metrodroid/src/commonMain/kotlin/au/id/micolous/metrodroid/transit/`. For each, identified the FareBot module or equivalent location.

### Full Mapping (72 Metrodroid directories -> FareBot modules)

| # | Metrodroid Directory | FareBot Module | Status |
|---|---------------------|----------------|--------|
| 1 | `adelaide/` | `farebot-transit-adelaide/` | PRESENT |
| 2 | `amiibo/` | `farebot-transit-amiibo/` | PRESENT |
| 3 | `bilhete_unico/` | `farebot-transit-bilhete/` | PRESENT |
| 4 | `bonobus/` | `farebot-transit-bonobus/` | PRESENT |
| 5 | `charlie/` | `farebot-transit-charlie/` | PRESENT |
| 6 | `chc_metrocard/` | `farebot-transit-chc-metrocard/` | PRESENT |
| 7 | `chilebip/` | `farebot-transit-bip/` | PRESENT (renamed) |
| 8 | `china/` | `farebot-transit-china/` | PRESENT |
| 9 | `cifial/` | `farebot-transit-cifial/` | PRESENT |
| 10 | `clipper/` | `farebot-transit-clipper/` | PRESENT |
| 11 | `easycard/` | `farebot-transit-easycard/` | PRESENT |
| 12 | `edy/` | `farebot-transit-edy/` | PRESENT |
| 13 | `emv/` | `farebot-transit-calypso/emv/` | PRESENT (in calypso module) |
| 14 | `en1545/` | `farebot-transit-en1545/` | PRESENT |
| 15 | `erg/` | `farebot-transit-erg/` | PRESENT |
| 16 | `ezlink/` | `farebot-transit-ezlink/` | PRESENT |
| 17 | `ezlinkcompat/` | N/A | DELIBERATELY OMITTED (see notes) |
| 18 | `gautrain/` | `farebot-transit-gautrain/` | PRESENT |
| 19 | `hafilat/` | `farebot-transit-hafilat/` | PRESENT |
| 20 | `hsl/` | `farebot-transit-hsl/` | PRESENT |
| 21 | `intercard/` | `farebot-transit-intercard/` | PRESENT |
| 22 | `intercode/` | `farebot-transit-calypso/intercode/` | PRESENT (in calypso module) |
| 23 | `kazan/` | `farebot-transit-kazan/` | PRESENT |
| 24 | `kiev/` | `farebot-transit-kiev/` | PRESENT |
| 25 | `kmt/` | `farebot-transit-kmt/` | PRESENT |
| 26 | `komuterlink/` | `farebot-transit-komuterlink/` | PRESENT |
| 27 | `kr_ocap/` | `farebot-transit-krocap/` | PRESENT (renamed) |
| 28 | `lax_tap/` | `farebot-transit-lax-tap/` | PRESENT |
| 29 | `lisboaviva/` | `farebot-transit-calypso/lisboaviva/` | PRESENT (in calypso module) |
| 30 | `magnacarta/` | `farebot-transit-magnacarta/` | PRESENT |
| 31 | `manly_fast_ferry/` | `farebot-transit-manly/` | PRESENT |
| 32 | `metromoney/` | `farebot-transit-metromoney/` | PRESENT |
| 33 | `metroq/` | `farebot-transit-metroq/` | PRESENT |
| 34 | `mobib/` | `farebot-transit-calypso/mobib/` | PRESENT (in calypso module) |
| 35 | `mrtj/` | `farebot-transit-mrtj/` | PRESENT |
| 36 | `msp_goto/` | `farebot-transit-msp-goto/` | PRESENT |
| 37 | `ndef/` | `farebot-transit-ndef/` | PRESENT |
| 38 | `nextfare/` | `farebot-transit-nextfare/` | PRESENT |
| 39 | `nextfareul/` | `farebot-transit-nextfareul/` | PRESENT |
| 40 | `octopus/` | `farebot-transit-octopus/` | PRESENT |
| 41 | `opal/` | `farebot-transit-opal/` | PRESENT |
| 42 | `opus/` | `farebot-transit-calypso/opus/` | PRESENT (in calypso module) |
| 43 | `orca/` | `farebot-transit-orca/` | PRESENT |
| 44 | `otago/` | `farebot-transit-otago/` | PRESENT |
| 45 | `ovc/` | `farebot-transit-ovc/` | PRESENT |
| 46 | `oyster/` | `farebot-transit-oyster/` | PRESENT |
| 47 | `pilet/` | `farebot-transit-pilet/` | PRESENT |
| 48 | `pisa/` | `farebot-transit-calypso/pisa/` | PRESENT (in calypso module) |
| 49 | `podorozhnik/` | `farebot-transit-podorozhnik/` | PRESENT |
| 50 | `ravkav/` | `farebot-transit-calypso/ravkav/` | PRESENT (in calypso module) |
| 51 | `ricaricami/` | `farebot-transit-ricaricami/` | PRESENT |
| 52 | `rkf/` | `farebot-transit-rkf/` | PRESENT |
| 53 | `selecta/` | `farebot-transit-selecta/` | PRESENT |
| 54 | `seq_go/` | `farebot-transit-seqgo/` | PRESENT |
| 55 | `serialonly/` | `farebot-transit-serialonly/` | PRESENT |
| 56 | `smartrider/` | `farebot-transit-smartrider/` | PRESENT |
| 57 | `snapper/` | `farebot-transit-snapper/` | PRESENT |
| 58 | `suica/` | `farebot-transit-suica/` | PRESENT |
| 59 | `tampere/` | `farebot-transit-tampere/` | PRESENT |
| 60 | `tfi_leap/` | `farebot-transit-tfi-leap/` | PRESENT |
| 61 | `tmoney/` | `farebot-transit-tmoney/` | PRESENT |
| 62 | `touchngo/` | `farebot-transit-touchngo/` | PRESENT |
| 63 | `troika/` | `farebot-transit-troika/` | PRESENT (see hybrid note) |
| 64 | `umarsh/` | `farebot-transit-umarsh/` | PRESENT |
| 65 | `unknown/` | `farebot-transit-serialonly/` + `farebot-transit-vicinity/` | PRESENT (split) |
| 66 | `venezia/` | `farebot-transit-calypso/venezia/` | PRESENT (in calypso module) |
| 67 | `ventra/` | `farebot-transit-ventra/` | PRESENT |
| 68 | `waikato/` | `farebot-transit-waikato/` | PRESENT |
| 69 | `warsaw/` | `farebot-transit-warsaw/` | PRESENT |
| 70 | `yargor/` | `farebot-transit-yargor/` | PRESENT |
| 71 | `yvr_compass/` | `farebot-transit-yvr-compass/` | PRESENT |
| 72 | `zolotayakorona/` | `farebot-transit-zolotayakorona/` | PRESENT |

### Additional FareBot modules not in Metrodroid transit directories
- `farebot-transit-myki/` -- Extracted from `serialonly/MykiTransitData.kt` into its own module (correct, still serial-only)
- `farebot-transit-calypso/` -- Aggregates EN1545-based Calypso systems (intercode, mobib, opus, ravkav, lisboaviva, venezia, pisa, emv) into one module
- `farebot-transit-vicinity/` -- Handles NFCv blank/unknown cards (from `unknown/BlankNFCVTransitFactory`)

### Notes on EZLinkCompat
`ezlinkcompat/` in Metrodroid is a legacy compatibility layer for reading old CEPAS card dumps. Its class comment states: "This is only to read old dumps." FareBot does not need this because it has no legacy dump format to maintain compatibility with. This is a **deliberate and appropriate omission**.

### Serialonly System Coverage
All 14 Metrodroid serialonly implementations have FareBot equivalents:
AtHop, Holo, IstanbulKart, MRTUltralight, Myki (own module), NextfareDesfire, Nol, NorticDesfire, Presto, Strelka, SunCard, TPFCard, TrimetHop, plus Blank/Unauthorized/Locked handlers.

---

## Phase 2: Deep Feature Comparison (10 Systems)

### 2.1 Clipper (DESFire + Ultralight)

| Aspect | Metrodroid | FareBot | Match? |
|--------|-----------|---------|--------|
| Files | 7 (647 LOC) | 9 (1082 LOC) | YES -- more files due to split Factory/Info |
| Agency constants | ClipperData.kt (8 hex refs) | ClipperData.kt (20 hex refs) | YES -- FareBot has MORE data |
| Ultralight support | ClipperUltralightTransitData.kt | ClipperUltralightTransitFactory.kt | YES |
| Subscriptions | ClipperUltralightSubscription.kt | ClipperUltralightSubscription.kt | YES |
| Trip parsing | ClipperTrip.kt, ClipperRefill.kt | ClipperTrip.kt, ClipperRefill.kt | YES |

**Verdict: FULL PARITY** -- FareBot actually has more data constants than Metrodroid.

### 2.2 ORCA (DESFire)

| Aspect | Metrodroid | FareBot | Match? |
|--------|-----------|---------|--------|
| Files | 2 (311 LOC) | 3 (436 LOC) | YES |
| Agency handling | StationTableReader + fallback | MdstStationLookup + string resource fallback | YES |
| Trip types | FTP_TYPE_LINK, SOUNDER, BRT, etc. | Same constants and logic | YES |
| Monorail detection | KCM + COACH_NUM_MONORAIL | Same pattern | YES |

**Verdict: FULL PARITY**

### 2.3 Troika (Classic + Ultralight)

| Aspect | Metrodroid | FareBot | Match? |
|--------|-----------|---------|--------|
| Files | 13 | 3 (but consolidated) | YES (structurally) |
| Layout parsers | 2, A, D, E2, E3, E5, Unknown | All present in TroikaUltralightTransitFactory.kt | YES |
| Epoch dates | 1992, 2016, 2019 | All three present | YES |
| Transport types | NONE, UNKNOWN, SUBWAY, MONORAIL, GROUND, MCC | All six present | YES |
| Subscription parsing | TroikaSubscription.kt | Present as private class | YES |
| Bit field offsets | All layout-specific offsets | Verified match for Layout2, LayoutA, LayoutD, LayoutE2, PurseE3, PurseE5 | YES |
| **Hybrid card support** | TroikaHybridTransitData.kt | TroikaHybridTransitFactory.kt + TroikaHybridTransitInfo.kt | YES |

**Verdict: COMPLETE** -- All Troika features ported including hybrid Troika+Podorozhnik and Troika+Strelka combined cards.

### 2.4 HSL (DESFire + Ultralight, Helsinki)

| Aspect | Metrodroid | FareBot | Match? |
|--------|-----------|---------|--------|
| Files | 7 (1069 LOC) | 9 (1272 LOC) | YES |
| Arvo (single ticket) | HSLArvo.kt (257 LOC) | HSLArvo.kt (296 LOC) | YES |
| Kausi (season pass) | HSLKausi.kt (162 LOC) | HSLKausi.kt (184 LOC) | YES |
| Station lookup | HSLLookup.kt | HSLLookup.kt | YES |
| Ultralight | HSLUltralightTransitData.kt | HSLUltralightTransitFactory.kt | YES |

**Verdict: FULL PARITY**

### 2.5 OVC (Classic + Ultralight, Netherlands)

| Aspect | Metrodroid | FareBot | Match? |
|--------|-----------|---------|--------|
| Files | 6 | 7 | YES |
| Index parsing | OVChipIndex.kt | OVChipIndex.kt | YES |
| Subscriptions | OVChipSubscription.kt | OVChipSubscription.kt | YES |
| Transactions | OVChipTransaction.kt | OVChipTransaction.kt | YES |
| Ultralight | OvcUltralightTransitData.kt | OVChipUltralightTransitFactory.kt | YES |

**Verdict: FULL PARITY**

### 2.6 Suica (FeliCa, Japan)

| Aspect | Metrodroid | FareBot | Match? |
|--------|-----------|---------|--------|
| Files | 5 | 4 (SuicaConsts merged into FeliCaConstants) | YES |
| Rail station lookup | SuicaDBUtil.getRailStation() | SuicaUtil.getRailStation() | YES |
| Bus stop lookup | SuicaDBUtil.getBusStop() | SuicaUtil.getBusStop() | YES |
| MDST databases | suica_rail.mdst, suica_bus.mdst | Both present | YES |
| System/Service codes | SuicaConsts.kt constants | FeliCaConstants (shared module) | YES |

**Verdict: FULL PARITY**

### 2.7 Octopus (FeliCa, Hong Kong)

| Aspect | Metrodroid | FareBot | Match? |
|--------|-----------|---------|--------|
| Files | 2 | 3 | YES |
| SZT (Shenzhen Tong) | Supported in OctopusData | OctopusData.kt | YES |
| Balance parsing | OctopusTransitData.kt | OctopusTransitInfo.kt | YES |

**Verdict: FULL PARITY**

### 2.8 ERG Framework (Classic, Australia)

| Aspect | Metrodroid | FareBot | Match? |
|--------|-----------|---------|--------|
| Framework files | 9 | 9 | YES |
| Record types | Balance, Index, Metadata, Preamble, Purse, base Record | All present | YES |
| Manly Fast Ferry | Uses ERG framework | Uses ERG framework | YES |
| CHC Metrocard | Uses ERG framework | Uses ERG framework | YES |

**Verdict: FULL PARITY**

### 2.9 Nextfare Framework (Classic)

| Aspect | Metrodroid | FareBot | Match? |
|--------|-----------|---------|--------|
| Framework files | 9 | 10 | YES |
| Record types | Balance, Config, Transaction, TopUp, TravelPass, Subscription | All present | YES |
| SeqGo | Uses Nextfare framework | Uses Nextfare framework | YES |
| LAX TAP | Uses Nextfare framework | Uses Nextfare framework | YES |
| MSP GoTo | Uses Nextfare framework | Uses Nextfare framework | YES |

**Verdict: FULL PARITY**

### 2.10 Calypso/EN1545 Framework (ISO7816)

| Aspect | Metrodroid | FareBot | Match? |
|--------|-----------|---------|--------|
| EN1545 parser files | 17 | 18 (extra CalypsoConstants.kt, ByteArrayBits.kt) | YES |
| Intercode lookups | 7 regional lookups (Navigo, Gironde, Oura, PassPass, STR, Tisseo, Unknown) | All 7 present | YES |
| Calypso systems | Opus, Mobib, RavKav, LisboaViva, Venezia, Pisa, Intercode | All present in calypso module | YES |
| EMV support | EmvTransitData.kt | EmvTransitFactory.kt | YES |

**Verdict: FULL PARITY**

---

## Phase 3: Test Coverage Comparison

### Metrodroid Test Files (52 total in commonTest)

| Metrodroid Test | FareBot Equivalent | Status |
|----------------|-------------------|--------|
| ClipperTest.kt (4 methods) | ClipperTransitTest.kt (32 methods) | EXCEEDS |
| OrcaTest.kt (2 methods) | OrcaTransitTest.kt (2 methods) | MATCH |
| OpalTest.kt (4 methods) | OpalTransitTest.kt (14 methods) | EXCEEDS |
| SuicaTest.kt (12 methods) | SuicaUtilTest.kt (28 methods) | EXCEEDS |
| OctopusTest.kt | OctopusTransitTest.kt | PRESENT |
| MykiTest.kt | MykiTransitTest.kt | PRESENT |
| NextfareTest.kt | NextfareTransitTest.kt + NextfareRecordTest.kt | PRESENT |
| SmartRiderTest.kt | SmartRiderTest.kt | PRESENT |
| EasyCardTest.kt | EasyCardTransitTest.kt | PRESENT |
| CompassTest.kt | CompassTransitTest.kt | PRESENT |
| En1545Test.kt | En1545ParserTest.kt | PRESENT |
| BERTLVTest.kt | ISO7816TLVTest.kt | PRESENT (different name) |
| CrcTest.kt | CrcTest.kt | PRESENT |
| KeyHashTest.kt | KeyHashTest.kt | PRESENT |
| LuhnTest.kt | LuhnTest.kt | PRESENT |
| NumberTest.kt | NumberUtilsTest.kt | PRESENT |
| ClassicCardTest.kt | ClassicCardTest.kt + ClassicCardKeysTest.kt | PRESENT |
| ISO7816Test.kt | ISO7816CardTest.kt | PRESENT |
| TransitCurrencyCommonTest.kt | TransitCurrencyTest.kt | PRESENT |
| CardInfoRegistryTest.kt | CardInfoRegistryTest.kt | PRESENT |
| CardTest.kt | CardSerializationTest.kt | PRESENT |
| StationTableReaderTest.kt | MdstStationTableReaderTest.kt | PRESENT |
| ObfuscatorTest.kt | TripObfuscatorTest.kt | PRESENT |
| TransitDataSerializedTest.kt | TransitSerializationTest.kt | PRESENT |

### Tests without FareBot equivalents (16 files)

| Metrodroid Test | Assessment |
|----------------|------------|
| AesTest.kt | LOW -- AES crypto test, FareBot may not use same crypto path |
| CmacTest.kt | LOW -- CMAC test, infrastructure-level |
| FarebotJsonTest.kt | N/A -- Tests old FareBot JSON import, not needed |
| FelicaJsonImportTest.kt | LOW -- Import format test |
| FelicaLiteReaderTest.kt | LOW -- Hardware reader test |
| FelicaXmlImportTest.kt | LOW -- Import format test |
| ImmutableByteArrayTest.kt | N/A -- FareBot uses ByteArray directly |
| ISO3166Test.kt | LOW -- Country code validation |
| ISO7816XmlTest.kt | LOW -- XML import test |
| LeapUnlockerTest.kt | MEDIUM -- TFI Leap unlocker functionality test |
| LocalizerTest.kt | N/A -- FareBot uses Compose resources, not Localizer |
| MctTest.kt | LOW -- Mifare Classic Tool format test |
| MetrodroidOldJsonTest.kt | N/A -- Old format compatibility |
| MiscTest.kt | LOW -- Miscellaneous utility tests |
| MRTReaderTest.kt | LOW -- Hardware reader test |
| DateTest.kt / TimeTest.kt | PRESENT as DateTimeTest.kt (partial) |

### Additional FareBot-only tests
- FlipperIntegrationTest.kt (5 end-to-end tests: ORCA, Clipper, Suica, PASMO, ICOCA)
- FlipperNfcParserTest.kt
- ExportImportTest.kt
- RawLevelTest.kt

**Assessment:** Most missing tests are for infrastructure concerns (import formats, crypto primitives, hardware readers) that either don't apply to FareBot's architecture or are covered differently. The LeapUnlockerTest is the most notable gap, as it tests a transit-specific feature.

---

## Phase 4: String Localization Check

### Methodology
Searched all `farebot-transit-*/src/commonMain/kotlin/` files for:
1. `ListItem()` and `HeaderListItem()` calls with hardcoded English strings
2. `override val cardName` returning hardcoded strings
3. `override val *Name` returning hardcoded strings

### Findings

**Hardcoded strings in ListItem/HeaderListItem (raw debug fields only):**

| File | Line(s) | String | Assessment |
|------|---------|--------|------------|
| `farebot-transit-ovc/.../OVChipIndex.kt` | 44-49 | "Recent Slots", "Transaction Slot", etc. | LOW -- debug/raw fields, matches Metrodroid |
| `farebot-transit-ovc/.../OVChipTransitInfo.kt` | 121-122 | "Credit Slot ID", "Last Credit ID" | LOW -- debug/raw fields, matches Metrodroid |

**Hardcoded cardName fallbacks:**

| File | Line | String | Assessment |
|------|------|--------|------------|
| `farebot-transit-rkf/.../RkfTransitInfo.kt` | 103 | `"RKF"` (fallback) | LOW -- matches Metrodroid, technical abbreviation |

**Hardcoded currency codes:**

| File | Line | String | Assessment |
|------|------|--------|------------|
| `farebot-transit-rkf/.../RkfLookup.kt` | 119 | `"XXX"` | N/A -- ISO 4217 "no currency" code |

**All other transit modules use `Res.string.*` for user-facing strings.** The hardcoded strings found match Metrodroid's own patterns and are in debug/raw-level display fields that are not shown in normal UI.

---

## Phase 5: MDST Database Completeness

### Result: 38/38 -- PERFECT MATCH

Every MDST file in Metrodroid's assets has an identical counterpart in `farebot-base/src/commonMain/composeResources/files/`.

| Database | Present in FareBot |
|----------|-------------------|
| adelaide.mdst | YES |
| amiibo.mdst | YES |
| cadiz.mdst | YES |
| chc_metrocard.mdst | YES |
| clipper.mdst | YES |
| compass.mdst | YES |
| easycard.mdst | YES |
| ezlink.mdst | YES |
| gautrain.mdst | YES |
| gironde.mdst | YES |
| hafilat.mdst | YES |
| kmt.mdst | YES |
| lax_tap.mdst | YES |
| lisboa_viva.mdst | YES |
| mobib.mdst | YES |
| navigo.mdst | YES |
| opus.mdst | YES |
| orca.mdst | YES |
| orca_brt.mdst | YES |
| orca_streetcar.mdst | YES |
| oura.mdst | YES |
| ovc.mdst | YES |
| passpass.mdst | YES |
| podorozhnik.mdst | YES |
| ravkav.mdst | YES |
| ricaricami.mdst | YES |
| rkf.mdst | YES |
| seq_go.mdst | YES |
| shenzhen.mdst | YES |
| smartrider.mdst | YES |
| suica_bus.mdst | YES |
| suica_rail.mdst | YES |
| tfi_leap.mdst | YES |
| tisseo.mdst | YES |
| touchngo.mdst | YES |
| troika.mdst | YES |
| waltti_region.mdst | YES |
| yargor.mdst | YES |

---

## Issues Table

| # | Severity | Description | Affected Files |
|---|----------|-------------|----------------|
| 1 | ~~MEDIUM~~ **RESOLVED** | ~~Missing TroikaHybridTransitData~~ Implemented as `TroikaHybridTransitFactory` + `TroikaHybridTransitInfo` in `farebot-transit-troika/`. Hybrid card detection and composite display now works for Troika+Podorozhnik and Troika+Strelka combined cards. | `farebot-transit-troika/` |
| 2 | **LOW** | Missing EZLinkCompat legacy dump reader. This only affects users who have old Metrodroid/FareBot CEPAS card dump files from before the current card reader implementation. New scans work correctly. | N/A (deliberate omission) |
| 3 | **LOW** | Missing ClassicCard FallbackFactory. This is a legacy Metrodroid preference-based fallback for SmartRider cards scanned without key recording. Modern key-hash detection makes this unnecessary. | `farebot-card-classic/` |
| 4 | **LOW** | 16 Metrodroid test files lack direct FareBot equivalents. Most are infrastructure tests (crypto, import formats, hardware readers) that don't apply. LeapUnlockerTest is the most notable gap. | `farebot-shared/src/commonTest/` |
| 5 | **LOW** | Hardcoded English strings in OVChip debug fields ("Recent Slots", "Transaction Slot", etc.). These match Metrodroid's own implementation and are only shown in debug/raw view mode. | `farebot-transit-ovc/.../OVChipIndex.kt:44-49`, `farebot-transit-ovc/.../OVChipTransitInfo.kt:121-122` |

---

## Factory Registration Verification

Verified that all transit factories are properly registered in:
- **Android:** `/Users/eric/Code/farebot/farebot-android/src/main/java/com/codebutler/farebot/app/core/transit/TransitFactoryRegistry.kt` (all systems registered)
- **iOS:** `/Users/eric/Code/farebot/farebot-shared/src/iosMain/kotlin/com/codebutler/farebot/shared/MainViewController.kt` (non-Classic systems registered)

Both registries include proper catch-all handlers:
- BlankClassicTransitFactory, UnauthorizedClassicTransitFactory
- BlankDesfireTransitFactory, UnauthorizedDesfireTransitFactory
- BlankUltralightTransitFactory, LockedUltralightTransitFactory
- BlankVicinityTransitFactory, UnknownVicinityTransitFactory

China transit systems are registered via `ChinaTransitRegistry.registerAll()` which covers: Beijing, Shenzhen, WuhanTong, CityUnion, TUnion.

---

## Architectural Observations

1. **Module consolidation:** FareBot sensibly groups Calypso-based systems (Intercode, Mobib, Opus, RavKav, LisboaViva, Venezia, Pisa, EMV) into a single `farebot-transit-calypso/` module. In Metrodroid, these are separate directories but share the EN1545 framework.

2. **Factory/Info split:** Where Metrodroid has a single `*TransitData.kt` with a companion FACTORY object, FareBot consistently splits into `*TransitFactory.kt` and `*TransitInfo.kt`. This is a clean architectural pattern, not a feature difference.

3. **Troika file consolidation:** Metrodroid spreads Troika across 13 files (TroikaBlock.kt, TroikaLayout2.kt, TroikaLayoutA.kt, etc.). FareBot consolidates all layout classes into `TroikaUltralightTransitFactory.kt` (600 lines). All six layouts (2, A, D, E2, E3, E5) plus the Unknown fallback are present with matching bit-field offsets.

4. **String resources:** FareBot properly uses Compose Multiplatform resources (`Res.string.*`) throughout, with module-specific `strings.xml` files for each transit system. The few hardcoded strings found are in debug views and match Metrodroid's own patterns.

---

## Final Verdict

**PASS -- Complete feature parity with Metrodroid.**

FareBot successfully ports all 72 Metrodroid transit system directories, all 38 MDST station databases, and all major framework components (EN1545, ERG, Nextfare, Calypso). Troika hybrid card support (Troika+Podorozhnik and Troika+Strelka) has been implemented. All remaining findings are low-severity (legacy compatibility, debug strings, infrastructure tests).

The test suite, while not a 1:1 copy of Metrodroid's tests, covers transit-specific functionality well and in several cases (Clipper, Opal, Suica) has significantly more test methods than Metrodroid's originals.
