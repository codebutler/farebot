# Remove Builder Classes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove all Builder classes and replace with idiomatic Kotlin patterns (data class constructors, DSL).

**Architecture:** Three simple Builder classes (ClipperTrip, SeqGoTrip, Station) are replaced with data class constructors using named parameters and defaults. The hierarchical FareBotUiTree Builder is replaced by rewriting the existing `uiTree {}` DSL to build items directly, and converting all 19 imperative builder call sites to use the DSL.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, compose-resources

---

## Task 1: Rewrite FareBotUiTree data model

**Files:**
- Modify: `base/src/commonMain/kotlin/com/codebutler/farebot/base/ui/FareBotUiTree.kt`

**Step 1: Rewrite FareBotUiTree.kt**

Replace the entire file with:

```kotlin
package com.codebutler.farebot.base.ui

import com.codebutler.farebot.base.util.FormattedString

data class FareBotUiTree(
    val items: List<Item>,
) {
    data class Item(
        val title: FormattedString,
        val value: Any? = null,
        val children: List<Item> = emptyList(),
    )
}
```

Key changes:
- Remove `@Serializable` from both classes (never serialized)
- Change `Item.title` from `String` to `FormattedString`
- Remove `Item.Builder`, `FareBotUiTree.Builder`, companion objects
- Remove `@Contextual` annotation on `value`
- `children` and `value` get defaults

**Step 2: Commit**

```bash
git add base/src/commonMain/kotlin/com/codebutler/farebot/base/ui/FareBotUiTree.kt
git commit -m "refactor: simplify FareBotUiTree to plain data classes

Remove Builder classes and @Serializable annotation. Change Item.title
from String to FormattedString to defer resolution to UI layer."
```

---

## Task 2: Rewrite UiTreeBuilder DSL

**Files:**
- Modify: `base/src/commonMain/kotlin/com/codebutler/farebot/base/ui/UiTreeBuilder.kt`

**Step 1: Rewrite UiTreeBuilder.kt**

Replace the entire file with:

```kotlin
package com.codebutler.farebot.base.ui

import com.codebutler.farebot.base.util.FormattedString
import org.jetbrains.compose.resources.StringResource

@DslMarker
private annotation class UiTreeBuilderMarker

fun uiTree(init: TreeScope.() -> Unit): FareBotUiTree {
    val scope = TreeScope()
    scope.init()
    return FareBotUiTree(scope.items.toList())
}

@UiTreeBuilderMarker
class TreeScope {
    internal val items = mutableListOf<FareBotUiTree.Item>()

    fun item(init: ItemScope.() -> Unit) {
        val scope = ItemScope()
        scope.init()
        items.add(scope.build())
    }
}

@UiTreeBuilderMarker
class ItemScope {
    private var _title: FormattedString = FormattedString("")
    var title: Any?
        get() = _title
        set(value) {
            _title = when (value) {
                is FormattedString -> value
                is StringResource -> FormattedString(value)
                else -> FormattedString(value.toString())
            }
        }

    var value: Any? = null

    private val children = mutableListOf<FareBotUiTree.Item>()

    fun item(init: ItemScope.() -> Unit) {
        val scope = ItemScope()
        scope.init()
        children.add(scope.build())
    }

    fun addChildren(items: List<FareBotUiTree.Item>) {
        children.addAll(items)
    }

    internal fun build(): FareBotUiTree.Item = FareBotUiTree.Item(
        title = _title,
        value = value,
        children = children.toList(),
    )
}
```

Key changes:
- `uiTree` is no longer `suspend` (no async string resolution during build)
- DSL builds `Item` objects directly instead of delegating to Builder
- `ItemScope.title` accepts `Any?` (String, StringResource, FormattedString) for ergonomics
- `addChildren()` allows appending pre-built items (for OVChipIndex pattern)

**Step 2: Commit**

```bash
git add base/src/commonMain/kotlin/com/codebutler/farebot/base/ui/UiTreeBuilder.kt
git commit -m "refactor: rewrite uiTree DSL to build items directly

No longer wraps Builder classes. The DSL constructs FareBotUiTree.Item
objects directly. uiTree is no longer suspend since FormattedString
resolution is deferred to the UI layer."
```

---

## Task 3: Update CardAdvancedScreen for FormattedString title

**Files:**
- Modify: `app/src/commonMain/kotlin/com/codebutler/farebot/shared/ui/screen/CardAdvancedScreen.kt`

**Step 1: Change `item.title.orEmpty()` to `item.title.resolve()`**

In the `TreeItemView` composable, change:
```kotlin
Text(
    text = item.title.orEmpty(),
```
to:
```kotlin
Text(
    text = item.title.resolve(),
```

The `resolve()` method is a `@Composable` function on `FormattedString` that resolves string resources at render time.

**Step 2: Commit**

```bash
git add app/src/commonMain/kotlin/com/codebutler/farebot/shared/ui/screen/CardAdvancedScreen.kt
git commit -m "refactor: resolve FormattedString title in CardAdvancedScreen"
```

---

## Task 4: Convert card module getAdvancedUi() to DSL

**Files (8):**
- `card/classic/src/commonMain/kotlin/.../ClassicCard.kt`
- `card/desfire/src/commonMain/kotlin/.../DesfireCard.kt`
- `card/felica/src/commonMain/kotlin/.../FelicaCard.kt`
- `card/iso7816/src/commonMain/kotlin/.../ISO7816Card.kt`
- `card/cepas/src/commonMain/kotlin/.../CEPASCard.kt`
- `card/ultralight/src/commonMain/kotlin/.../UltralightCard.kt`
- `card/vicinity/src/commonMain/kotlin/.../VicinityCard.kt`
- `card/ksx6924/src/commonMain/kotlin/.../KSX6924PurseInfo.kt`

**Step 1: Convert each file's getAdvancedUi() from builder to DSL**

Each file follows the same pattern. Replace:
```kotlin
import com.codebutler.farebot.base.ui.FareBotUiTree
// ...
val builder = FareBotUiTree.builder()
builder.item().title("X").value(y)
return builder.build()
```
with:
```kotlin
import com.codebutler.farebot.base.ui.uiTree
// ...
return uiTree {
    item { title = "X"; value = y }
}
```

Specific conversions per file:

**ClassicCard.kt** — nested sectors with conditional titles:
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
    for (sector in sectors) {
        val sectorIndexString = sector.index.toString(16)
        item {
            when (sector) {
                is UnauthorizedClassicSector -> {
                    title = FormattedString(Res.string.classic_unauthorized_sector_title_format, sectorIndexString)
                }
                is InvalidClassicSector -> {
                    title = FormattedString(Res.string.classic_invalid_sector_title_format, sectorIndexString, sector.error)
                }
                else -> {
                    val dataClassicSector = sector as DataClassicSector
                    title = FormattedString(Res.string.classic_sector_title_format, sectorIndexString)
                    for (block in dataClassicSector.blocks) {
                        item {
                            title = FormattedString(Res.string.classic_block_title_format, block.index.toString())
                            value = block.data
                        }
                    }
                }
            }
        }
    }
}
```

**DesfireCard.kt** — deeply nested apps/files/settings:
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
    item {
        title = "Applications"
        for (app in applications) {
            item {
                title = "Application: 0x${app.id.toString(16)}"
                item {
                    title = "Files"
                    for (file in app.files) {
                        item {
                            title = "File: 0x${file.id.toString(16)}"
                            val fileSettings = file.fileSettings
                            if (fileSettings != null) {
                                item {
                                    title = "Settings"
                                    item { title = "Type"; value = fileSettings.fileTypeName }
                                    if (fileSettings is StandardDesfireFileSettings) {
                                        item { title = "Size"; value = fileSettings.fileSize }
                                    } else if (fileSettings is RecordDesfireFileSettings) {
                                        item { title = "Cur Records"; value = fileSettings.curRecords }
                                        item { title = "Max Records"; value = fileSettings.maxRecords }
                                        item { title = "Record Size"; value = fileSettings.recordSize }
                                    } else if (fileSettings is ValueDesfireFileSettings) {
                                        item { title = "Range"; value = "${fileSettings.lowerLimit} - ${fileSettings.upperLimit}" }
                                        item {
                                            title = "Limited Credit"
                                            value = "${fileSettings.limitedCreditValue} (${if (fileSettings.limitedCreditEnabled) "enabled" else "disabled"})"
                                        }
                                    }
                                }
                            }
                            if (file is StandardDesfireFile) {
                                item { title = "Data"; value = file.data }
                            } else if (file is RecordDesfireFile) {
                                item {
                                    title = "Records"
                                    for (i in file.records.indices) {
                                        item { title = "Record $i"; value = file.records[i].data }
                                    }
                                }
                            } else if (file is ValueDesfireFile) {
                                item { title = "Value"; value = file.value }
                            } else if (file is InvalidDesfireFile) {
                                item { title = "Error"; value = file.errorMessage }
                            } else if (file is UnauthorizedDesfireFile) {
                                item { title = "Error"; value = file.errorMessage }
                            }
                        }
                    }
                }
            }
        }
    }
    item {
        title = "Manufacturing Data"
        item {
            title = "Hardware Information"
            item { title = "Vendor ID"; value = manufacturingData.hwVendorID }
            item { title = "Type"; value = manufacturingData.hwType }
            item { title = "Subtype"; value = manufacturingData.hwSubType }
            item { title = "Major Version"; value = manufacturingData.hwMajorVersion }
            item { title = "Minor Version"; value = manufacturingData.hwMinorVersion }
            item { title = "Storage Size"; value = manufacturingData.hwStorageSize }
            item { title = "Protocol"; value = manufacturingData.hwProtocol }
        }
        item {
            title = "Software Information"
            item { title = "Vendor ID"; value = manufacturingData.swVendorID }
            item { title = "Type"; value = manufacturingData.swType }
            item { title = "Subtype"; value = manufacturingData.swSubType }
            item { title = "Major Version"; value = manufacturingData.swMajorVersion }
            item { title = "Minor Version"; value = manufacturingData.swMinorVersion }
            item { title = "Storage Size"; value = manufacturingData.swStorageSize }
            item { title = "Protocol"; value = manufacturingData.swProtocol }
        }
        item {
            title = "General Information"
            item { title = "Serial Number"; value = manufacturingData.uidHex }
            item { title = "Batch Number"; value = manufacturingData.batchNoHex }
            item { title = "Week of Production"; value = manufacturingData.weekProd.toString(16) }
            item { title = "Year of Production"; value = manufacturingData.yearProd.toString(16) }
        }
    }
}
```

**FelicaCard.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
    item { title = "IDm"; value = idm }
    item { title = "PMm"; value = pmm }
    item {
        title = "Systems"
        for (system in systems) {
            item {
                title = "System: ${system.code.toString(16)}"
                for (service in system.services) {
                    item {
                        title = "Service: 0x${service.serviceCode.toString(16)} (${FelicaUtils.getFriendlyServiceName(system.code, service.serviceCode)})"
                        for (block in service.blocks) {
                            item {
                                title = "Block ${block.address.toString().padStart(2, '0')}"
                                value = block.data
                            }
                        }
                    }
                }
            }
        }
    }
}
```

**ISO7816Card.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
    item {
        title = "Applications"
        for (app in applications) {
            item {
                val appNameStr = app.appName?.let { formatAID(it) } ?: "Unknown"
                title = "Application: $appNameStr (${app.type})"
                if (app.files.isNotEmpty()) {
                    item {
                        title = "Files"
                        for ((selector, file) in app.files) {
                            item {
                                title = "File: $selector"
                                if (file.binaryData != null) {
                                    item { title = "Binary Data"; value = file.binaryData }
                                }
                                for ((index, record) in file.records.entries.sortedBy { it.key }) {
                                    item { title = "Record $index"; value = record }
                                }
                            }
                        }
                    }
                }
                if (app.sfiFiles.isNotEmpty()) {
                    item {
                        title = "SFI Files"
                        for ((sfi, file) in app.sfiFiles.entries.sortedBy { it.key }) {
                            item {
                                title = "SFI 0x${sfi.toString(16)}"
                                if (file.binaryData != null) {
                                    item { title = "Binary Data"; value = file.binaryData }
                                }
                                for ((index, record) in file.records.entries.sortedBy { it.key }) {
                                    item { title = "Record $index"; value = record }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

**CEPASCard.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
    for (purse in purses) {
        item {
            title = "Purse ID ${purse.id}"
            item { title = "CEPAS Version"; value = purse.cepasVersion }
            item { title = "Purse Status"; value = purse.purseStatus }
            item { title = "Purse Balance"; value = CurrencyFormatter.formatValue(purse.purseBalance / 100.0, "SGD") }
            item { title = "Purse Creation Date"; value = formatDate(Instant.fromEpochMilliseconds(purse.purseCreationDate * 1000L), DateFormatStyle.LONG) }
            item { title = "Purse Expiry Date"; value = formatDate(Instant.fromEpochMilliseconds(purse.purseExpiryDate * 1000L), DateFormatStyle.LONG) }
            item { title = "Autoload Amount"; value = purse.autoLoadAmount }
            item { title = "CAN"; value = purse.can }
            item { title = "CSN"; value = purse.csn }
        }
        item {
            title = "Last Transaction Information"
            item { title = "TRP"; value = purse.lastTransactionTRP }
            item { title = "Credit TRP"; value = purse.lastCreditTransactionTRP }
            item { title = "Credit Header"; value = purse.lastCreditTransactionHeader }
            item { title = "Debit Options"; value = purse.lastTransactionDebitOptionsByte }
        }
        item {
            title = "Other Purse Information"
            item { title = "Logfile Record Count"; value = purse.logfileRecordCount }
            item { title = "Issuer Data Length"; value = purse.issuerDataLength }
            item { title = "Issuer-specific Data"; value = purse.issuerSpecificData }
        }
    }
}
```

**UltralightCard.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
    item {
        title = Res.string.ultralight_pages
        for (page in pages) {
            item {
                title = FormattedString(Res.string.ultralight_page_title_format, page.index.toString())
                value = page.data
            }
        }
    }
}
```

**VicinityCard.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
    if (sysInfo != null) {
        item { title = "System Info"; value = sysInfo }
    }
    item {
        title = "Pages"
        for (page in pages) {
            item {
                title = "Page ${page.index}"
                value = if (page.isUnauthorized) "Unauthorized" else page.data
            }
        }
    }
}
```

**KSX6924PurseInfo.kt:**
```kotlin
suspend fun getAdvancedInfo(resolver: KSX6924PurseInfoResolver = KSX6924PurseInfoDefaultResolver): FareBotUiTree = uiTree {
    item { title = Res.string.ksx6924_crypto_algorithm; value = resolver.resolveCryptoAlgo(alg) }
    item { title = Res.string.ksx6924_encryption_key_version; value = vk.hexString }
    item { title = Res.string.ksx6924_auth_id; value = idtr.hexString }
    item { title = Res.string.ksx6924_ticket_type; value = resolver.resolveUserCode(userCode) }
    item { title = Res.string.ksx6924_max_balance; value = balMax.toString() }
    item { title = Res.string.ksx6924_branch_code; value = bra.hexString }
    item { title = Res.string.ksx6924_one_time_limit; value = mmax.toString() }
    item { title = Res.string.ksx6924_mobile_carrier; value = resolver.resolveTCode(tcode) }
    item { title = Res.string.ksx6924_financial_institution; value = resolver.resolveCCode(ccode) }
    item { title = Res.string.ksx6924_rfu; value = rfu.hex() }
}
```

For each file, update imports: replace `import com.codebutler.farebot.base.ui.FareBotUiTree` with `import com.codebutler.farebot.base.ui.uiTree` (and keep FareBotUiTree import only if the type is referenced in return type annotations).

**Step 2: Commit**

```bash
git add card/
git commit -m "refactor: convert card getAdvancedUi() from builder to DSL"
```

---

## Task 5: Convert transit module getAdvancedUi() to DSL

**Files (12):**
- `transit/ovc/src/commonMain/kotlin/.../OVChipTransitInfo.kt`
- `transit/ovc/src/commonMain/kotlin/.../OVChipIndex.kt`
- `transit/octopus/src/commonMain/kotlin/.../OctopusTransitInfo.kt`
- `transit/smartrider/src/commonMain/kotlin/.../SmartRiderTransitInfo.kt`
- `transit/hsl/src/commonMain/kotlin/.../HSLTransitInfo.kt`
- `transit/charlie/src/commonMain/kotlin/.../CharlieCardTransitInfo.kt`
- `transit/nextfareul/src/commonMain/kotlin/.../NextfareUltralightTransitData.kt`
- `transit/calypso/src/commonMain/kotlin/.../LisboaVivaTransitInfo.kt`
- `transit/serialonly/src/commonMain/kotlin/.../StrelkaTransitInfo.kt`
- `transit/serialonly/src/commonMain/kotlin/.../HoloTransitInfo.kt`
- `transit/umarsh/src/commonMain/kotlin/.../UmarshTransitInfo.kt`
- `transit/troika/src/commonMain/kotlin/.../TroikaHybridTransitInfo.kt`

**Step 1: Convert OVChipIndex.addAdvancedItems**

Change from taking `FareBotUiTree.Item.Builder` to returning `List<FareBotUiTree.Item>`:

```kotlin
fun advancedItems(): List<FareBotUiTree.Item> = listOf(
    FareBotUiTree.Item(title = FormattedString("Transaction Slot"), value = if (recentTransactionSlot) "B" else "A"),
    FareBotUiTree.Item(title = FormattedString("Info Slot"), value = if (recentInfoSlot) "B" else "A"),
    FareBotUiTree.Item(title = FormattedString("Subscription Slot"), value = if (recentSubscriptionSlot) "B" else "A"),
    FareBotUiTree.Item(title = FormattedString("Travelhistory Slot"), value = if (recentTravelhistorySlot) "B" else "A"),
    FareBotUiTree.Item(title = FormattedString("Credit Slot"), value = if (recentCreditSlot) "B" else "A"),
)
```

**Step 2: Convert OVChipTransitInfo.getAdvancedUi()**

```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
    item { title = "Credit Slot ID"; value = creditSlotId.toString() }
    item { title = "Last Credit ID"; value = creditId.toString() }
    item {
        title = "Recent Slots"
        addChildren(index.advancedItems())
    }
}
```

**Step 3: Convert remaining transit files**

Each follows the same builder-to-DSL pattern. Specific conversions:

**OctopusTransitInfo.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree? {
    val szt = shenzhenBalance
    if (!hasOctopus || szt == null) return null
    return uiTree {
        item {
            title = Res.string.octopus_alternate_purse_balances
            item {
                title = Res.string.octopus_szt
                value = TransitCurrency.CNY(szt).formatCurrencyString(isBalance = true)
            }
        }
    }
}
```

**SmartRiderTransitInfo.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
    item { title = Res.string.smartrider_ticket_type; value = mTokenType.toString() }
    if (mSmartRiderType == SmartRiderType.SMARTRIDER) {
        item { title = Res.string.smartrider_autoload_threshold; value = TransitCurrency.AUD(mAutoloadThreshold).formatCurrencyString(true) }
        item { title = Res.string.smartrider_autoload_value; value = TransitCurrency.AUD(mAutoloadValue).formatCurrencyString(true) }
    }
}
```

**HSLTransitInfo.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree? {
    val tree = uiTree {
        applicationVersion?.let { item { title = Res.string.hsl_application_version; value = it } }
        applicationKeyVersion?.let { item { title = Res.string.hsl_application_key_version; value = it } }
        platformType?.let { item { title = Res.string.hsl_platform_type; value = it } }
        securityLevel?.let { item { title = Res.string.hsl_security_level; value = it } }
    }
    return if (tree.items.isEmpty()) null else tree
}
```

**CharlieCardTransitInfo.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree? {
    if (secondSerial == 0L || secondSerial == 0xffffffffL) return null
    return uiTree {
        item { title = Res.string.charlie_2nd_card_number; value = "A" + NumberUtils.zeroPad(secondSerial, 10) }
    }
}
```

**NextfareUltralightTransitData.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
    item { title = Res.string.nextfareul_machine_code; value = capsule.mMachineCode.toString(16) }
}
```

**LisboaVivaTransitInfo.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree? {
    if (tagId == null) return null
    return uiTree {
        item { title = Res.string.calypso_engraved_serial; value = tagId.toString() }
    }
}
```

**StrelkaTransitInfo.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
    item { title = Res.string.strelka_long_serial; value = mSerial }
}
```

**HoloTransitInfo.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
    item { title = Res.string.manufacture_id; value = mManufacturingId }
}
```

**UmarshTransitInfo.kt:**
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree? {
    val rubSectors = sectors.filter { it.denomination == UmarshDenomination.RUB }
    if (rubSectors.isEmpty()) return null
    return uiTree {
        for (sec in rubSectors) {
            item { title = Res.string.umarsh_machine_id; value = sec.machineId.toString() }
        }
    }
}
```

**TroikaHybridTransitInfo.kt** — flattens sub-trees:
```kotlin
override suspend fun getAdvancedUi(): FareBotUiTree? {
    val trees = listOfNotNull(
        troika.getAdvancedUi(),
        podorozhnik?.getAdvancedUi(),
        strelka?.getAdvancedUi(),
    )
    if (trees.isEmpty()) return null
    return FareBotUiTree(items = trees.flatMap { tree ->
        tree.items.map { FareBotUiTree.Item(title = it.title, value = it.value) }
    })
}
```

**Step 4: Commit**

```bash
git add transit/
git commit -m "refactor: convert transit getAdvancedUi() from builder to DSL"
```

---

## Task 6: Remove ClipperTrip.Builder

**Files:**
- Modify: `transit/clipper/src/commonMain/kotlin/.../ClipperTrip.kt`
- Modify: `transit/clipper/src/commonMain/kotlin/.../ClipperTransitFactory.kt`
- Modify: `app/src/commonTest/kotlin/.../ClipperTransitTest.kt`

**Step 1: Add default values to ClipperTrip constructor, remove Builder**

Add `= 0` defaults to all constructor parameters (vehicleNum and transportCode already have them). Remove the `Builder` class and `companion object`:

```kotlin
class ClipperTrip(
    private val timestamp: Long = 0,
    private val exitTimestampValue: Long = 0,
    private val balance: Long = 0,
    private val fareValue: Long = 0,
    private val agency: Long = 0,
    private val from: Long = 0,
    private val to: Long = 0,
    private val route: Long = 0,
    private val vehicleNum: Long = 0,
    private val transportCode: Long = 0,
) : Trip() {
    // ... all properties and methods stay the same ...
    // DELETE: companion object { fun builder() }
    // DELETE: class Builder { ... }
}
```

**Step 2: Update ClipperTransitFactory.createTrip()**

Replace:
```kotlin
return ClipperTrip
    .builder()
    .timestamp(timestamp)
    .exitTimestamp(exitTimestamp)
    // ...
    .build()
```
with:
```kotlin
return ClipperTrip(
    timestamp = timestamp,
    exitTimestampValue = exitTimestamp,
    balance = 0,
    fareValue = fare,
    agency = agency,
    from = from,
    to = to,
    route = route,
    vehicleNum = vehicleNum,
    transportCode = transportCode,
)
```

**Step 3: Update ClipperTransitTest.kt**

Replace all `ClipperTrip.builder().agency(x).transportCode(y).build()` with constructor calls:
```kotlin
// Before:
val trip = ClipperTrip.builder().agency(0x04).transportCode(0x6f).build()
// After:
val trip = ClipperTrip(agency = 0x04, transportCode = 0x6f)
```

Apply this to every test method (~15 call sites).

**Step 4: Commit**

```bash
git add transit/clipper/ app/src/commonTest/
git commit -m "refactor: remove ClipperTrip.Builder, use constructor with named params"
```

---

## Task 7: Remove SeqGoTrip.Builder

**Files:**
- Modify: `transit/seqgo/src/commonMain/kotlin/.../SeqGoTrip.kt`
- Modify: `transit/seqgo/src/commonMain/kotlin/.../SeqGoTransitFactory.kt`

**Step 1: Add default values to SeqGoTrip constructor, remove Builder**

```kotlin
class SeqGoTrip(
    private val journeyId: Int = 0,
    private val modeValue: Mode = Mode.OTHER,
    private val startTime: Instant? = null,
    private val endTime: Instant? = null,
    private val startStationId: Int = 0,
    private val endStationId: Int = 0,
    private val startStationValue: Station? = null,
    private val endStationValue: Station? = null,
) : Trip() {
    // ... all properties and methods stay the same ...
    // DELETE: class Builder { ... }
    // DELETE: companion object { fun builder() }
}
```

**Step 2: Update SeqGoTransitFactory**

Replace builder usage with direct construction. The tricky part: the builder pattern was used to conditionally add tap-off data. Use `var` locals + copy pattern, or build the trip in one expression:

```kotlin
val tapOn = sortedTaps[i]
var endTime: Instant? = null
var endStationId = 0
var endStation: Station? = null

if (sortedTaps.size > i + 1 &&
    sortedTaps[i + 1].journey == tapOn.journey &&
    sortedTaps[i + 1].mode == tapOn.mode
) {
    val tapOff = sortedTaps[i + 1]
    endTime = tapOff.timestamp
    endStationId = tapOff.station
    endStation = SeqGoUtil.getStation(tapOff.station)
    i++
}

trips.add(
    SeqGoTrip(
        journeyId = tapOn.journey,
        modeValue = tapOn.mode,
        startTime = tapOn.timestamp,
        endTime = endTime,
        startStationId = tapOn.station,
        endStationId = endStationId,
        startStationValue = SeqGoUtil.getStation(tapOn.station),
        endStationValue = endStation,
    )
)
```

**Step 3: Commit**

```bash
git add transit/seqgo/
git commit -m "refactor: remove SeqGoTrip.Builder, use constructor with named params"
```

---

## Task 8: Remove Station.Builder

**Files:**
- Modify: `transit/src/commonMain/kotlin/com/codebutler/farebot/transit/Station.kt`

**Step 1: Remove Builder class and companion factory method**

Delete the `Builder` class (lines 110-170) and the `builder()` method from the companion object. The `Station` data class already has default values for all constructor parameters. No call sites to update (0 usages).

**Step 2: Commit**

```bash
git add transit/src/commonMain/kotlin/com/codebutler/farebot/transit/Station.kt
git commit -m "refactor: remove unused Station.Builder"
```

---

## Task 9: Build and test

**Step 1: Run tests**

```bash
cd /workspace/.worktrees/remove-builders && ./gradlew allTests
```

Expected: All tests pass.

**Step 2: Run full build**

```bash
cd /workspace/.worktrees/remove-builders && ./gradlew assemble
```

Expected: Build succeeds.

**Step 3: Fix any compilation errors**

Common issues to watch for:
- Missing imports for `uiTree` or `FormattedString`
- `FareBotUiTree.builder()` references still lingering
- `FareBotUiTree.Item.Builder` type references in function signatures (OVChipIndex)

---

## Task Dependency Graph

```
Task 1 (FareBotUiTree data model)
  └─ Task 2 (UiTreeBuilder DSL)
       ├─ Task 3 (CardAdvancedScreen)
       ├─ Task 4 (card modules) ──┐
       └─ Task 5 (transit modules)┤
                                   ├─ Task 9 (build & test)
Task 6 (ClipperTrip.Builder) ─────┤
Task 7 (SeqGoTrip.Builder) ───────┤
Task 8 (Station.Builder) ─────────┘
```

Tasks 6, 7, 8 are independent of tasks 1-5 and can run in parallel.
Tasks 4 and 5 can run in parallel after tasks 1-3 are complete.
