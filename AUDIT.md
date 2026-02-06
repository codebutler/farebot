# Metrodroid Port Audit

**Date:** 2026-02-06
**Branch:** `claude/update-android-dependencies-87n3T`
**Metrodroid source:** `metrodroid/` directory in this repo (local copy of Metrodroid master branch)
**Methodology:** Each FareBot transit module was compared file-by-file against its Metrodroid counterpart in `metrodroid/`. Detection logic, constants, enums, parsing offsets, trip/balance/subscription handling, station lookups, and string resources were all verified.

## Summary

- **Total modules audited:** 65
- **PASS:** 63 (faithful port, no issues)
- **MINOR:** 2 (cosmetic differences, all features preserved)

All FAIL and KNOWN LIMITATION items have been resolved. All MINOR items have been fixed or confirmed as acceptable.

---

## Architectural Differences (Intentional)

These are systematic differences between FareBot and Metrodroid, applied consistently across all modules. They are intentional adaptations for Kotlin Multiplatform and do not affect functionality.

| FareBot | Metrodroid | Reason |
|---------|-----------|--------|
| `TransitFactory<Card, Info>` | `CardTransitFactory` | Typed generics for compile-time safety |
| `TransitInfo` | `TransitData` | Naming convention |
| Compose Resources (`Res.string.*`) | Android Resources (`R.string.*`) | KMP multiplatform resources |
| `kotlinx.datetime.Instant` | Custom `Timestamp`/`Daystamp` | KMP standard library |
| `kotlinx.serialization.Serializable` | `Parcelize`/`Parcelable` | KMP serialization |
| `ByteArray` | `ImmutableByteArray` | No immutable equivalent in stdlib |
| One Gradle module per transit system | Flat packages under `transit/` | Better build isolation and encapsulation |
| Ultralight factories co-located with parent transit module | Spread across individual transit modules | Each UL factory lives alongside its Classic/DESFire counterpart |
| `StringResource` wrapper or `runBlocking { getString() }` | `Localizer.localizeString(R.string.*)` | KMP string resource access |
| No obfuscation framework | `Preferences.obfuscateTripDates`, `hideCardNumbers` | Privacy feature — not ported (intentional) |

---

## Resolved Items

### farebot-transit-hsl — RESOLVED (was NEEDS POLISH)

**Resolution:** All remaining gaps fixed:
1. **Oulu/Lahti zones** — Added `WALTTI_OULU=229` and `WALTTI_LAHTI=223` city-specific zone name arrays to HSLLookup
2. **MDST region lookup** — `walttiNameRegion()` now does MDST lookup via `waltti_region.mdst` for Waltti city names
3. **Pluralization** — `formatPeriod()` in HSLArvo and HSLKausi now uses `getPluralString()` for proper singular/plural
4. **Purchase date** — HSLArvo overrides `purchaseTimestamp` to null, adds custom purchase date with hour suffix
5. **Unknown fallbacks** — Profile fallback now uses `Res.string.hsl_unknown_format` resource
6. **Ultralight deduplication** — HSL Ultralight factory (now in `farebot-transit-hsl`) shares `HSLArvo`/`HSLLookup` instead of duplicating ~300 lines of code

**Not porting (intentional):**
- Trip time obfuscation — Metrodroid's privacy feature that fuzzes displayed trip times. FareBot does not have an obfuscation framework and does not need one; this is not a functional gap.

### farebot-transit-ovc — RESOLVED (was FAIL)

**Resolution:** Full rewrite to EN1545 framework completed.

| FareBot (new) | Metrodroid | Status |
|---------------|-----------|--------|
| `OvcLookup.kt` | `ovc/OvcLookup.kt` | **NEW** — En1545LookupSTR with MDST stations, 25 subscription names |
| `OVChipTransaction.kt` | `ovc/OVChipTransaction.kt` | **REWRITTEN** — En1545Transaction with infixBitmap fields |
| `OVChipSubscription.kt` | `ovc/OVChipSubscription.kt` | **REWRITTEN** — En1545Subscription with nested bitmap fields |
| `OVChipIndex.kt` | `ovc/OVChipIndex.kt` | **REWRITTEN** — Boolean slot selectors, 12 subscription index pointers |
| `OVChipTransitFactory.kt` | `ovc/OVChipTransitData.kt` (FACTORY) | **REWRITTEN** — EN1545 parsing, trip dedup, TransactionTripLastPrice.merge() |
| `OVChipTransitInfo.kt` | `ovc/OVChipTransitData.kt` (TransitData) | **REWRITTEN** — Balance, autocharge, banned status, BCD birthdate |
| `strings.xml` | Metrodroid `R.string.*` | **UPDATED** — All subscription names, info labels, card type strings |

**Deleted legacy files:** OVChipParser.kt, OVChipPreamble.kt, OVChipCredit.kt, OVChipInfo.kt, OVChipUtil.kt, OVChipTrip.kt

### farebot-transit-snapper — RESOLVED (was KNOWN LIMITATION)

**Resolution:** Full implementation ported. Snapper now has balance reading (NZD), trip parsing (paired SFI 3+4 records with TransactionTrip.merge()), and purse info display.

| FareBot (new) | Metrodroid | Status |
|---------------|-----------|--------|
| `SnapperTransitFactory.kt` | `snapper/SnapperTransitData.kt` (FACTORY) | **REWRITTEN** — Full KSX6924 extraction, balance + trips |
| `SnapperTransitInfo.kt` | `snapper/SnapperTransitData.kt` (TransitData) | **REWRITTEN** — NZD balance, purse info, trip merging |
| `SnapperTransaction.kt` | `snapper/SnapperTransaction.kt` | **NEW** — Transaction with tap-on/off merging, journey grouping |
| `SnapperPurseInfoResolver.kt` | `snapper/SnapperPurseInfoResolver.kt` | **NEW** — Issuer mapping (Snapper Services Ltd.) |

### farebot-transit-seqgo — RESOLVED (was MINOR)

**Resolution:** System code check added, refills now passed to SeqGoTransitInfo constructor. Added `moreInfoPage` and `onlineServicesPage` URLs matching Metrodroid.

### farebot-transit-kmt — RESOLVED (was MINOR)

**Resolution:** Transaction counter and last transaction amount now displayed in info.

### farebot-transit-lax-tap — RESOLVED

**Resolution:** Added `onlineServicesPage` URL (`https://www.taptogo.net/`) matching Metrodroid.

### farebot-transit-myki — RESOLVED

**Resolution:** Added `moreInfoPage` URL. Now extends `SerialOnlyTransitInfo` with `Reason.LOCKED` matching Metrodroid's architecture (Metrodroid has Myki in `serialonly/`).

### MRT Ultralight (in farebot-transit-serialonly) — RESOLVED

**Resolution:** `MRTUltralightTransitInfo` now extends `SerialOnlyTransitInfo` with `Reason.LOCKED` matching Metrodroid's architecture (Metrodroid has MRT Ultralight in `serialonly/`).

### farebot-transit (Subscription base class) — RESOLVED

**Resolution:** Replaced hardcoded English strings in `formatRemainingTrips()` and `formatValidity()` with Compose plural/string resources. PaymentMethod enum now uses string resources instead of hardcoded descriptions.

---

## MINOR Items (Acceptable)

### farebot-transit-clipper — MINOR
**Difference:** FareBot adds `computeBalances()` enhancement and extra agency constants. All Metrodroid features faithfully ported.
**Action needed:** None

### farebot-transit-manly — RESOLVED (was MINOR)
**Resolution:** Rewrote to use ERG framework. ManlyFastFerryTransitInfo now extends ErgTransitInfo, ManlyFastFerryTrip extends ErgTrip, ManlyFastFerryRefill extends ErgRefill — matching the CHC Metrocard pattern. Deleted 6 legacy record classes.

### farebot-transit-pilet — RESOLVED (was MINOR)
**Resolution:** Added BER-TLV extra info display to PiletTransitInfo. Now shows full serial number (PAN), issuer country, card expiration/effective dates, interchange protocol, and Kyiv Digital UID — matching Metrodroid's `PiletTransitData.TAG_MAP`.

---

## Transit System Coverage

All Metrodroid transit systems have been accounted for in FareBot:

| Metrodroid system | FareBot equivalent | Notes |
|---|---|---|
| `ezlinkcompat/` | Not needed | Legacy backward-compat for reading old dump formats |
| `amiibo/` | `farebot-transit-amiibo/AmiiboTransitFactory` | Nintendo Amiibo tag reader (not transit, but ported) |
| `serialonly/MRTUltralightTransitData` | `farebot-transit-serialonly/MRTUltralightTransitFactory` | Extends SerialOnlyTransitInfo |
| `serialonly/MykiTransitData` | `farebot-transit-myki/` | Extends SerialOnlyTransitInfo |
| `intercode/`, `lisboaviva/`, `mobib/`, `opus/`, `pisa/`, `ravkav/`, `venezia/`, `emv/` | `farebot-transit-calypso/` | All 8 Calypso subsystems in one module |

---

## Full Results

### Framework Modules (10/10 PASS)

| Module | Rating | Notes |
|--------|--------|-------|
| farebot-transit-en1545 | **PASS** | Complete EN1545 framework; all 18 files, all constants, timestamp formats match |
| farebot-transit-calypso | **PASS** | 9 Calypso system families (Intercode, Lisboa Viva, Mobib, Opus, Pisa, Rav-Kav, Venezia, EMV) + Pisa/Venezia Ultralight factories |
| farebot-transit-erg | **PASS** | Full ERG/Videlli/Vix framework; all record types match |
| farebot-transit-nextfare | **PASS** | Full Cubic Nextfare Classic framework; all record types and trip merging logic |
| farebot-transit-nextfareul | **PASS** | Full Nextfare Ultralight framework; all field offsets and detection |
| farebot-transit-ndef | **PASS** | 4 card backends, full NDEF/TLV/WiFi parsing, MAD v1/v2 with CRC |
| farebot-transit-pilet | **PASS** | Tartu + Kyiv Digital; detection, serial, and EMV TLV extra info all match |
| farebot-transit-serialonly | **PASS** | 11 serial-only systems + blank/locked handlers for Classic, DESFire, and Ultralight + MRT Ultralight |
| farebot-transit-vicinity | **PASS** | NFC-V blank + unknown handlers |
| farebot-transit-amiibo | **PASS** | Nintendo Amiibo tag reader; character/series/game detection all match |

### Transit A-E (11 PASS, 1 MINOR)

| Module | Rating | Notes |
|--------|--------|-------|
| farebot-transit-adelaide | **PASS** | APP_ID, serial, transaction/subscription fields, lookup all match |
| farebot-transit-bilhete | **PASS** | Detection, serial, trip offsets, CRC16, epoch, balance all match |
| farebot-transit-bip | **PASS** | Key hash, serial, balance sign bit, holder name/ID, trip/refill parsing all match |
| farebot-transit-bonobus | **PASS** | Key hash, date parsing, trip fields, MDST station lookup, route names all match |
| farebot-transit-charlie | **PASS** | Key hash, serial, balance sector selection, trip parsing, price logic all match |
| farebot-transit-chc-metrocard | **PASS** | ERG agency ID 0x0136, NZD currency, MDST station lookup all match |
| farebot-transit-china | **PASS** | All 5 sub-systems (Beijing, NewShenzhen, TUnion, WuhanTong, CityUnion) verified |
| farebot-transit-cifial | **PASS** | Detection, date validation, BCD parsing, room number all match |
| farebot-transit-clipper | **MINOR** | All Metrodroid features ported; extra computeBalances() enhancement |
| farebot-transit-easycard | **PASS** | Magic bytes, balance, transaction offsets, station lookup, trip merging all match |
| farebot-transit-edy | **PASS** | FeliCa services, serial, balance, trip types, epoch 2000 all match |
| farebot-transit-ezlink | **PASS** | CEPAS detection, card issuer routing, EZUserData parsing, station lookup all match |

### Transit F-M (12 PASS, 1 MINOR)

| Module | Rating | Notes |
|--------|--------|-------|
| farebot-transit-gautrain | **PASS** | Detection, EN1545 fields (inlined), balance, OVChipIndex logic all match |
| farebot-transit-hafilat | **PASS** | APP_ID, EN1545 trip/subscription fields, lookup, purse info all match |
| farebot-transit-hsl | **PASS** | Full EN1545 rewrite with V1/V2/Waltti variants, Arvo/Kausi subscriptions, all zone lookups |
| farebot-transit-intercard | **PASS** | Balance, last transaction, serial all match |
| farebot-transit-kazan | **PASS** | Key hash, serial, trip parsing, subscription types, balance logic all match |
| farebot-transit-kiev | **PASS** | Key hash, serial, trip parsing, transaction type, mode detection all match |
| farebot-transit-kmt | **PASS** | Transaction counter and last transaction amount now displayed |
| farebot-transit-komuterlink | **PASS** | Detection, serial, balance, timestamp parsing, trip fields all match |
| farebot-transit-krocap | **PASS** | KR-OCAP detection, serial-only with BER-TLV, KSX6924 exclusion all match |
| farebot-transit-lax-tap | **PASS** | Nextfare detection, agency constants, mode detection, MDST lookup all match |
| farebot-transit-magnacarta | **PASS** | APP_ID, balance, no serial — all match |
| farebot-transit-manly | **PASS** | Now uses ERG framework (ErgTransitInfo/ErgTrip/ErgRefill), matching CHC Metrocard pattern |
| farebot-transit-metromoney | **PASS** | Key hash, serial, balance, date parsing from 4 sectors all match |

### Transit M-P (10 PASS)

| Module | Rating | Notes |
|--------|--------|-------|
| farebot-transit-metroq | **PASS** | Detection, serial, balance sector selection, product mapping all match |
| farebot-transit-mrtj | **PASS** | FeliCa detection, balance, transaction counter all match |
| farebot-transit-msp-goto | **PASS** | Nextfare delegation, detection constants, USD currency all match |
| farebot-transit-myki | **PASS** | DESFire detection, serial parsing, Luhn check all match; extends SerialOnlyTransitInfo with Reason.LOCKED |
| farebot-transit-octopus | **PASS** | FeliCa detection, balance offsets, dual-mode (Octopus/SZT) naming all match |
| farebot-transit-opal | **PASS** | All 12 bit fields, epoch 1980, subscription, modes/actions all match |
| farebot-transit-orca | **PASS** | All agency/FTP/transaction constants, MDST lookups, mode logic all match |
| farebot-transit-otago | **PASS** | Detection, timestamp parsing, balance sector logic, trips/refills all match |
| farebot-transit-ovc | **PASS** | Full EN1545 rewrite; MDST stations, 25 subscription names, trip dedup |
| farebot-transit-oyster | **PASS** | Purse, transaction, refill, travel pass parsing, epoch 1980 all match |

### Transit P-S (8 PASS)

| Module | Rating | Notes |
|--------|--------|-------|
| farebot-transit-podorozhnik | **PASS** | Key hash, serial, sector 4/5 parsing, station lookup, epoch 2010 all match |
| farebot-transit-ricaricami | **PASS** | EN1545 fields, trip/subscription parsing, Milan-specific lookup all match |
| farebot-transit-rkf | **PASS** | Complex 7-file module (Rejsekort, SLaccess, Vasttrafik) faithfully ported |
| farebot-transit-selecta | **PASS** | Detection, serial, balance — simple card, fully matches |
| farebot-transit-seqgo | **PASS** | System code check, auto-topup flag, refills passed to TransitInfo |
| farebot-transit-smartrider | **PASS** | Balance records, tag records, trip bitfields, dual card type (SmartRider/MyWay) all match |
| farebot-transit-snapper | **PASS** | Full implementation: NZD balance, trip merging (SFI 3+4), purse info, KSX6924 |
| farebot-transit-suica | **PASS** | All 10 IC card types, trip parsing, two-pass tap matching, station lookup all match |

### Transit T-Z (12/12 PASS)

| Module | Rating | Notes |
|--------|--------|-------|
| farebot-transit-tampere | **PASS** | APP_ID, serial, balance, contract/trip/subscription parsing all match |
| farebot-transit-tfi-leap | **PASS** | Comprehensive 5-file port; serial, balance, trips, accumulators all match |
| farebot-transit-tmoney | **PASS** | KSX6924 detection, balance, trips, purse info resolver all match |
| farebot-transit-touchngo | **PASS** | Comprehensive 9-file port; balance, trips, refills, travel pass, station lookup all match |
| farebot-transit-troika | **PASS** | Classic + Ultralight; all 5 layouts, 3 epochs match |
| farebot-transit-umarsh | **PASS** | Complex 9-region module; all field offsets, tariffs, denominations match |
| farebot-transit-ventra | **PASS** | NextfareUL detection, USD currency, mode detection all match |
| farebot-transit-waikato | **PASS** | Dual card detection (SmartRide/BUSIT), balance, trips all match |
| farebot-transit-warsaw | **PASS** | TOC detection, serial, timestamp parsing, subscription all match |
| farebot-transit-yargor | **PASS** | Key hash, serial, BCD timestamps, MDST route lookup, subscription types all match |
| farebot-transit-yvr-compass | **PASS** | NextfareUL detection, all 18 product codes, CAD currency, MDST stations all match |
| farebot-transit-zolotayakorona | **PASS** | TOC detection, all 9 card types, trip/refill parsing, timezone lookup all match |
