# CLAUDE.md — Project Rules for FareBot

## Project Overview

FareBot is a Kotlin Multiplatform (KMP) Android/iOS app for reading NFC transit cards. It is being ported from/aligned with [Metrodroid](https://github.com/metrodroid/metrodroid).

**Metrodroid source code is in the `metrodroid/` directory in this repo.** Always use this local copy for comparisons and porting — do not fetch from GitHub.

## Critical Rules

### 1. NEVER lose existing features

When refactoring, rewriting, or porting code: **every existing feature must be preserved**. Before modifying a file, understand what it currently does. After modifying it, verify nothing was lost. Do not silently drop functionality — if something must change, say so explicitly.

Common regressions to watch for:
- Missing UI elements (images, buttons, screens)
- Lost navigation paths (menu items, long-press handlers)
- Removed data fields from transit info display
- Broken sample data loading

### 2. No stubs — use serialonly for identification-only systems

Do NOT create stub/skeleton transit implementations that only show a card name and serial number **when Metrodroid has a full implementation available to port**. If Metrodroid has trip parsing, balance reading, subscriptions, or other features for a system, port all of it — never reduce a full implementation to a stub.

For systems where Metrodroid itself only supports identification (card name + serial number) with no further parsing, use `transit/serialonly/` — matching Metrodroid's `serialonly/` directory. These extend `SerialOnlyTransitInfo` and provide a `Reason` (LOCKED, NOT_STORED, MORE_RESEARCH_NEEDED) explaining why data isn't available.

If a full implementation can't be ported yet (e.g., missing infrastructure framework), don't add the system at all until the dependency is ready.

### 3. Faithful ports from Metrodroid

When porting code from Metrodroid: **do a faithful port**. Do not simplify, abbreviate, or "improve" the logic. Port ALL features, ALL edge cases, ALL constants. After writing each file, diff it against the Metrodroid original to verify nothing was missed.

- `ImmutableByteArray` → `ByteArray`
- `Parcelize`/`Parcelable` → `kotlinx.serialization.Serializable`
- `Localizer.localizeString(R.string.x)` → `stringResource.getString(Res.string.x)`
- `Timestamp`/`TimestampFull`/`Daystamp` → `kotlinx.datetime.Instant`
- `TransitData` → `TransitInfo`
- `CardTransitFactory` → `TransitFactory<CardType, TransitInfoType>`

Do NOT:
- Skip features "for later"
- Change logic unless there's a concrete reason
- Remove constants, enums, or data that exist in the original
- Simplify switch/when statements by dropping cases

### 4. Debug systematically, not speculatively

When something is broken: **add logging and diagnostics first**. Do not guess at fixes. The workflow should be:

1. Add debug logging to understand what's actually happening
2. Read the device console output
3. Identify the root cause from actual data
4. Fix the specific problem
5. Remove debug logging

Do NOT make speculative changes hoping they fix the issue. Each failed guess wastes a round.

### 5. All code in commonMain unless it requires OS APIs

Write all code in `src/commonMain/kotlin/`. Only use `androidMain` or `iosMain` for code that directly interfaces with platform APIs (NFC hardware, file system, UI system dialogs). No Objective-C. Tests use `kotlin.test`.

### 6. Use StringResource for all user-facing strings

All user-facing strings must go through Compose Multiplatform resources:
- Define strings in `src/commonMain/composeResources/values/strings.xml`
- For UI labels in `TransitInfo.getInfo()`, use `ListItem(Res.string.xxx, value)` or `HeaderListItem(Res.string.xxx)` directly
- For dynamic string formatting, use `runBlocking { getString(Res.string.xxx) }`
- Legacy pattern: Pass `StringResource` to factories — still works but not required for new code

Example patterns:
```kotlin
// Preferred for static labels
ListItem(Res.string.card_type, cardType)
HeaderListItem(Res.string.card_details)

// For dynamic values
val formatted = runBlocking { getString(Res.string.balance_format) }
```

Do NOT hardcode English strings in Kotlin files.

### 7. Use MDST for station lookups, not SQLite .db3

Station databases should use the MDST (protobuf) format via `MdstStationLookup`, not SQLite .db3 files with SQLDelight. All MDST files live in `base/src/commonMain/composeResources/files/` and are accessed via `MdstStationLookup.getStation(dbName, stationId)`.

Example:
```kotlin
val station = MdstStationLookup.getStation("orca", stationId)
station?.stationName  // English name
station?.companyName  // Operator name
station?.latitude     // GPS coordinates (if available)
```

### 8. Verify your own work

After making changes:
- Run `./gradlew allTests` to confirm tests pass
- Run `./gradlew assemble` to confirm the build succeeds
- If you changed UI code, describe what the user should see
- If you ported code, diff against the original source

Do NOT claim work is complete without verification.

### 9. Preserve context across sessions

Key project state is in:
- `/Users/eric/.claude/plans/` — implementation plans (check newest first)
- `/Users/eric/Code/farebot/REMAINING-WORK.md` — tracked remaining work
- Session transcripts in `/Users/eric/.claude/projects/-Users-eric-Code-farebot/`

When continuing from a previous session, read these files to recover context rather than starting from scratch.

## Build Commands

```bash
./gradlew allTests          # Run all tests
./gradlew assemble          # Full build (Android + iOS frameworks)
./gradlew :app:android:assembleDebug  # Android only
```

## Module Structure

- `base/` — Core utilities, MDST reader, ByteArray extensions (`:base`)
- `card/` — Shared card abstractions (`:card`)
- `card/*/` — Card type implementations: classic, desfire, felica, ultralight, iso7816, cepas, vicinity (`:card:*`)
- `transit/` — Shared transit abstractions: Trip, Station, TransitInfo, TransitCurrency (`:transit`)
- `transit/*/` — Transit system implementations, one per system (`:transit:*`)
- `transit/serialonly/` — Identification-only systems (serial number + reason, matches Metrodroid's `serialonly/`)
- `app/` — KMP app framework: UI, ViewModels, DI, platform code (`:app`)
- `app/android/` — Android app shell: Activities, manifest, resources (`:app:android`)
- `app/desktop/` — Desktop app shell (`:app:desktop`)
- `app/ios/` — iOS app shell: Swift entry point, assets, config (Xcode project, not a Gradle module)

## Registration Checklist for New Transit Modules

1. Create `transit/{name}/build.gradle.kts`
2. Add `include(":transit:{name}")` to `settings.gradle.kts`
3. Add `api(project(":transit:{name}"))` to `app/build.gradle.kts`
4. Register factory in `TransitFactoryRegistry.kt` (Android)
5. Register factory in `MainViewController.kt` (iOS, non-Classic cards only)
6. Add string resources in `composeResources/values/strings.xml`

## Kotlin/Native Gotchas

- `internal` types cannot be exposed in public APIs (stricter than JVM)
- Constructor parameter names matter — use the exact names the data class defines
- When removing a transitive dependency, add direct `api()` deps for anything that was accessed transitively
