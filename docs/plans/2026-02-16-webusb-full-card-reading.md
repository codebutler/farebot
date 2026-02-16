# Full Card Reading over WebUSB — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enable full NFC card data reading (not just detection) over WebUSB in the Kotlin/WasmJs web target.

**Architecture:** The core problem is a sync/async mismatch: all NFC technology interfaces (`CardTransceiver.transceive()`, `ClassicTechnology.readBlock()`, etc.) are synchronous, but WebUSB is Promise-based and Kotlin/WasmJs cannot block on Promises. The solution is to make these interfaces `suspend`-compatible. This is the architecturally correct approach — NFC I/O is inherently async, and all platforms already call card readers from async contexts (coroutines, threads, GCD queues). Adding `suspend` to non-suspending implementations has zero runtime overhead.

**Tech Stack:** Kotlin Multiplatform, Kotlin Coroutines, WebUSB (JS interop), PN533 NFC protocol

---

## Architecture Overview

The call chain from card detection to data is:

```
WebCardScanner.pollLoop() [suspend, web-specific]
  → Card readers: DesfireCardReader, ClassicCardReader, UltralightCardReader, FeliCaReader, etc.
    → Technology interfaces: CardTransceiver.transceive(), ClassicTechnology.readBlock(), etc.
      → PN533 controller: PN533.inDataExchange(), PN533.inCommunicateThru()
        → Transport: PN533Transport.sendCommand()
          → WebUsbPN533Transport: currently throws "cannot call synchronously"
```

We make every layer in this chain `suspend`, bottom-up. Existing sync implementations (Android, iOS, Desktop) simply add the `suspend` keyword with no behavior change. WebUSB implementations delegate to their existing async methods.

**Files NOT changed:** Transit system implementations (`transit/*/`), UI code, ViewModels, persistence, MDST lookups. The change is entirely within the NFC I/O pipeline.

**Vicinity (NFC-V/ISO 15693)** is excluded from WebUSB card reading because PN533 readers don't support ISO 15693. The `VicinityTechnology` interface still gets `suspend` for consistency, but no web implementation is created.

---

### Task 1: Make PN533Transport and PN533 suspend-compatible

This is the foundation. Make the transport interface and PN533 controller suspend-compatible, and implement WebUsbPN533Transport.sendCommand() properly.

**Files:**
- Modify: `card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/pn533/PN533Transport.kt`
- Modify: `card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/pn533/PN533.kt`
- Modify: `card/src/wasmJsMain/kotlin/com/codebutler/farebot/card/nfc/pn533/WebUsbPN533Transport.kt`
- Modify: `card/src/jvmMain/kotlin/com/codebutler/farebot/card/nfc/pn533/Usb4JavaPN533Transport.kt` (add `suspend` keyword only)

**Step 1: Make PN533Transport interface suspend**

In `card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/pn533/PN533Transport.kt`, change:

```kotlin
interface PN533Transport {
    suspend fun sendCommand(
        code: Byte,
        data: ByteArray = byteArrayOf(),
        timeoutMs: Int = 5000,
    ): ByteArray

    suspend fun sendAck()

    fun flush()

    fun close()
}
```

`flush()` and `close()` stay non-suspend (they're setup/teardown, not I/O exchange).

**Step 2: Make PN533 class methods suspend**

In `card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/pn533/PN533.kt`, add `suspend` to every method that calls `transport.sendCommand()` or `transport.sendAck()`:

- `getFirmwareVersion()` → `suspend fun getFirmwareVersion()`
- `samConfiguration()` → `suspend fun samConfiguration()`
- `setParameters()` → `suspend fun setParameters()`
- `resetMode()` → `suspend fun resetMode()`
- `writeRegister()` → `suspend fun writeRegister()`
- `rfConfiguration()` → `suspend fun rfConfiguration()`
- `setMaxRetries()` → `suspend fun setMaxRetries()`
- `rfFieldOff()` → `suspend fun rfFieldOff()`
- `rfFieldOn()` → `suspend fun rfFieldOn()`
- `inListPassiveTarget()` → `suspend fun inListPassiveTarget()`
- `inDataExchange()` → `suspend fun inDataExchange()`
- `inCommunicateThru()` → `suspend fun inCommunicateThru()`
- `inRelease()` → `suspend fun inRelease()`
- `sendAck()` → `suspend fun sendAck()`

`close()` stays non-suspend.

**Step 3: Implement WebUsbPN533Transport.sendCommand() properly**

In `card/src/wasmJsMain/kotlin/com/codebutler/farebot/card/nfc/pn533/WebUsbPN533Transport.kt`, change `sendCommand()` from throwing an error to delegating to `sendCommandAsync()`:

```kotlin
override suspend fun sendCommand(
    code: Byte,
    data: ByteArray,
    timeoutMs: Int,
): ByteArray = sendCommandAsync(code, data, timeoutMs)

override suspend fun sendAck() = sendAckAsync()
```

**Step 4: Add suspend to Usb4JavaPN533Transport**

In the JVM transport implementation, just add `suspend` keyword to `sendCommand()` and `sendAck()`. No logic change.

**Step 5: Build the card module to verify**

Run: `./gradlew :card:compileCommonMainKotlinMetadata`

Expected: Compilation errors in callers of PN533 that haven't been updated yet (technology implementations). This is expected — we fix those in Task 3.

**Step 6: Commit**

```bash
git add card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/pn533/PN533Transport.kt \
       card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/pn533/PN533.kt \
       card/src/wasmJsMain/kotlin/com/codebutler/farebot/card/nfc/pn533/WebUsbPN533Transport.kt \
       card/src/jvmMain/kotlin/com/codebutler/farebot/card/nfc/pn533/Usb4JavaPN533Transport.kt
git commit -m "refactor: make PN533Transport and PN533 suspend-compatible

Adds suspend to PN533Transport.sendCommand() and sendAck(), and
propagates through all PN533 controller methods. WebUsbPN533Transport
now delegates sendCommand() to sendCommandAsync() instead of throwing."
```

---

### Task 2: Make NFC technology interfaces suspend-compatible

Add `suspend` to the NFC technology interface methods that perform I/O. `NfcTechnology.connect()` and `close()` stay non-suspend.

**Files:**
- Modify: `card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/CardTransceiver.kt`
- Modify: `card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/ClassicTechnology.kt`
- Modify: `card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/UltralightTechnology.kt`
- Modify: `card/felica/src/commonMain/kotlin/com/codebutler/farebot/card/felica/FeliCaTagAdapter.kt`
- Modify: `card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/VicinityTechnology.kt`

**Step 1: Update CardTransceiver**

```kotlin
interface CardTransceiver : NfcTechnology {
    suspend fun transceive(data: ByteArray): ByteArray

    val maxTransceiveLength: Int
}
```

**Step 2: Update ClassicTechnology**

```kotlin
interface ClassicTechnology : NfcTechnology {
    val sectorCount: Int

    suspend fun authenticateSectorWithKeyA(sectorIndex: Int, key: ByteArray): Boolean

    suspend fun authenticateSectorWithKeyB(sectorIndex: Int, key: ByteArray): Boolean

    suspend fun readBlock(blockIndex: Int): ByteArray

    fun sectorToBlock(sectorIndex: Int): Int

    fun getBlockCountInSector(sectorIndex: Int): Int
    // ... companion object unchanged
}
```

`sectorToBlock()` and `getBlockCountInSector()` are pure computation — no suspend needed.

**Step 3: Update UltralightTechnology**

```kotlin
interface UltralightTechnology : NfcTechnology {
    val type: Int

    suspend fun readPages(pageOffset: Int): ByteArray

    suspend fun transceive(data: ByteArray): ByteArray

    fun reconnect() { /* default no-op */ }
}
```

**Step 4: Update FeliCaTagAdapter**

```kotlin
interface FeliCaTagAdapter {
    fun getIDm(): ByteArray  // stays non-suspend (returns cached value)

    suspend fun getSystemCodes(): List<Int>

    suspend fun selectSystem(systemCode: Int): ByteArray?

    suspend fun getServiceCodes(): List<Int>

    suspend fun readBlock(serviceCode: Int, blockAddr: Byte): ByteArray?
}
```

**Step 5: Update VicinityTechnology**

```kotlin
interface VicinityTechnology : NfcTechnology {
    val uid: ByteArray

    suspend fun transceive(data: ByteArray): ByteArray
}
```

**Step 6: Commit**

```bash
git add card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/CardTransceiver.kt \
       card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/ClassicTechnology.kt \
       card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/UltralightTechnology.kt \
       card/felica/src/commonMain/kotlin/com/codebutler/farebot/card/felica/FeliCaTagAdapter.kt \
       card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/VicinityTechnology.kt
git commit -m "refactor: make NFC technology interfaces suspend-compatible

CardTransceiver.transceive(), ClassicTechnology.readBlock()/auth,
UltralightTechnology.readPages()/transceive(), FeliCaTagAdapter I/O
methods, and VicinityTechnology.transceive() are now suspend functions."
```

---

### Task 3: Update all technology implementations

Add `suspend` keyword to all implementations of the interfaces changed in Task 2. No logic changes — purely mechanical.

**Files:**
- Modify: `card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/pn533/PN533CardTransceiver.kt`
- Modify: `card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/pn533/PN533CommunicateThruTransceiver.kt`
- Modify: `card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/pn533/PN533ClassicTechnology.kt`
- Modify: `card/src/commonMain/kotlin/com/codebutler/farebot/card/nfc/pn533/PN533UltralightTechnology.kt`
- Modify: `card/src/jvmMain/kotlin/com/codebutler/farebot/card/nfc/PCSCCardTransceiver.kt`
- Modify: JVM PCSC Classic/Ultralight technology implementations (find with `grep -r "ClassicTechnology\|UltralightTechnology" card/src/jvmMain/`)
- Modify: `card/src/androidMain/kotlin/com/codebutler/farebot/card/nfc/AndroidCardTransceiver.kt`
- Modify: `card/src/androidMain/kotlin/com/codebutler/farebot/card/nfc/AndroidUltralightTechnology.kt`
- Modify: `card/src/androidMain/kotlin/com/codebutler/farebot/card/nfc/AndroidVicinityTechnology.kt`
- Modify: `card/felica/src/androidMain/kotlin/com/codebutler/farebot/card/felica/AndroidFeliCaTagAdapter.kt`
- Modify: `card/src/iosMain/kotlin/com/codebutler/farebot/card/nfc/IosCardTransceiver.kt`
- Modify: iOS Ultralight/Vicinity technology implementations
- Modify: `card/felica/src/iosMain/kotlin/com/codebutler/farebot/card/felica/IosFeliCaTagAdapter.kt`
- Modify: JVM PCSC FeliCa adapter (`card/felica/src/jvmMain/`)

**Step 1: Update PN533 technology implementations**

For each file, add `suspend` to the overriding methods. Example for `PN533CardTransceiver`:

```kotlin
override suspend fun transceive(data: ByteArray): ByteArray = pn533.inDataExchange(tg, data)
```

For `PN533CommunicateThruTransceiver`:

```kotlin
override suspend fun transceive(data: ByteArray): ByteArray {
    // ... existing logic unchanged, just add suspend
}
```

For `PN533ClassicTechnology`:

```kotlin
override suspend fun authenticateSectorWithKeyA(sectorIndex: Int, key: ByteArray): Boolean =
    authenticate(sectorIndex, key, MIFARE_CMD_AUTH_A)

override suspend fun authenticateSectorWithKeyB(sectorIndex: Int, key: ByteArray): Boolean =
    authenticate(sectorIndex, key, MIFARE_CMD_AUTH_B)

override suspend fun readBlock(blockIndex: Int): ByteArray = ...

private suspend fun authenticate(...): Boolean = ...  // calls pn533.inDataExchange
```

For `PN533UltralightTechnology`:

```kotlin
override suspend fun readPages(pageOffset: Int): ByteArray = ...
override suspend fun transceive(data: ByteArray): ByteArray = ...
```

**Step 2: Update platform technology implementations (Android, iOS, JVM/PCSC)**

Same mechanical change: add `suspend` to each overriding method. Use the compiler to find every implementation that needs updating:

```bash
./gradlew :card:compileCommonMainKotlinMetadata 2>&1 | grep "error:"
```

Fix each error by adding `suspend`.

**Step 3: Commit**

```bash
git add -A card/src/ card/felica/src/
git commit -m "refactor: add suspend to all NFC technology implementations

Mechanical change: adds suspend keyword to all implementations of
CardTransceiver, ClassicTechnology, UltralightTechnology,
FeliCaTagAdapter, and VicinityTechnology across PN533, PCSC,
Android, and iOS source sets."
```

---

### Task 4: Move PN533FeliCaTagAdapter to commonMain

Currently in `card/felica/src/jvmMain/`. Needed in commonMain for web to use it. The class has no JVM-specific dependencies — it only uses `PN533` (commonMain) and `FeliCaTagAdapter` (commonMain).

**Files:**
- Move: `card/felica/src/jvmMain/kotlin/com/codebutler/farebot/card/felica/PN533FeliCaTagAdapter.kt` → `card/felica/src/commonMain/kotlin/com/codebutler/farebot/card/felica/PN533FeliCaTagAdapter.kt`

**Step 1: Move the file**

```bash
mkdir -p card/felica/src/commonMain/kotlin/com/codebutler/farebot/card/felica/
mv card/felica/src/jvmMain/kotlin/com/codebutler/farebot/card/felica/PN533FeliCaTagAdapter.kt \
   card/felica/src/commonMain/kotlin/com/codebutler/farebot/card/felica/PN533FeliCaTagAdapter.kt
```

**Step 2: Add suspend to its methods**

Update `PN533FeliCaTagAdapter` methods to match the now-suspend `FeliCaTagAdapter` interface:

```kotlin
override suspend fun getSystemCodes(): List<Int> { ... }
override suspend fun selectSystem(systemCode: Int): ByteArray? { ... }
override suspend fun getServiceCodes(): List<Int> { ... }
override suspend fun readBlock(serviceCode: Int, blockAddr: Byte): ByteArray? { ... }
```

Also update private helpers that call `pn533.inCommunicateThru()`:

```kotlin
private suspend fun transceiveFelica(felicaFrame: ByteArray): ByteArray? = ...
private suspend fun polling(systemCode: Int): ByteArray? = ...
```

**Step 3: Verify the felica module compiles**

Run: `./gradlew :card:felica:compileCommonMainKotlinMetadata`

Expected: Success (or errors in callers not yet updated).

**Step 4: Commit**

```bash
git add card/felica/src/
git commit -m "refactor: move PN533FeliCaTagAdapter to commonMain

No JVM-specific dependencies — now available for all platforms
including wasmJs/web. Methods updated to suspend per FeliCaTagAdapter
interface changes."
```

---

### Task 5: Make protocol classes suspend-compatible

Protocol classes sit between card readers and technology interfaces. They call `transceiver.transceive()` which is now suspend, so they must become suspend too.

**Files:**
- Modify: `card/desfire/src/commonMain/kotlin/com/codebutler/farebot/card/desfire/DesfireProtocol.kt`
- Modify: `card/iso7816/src/commonMain/kotlin/com/codebutler/farebot/card/iso7816/ISO7816Protocol.kt`
- Modify: `card/cepas/src/commonMain/kotlin/com/codebutler/farebot/card/cepas/CEPASProtocol.kt`
- Modify: `card/ultralight/src/commonMain/kotlin/com/codebutler/farebot/card/ultralight/UltralightProtocol.kt` (if it exists)

**Step 1: Update DesfireProtocol**

Every method that calls `mTransceiver.transceive()` becomes suspend. This includes:

- `getManufacturingData()` → `suspend fun getManufacturingData()`
- `getAppList()` → `suspend fun getAppList()`
- `selectApp()` → `suspend fun selectApp()`
- `getFileList()` → `suspend fun getFileList()`
- `getFileSettings()` → `suspend fun getFileSettings()`
- `readFile()` → `suspend fun readFile()`
- `readRecord()` → `suspend fun readRecord()`
- `getValue()` → `suspend fun getValue()`
- All `sendRequest()` overloads → `private suspend fun sendRequest()`

**Step 2: Update ISO7816Protocol**

All methods that call `transceiver.transceive()`:

- `selectByName()` → `suspend fun selectByName()`
- `selectById()` → `suspend fun selectById()`
- `readBinary()` → `suspend fun readBinary()`
- `readRecord()` → `suspend fun readRecord()`
- `getBalance()` → `suspend fun getBalance()`
- All internal helper methods

**Step 3: Update CEPASProtocol**

- `getPurse()` → `suspend fun getPurse()`
- `getHistory()` → `suspend fun getHistory()`
- Any internal methods

**Step 4: Update UltralightProtocol (if exists)**

Check for a protocol class in `card/ultralight/`. If it calls `tech.readPages()` or `tech.transceive()`, make those calls suspend.

**Step 5: Build to verify**

Run: `./gradlew :card:desfire:compileCommonMainKotlinMetadata :card:iso7816:compileCommonMainKotlinMetadata :card:cepas:compileCommonMainKotlinMetadata`

Expected: Errors in card readers (not yet updated). Protocol classes should compile.

**Step 6: Commit**

```bash
git add card/desfire/src/ card/iso7816/src/ card/cepas/src/ card/ultralight/src/
git commit -m "refactor: make protocol classes suspend-compatible

DesfireProtocol, ISO7816Protocol, CEPASProtocol, and UltralightProtocol
now use suspend functions for NFC I/O operations."
```

---

### Task 6: Make card readers and ISO7816Dispatcher suspend-compatible

Card readers call protocol classes (now suspend) and technology interfaces (now suspend). Make all their methods suspend.

**Files:**
- Modify: `card/desfire/src/commonMain/kotlin/com/codebutler/farebot/card/desfire/DesfireCardReader.kt`
- Modify: `card/classic/src/commonMain/kotlin/com/codebutler/farebot/card/classic/ClassicCardReader.kt`
- Modify: `card/ultralight/src/commonMain/kotlin/com/codebutler/farebot/card/ultralight/UltralightCardReader.kt`
- Modify: `card/felica/src/commonMain/kotlin/com/codebutler/farebot/card/felica/FeliCaReader.kt`
- Modify: `card/cepas/src/commonMain/kotlin/com/codebutler/farebot/card/cepas/CEPASCardReader.kt`
- Modify: `card/iso7816/src/commonMain/kotlin/com/codebutler/farebot/card/iso7816/ISO7816CardReader.kt`
- Modify: `card/vicinity/src/commonMain/kotlin/com/codebutler/farebot/card/vicinity/VicinityCardReader.kt`
- Modify: `app/src/commonMain/kotlin/com/codebutler/farebot/shared/nfc/ISO7816Dispatcher.kt`

**Step 1: Update DesfireCardReader**

```kotlin
object DesfireCardReader {
    suspend fun readCard(tagId: ByteArray, tech: CardTransceiver): RawDesfireCard { ... }
    private suspend fun readApplications(...): List<RawDesfireApplication> { ... }
    private suspend fun readFiles(...): Pair<List<RawDesfireFile>, Boolean> { ... }
    private suspend fun readFile(...): RawDesfireFile { ... }
    private suspend fun tryReadFileWithoutSettings(...): RawDesfireFile { ... }
    private suspend fun readFileData(...): ByteArray { ... }
}
```

**Step 2: Update ClassicCardReader**

```kotlin
object ClassicCardReader {
    suspend fun readCard(tagId: ByteArray, tech: ClassicTechnology, cardKeys: ClassicCardKeys?): RawClassicCard { ... }
    // All private helper methods that call tech.authenticateSectorWithKeyA/B() or tech.readBlock()
}
```

**Step 3: Update UltralightCardReader**

```kotlin
object UltralightCardReader {
    suspend fun readCard(tagId: ByteArray, tech: UltralightTechnology): RawUltralightCard { ... }
    // All private helpers
}
```

**Step 4: Update FeliCaReader**

```kotlin
object FeliCaReader {
    suspend fun readTag(tagId: ByteArray, adapter: FeliCaTagAdapter, onlyFirst: Boolean = false): RawFelicaCard { ... }
    // All private helpers
}
```

**Step 5: Update CEPASCardReader**

```kotlin
object CEPASCardReader {
    suspend fun readCard(tagId: ByteArray, tech: CardTransceiver): RawCEPASCard { ... }
}
```

**Step 6: Update ISO7816CardReader**

```kotlin
object ISO7816CardReader {
    suspend fun readCard(tagId: ByteArray, transceiver: CardTransceiver, appConfigs: List<AppConfig>): RawISO7816Card { ... }
    // All internal methods
    // AppConfig lambdas (readBalances, readExtraData) must also become suspend lambdas:
    //   readBalances: suspend (ISO7816Protocol) -> Map<Int, ByteArray>
    //   readExtraData: suspend (ISO7816Protocol) -> Map<String, ByteArray>
}
```

**Step 7: Update VicinityCardReader**

```kotlin
object VicinityCardReader {
    suspend fun readCard(tagId: ByteArray, tech: VicinityTechnology): RawVicinityCard { ... }
}
```

**Step 8: Update ISO7816Dispatcher**

```kotlin
object ISO7816Dispatcher {
    suspend fun readCard(tagId: ByteArray, transceiver: CardTransceiver): RawCard<*> { ... }
    private suspend fun tryISO7816(...): RawCard<*>? { ... }
    // Update buildAppConfigs() — the lambdas need to be suspend
}
```

The `buildAppConfigs()` method returns `AppConfig` objects with lambda fields. If `readBalances` and `readExtraData` are called from suspend context, their function types need to become suspend:

```kotlin
data class AppConfig(
    val appNames: List<ByteArray>,
    val type: String,
    val readBalances: (suspend (ISO7816Protocol) -> Map<Int, ByteArray>)? = null,
    val readExtraData: (suspend (ISO7816Protocol) -> Map<String, ByteArray>)? = null,
    val fileSelectors: List<FileSelector> = emptyList(),
)
```

**Step 9: Build all card modules**

Run: `./gradlew compileCommonMainKotlinMetadata`

Expected: Errors in platform call sites (Desktop, Android, iOS). Card modules should compile.

**Step 10: Commit**

```bash
git add card/ app/src/commonMain/
git commit -m "refactor: make card readers and ISO7816Dispatcher suspend-compatible

All card reader public APIs (readCard/readTag) are now suspend functions.
ISO7816CardReader.AppConfig lambdas updated to suspend function types."
```

---

### Task 7: Update platform call sites

Each platform calls card readers from different contexts. Update each to work with the now-suspend card reader APIs.

**Files:**
- Modify: `app/desktop/src/jvmMain/kotlin/com/codebutler/farebot/desktop/PN53xReaderBackend.kt`
- Modify: `app/desktop/src/jvmMain/kotlin/com/codebutler/farebot/desktop/PcscReaderBackend.kt`
- Modify: `app/desktop/src/jvmMain/kotlin/com/codebutler/farebot/desktop/RCS956ReaderBackend.kt` (if it calls card readers)
- Modify: `app/src/androidMain/kotlin/com/codebutler/farebot/app/feature/home/AndroidCardScanner.kt`
- Modify: Android tag reader classes (if they call card readers directly)
- Modify: `app/src/iosMain/kotlin/com/codebutler/farebot/shared/nfc/IosNfcScanner.kt`

**Step 1: Update Desktop — PN53xReaderBackend**

The desktop poll loop runs on a dedicated thread. Wrap card reading calls in `runBlocking`:

```kotlin
import kotlinx.coroutines.runBlocking

// In pollLoop(), change the readTarget call:
try {
    val rawCard = runBlocking { readTarget(pn533, target) }
    onCardRead(rawCard)
} catch (e: Exception) { ... }

// Make readTarget suspend:
private suspend fun readTarget(pn533: PN533, target: PN533.TargetInfo): RawCard<*> = ...

// readTypeACard, readFeliCaCard become suspend
private suspend fun readTypeACard(...): RawCard<*> { ... }
private suspend fun readFeliCaCard(...): RawCard<*> { ... }
```

Also update `initDevice(pn533)` call — if it calls PN533 methods (which are now suspend), wrap in `runBlocking`:

```kotlin
// In scanLoop():
runBlocking { initDevice(pn533) }
```

Or make `initDevice` suspend and wrap the whole poll block.

The simplest approach: make `pollLoop` a suspend function and wrap the entire `scanLoop` body in `runBlocking`:

```kotlin
override fun scanLoop(...) {
    val transport = ...
    transport.flush()
    val pn533 = PN533(transport)
    try {
        runBlocking {
            initDevice(pn533)
            pollLoop(pn533, onCardDetected, onCardRead, onError)
        }
    } finally {
        pn533.close()
    }
}

private suspend fun pollLoop(...) {
    while (true) {
        // ... existing logic, now suspend-compatible
        // Replace Thread.sleep with delay:
        delay(POLL_INTERVAL_MS)
    }
}
```

**Step 2: Update Desktop — PcscReaderBackend**

Similar approach — wrap card reading in `runBlocking`. The PCSC backend also runs on a thread, so `runBlocking` is safe.

**Step 3: Update Desktop — RCS956ReaderBackend and PN533ReaderBackend**

Check if these are subclasses of `PN53xReaderBackend`. If so, they may just need `initDevice()` to become suspend:

```kotlin
override suspend fun initDevice(pn533: PN533) { ... }
```

**Step 4: Update Android — AndroidCardScanner**

Android already runs in a coroutine (`scope.launch { ... }`). The tag reader factory call needs to be suspend. Check `tagReaderFactory.getTagReader(tag.id, tag, cardKeys).readTag()`:

- If `readTag()` calls card readers, it needs to be suspend
- Trace through to find all Android tag reader classes

The key Android tag readers to check:
- `card/desfire/src/androidMain/kotlin/com/codebutler/farebot/card/desfire/DesfireTagReader.kt`
- `card/cepas/src/androidMain/kotlin/com/codebutler/farebot/card/cepas/CEPASTagReader.kt`
- `app/src/androidMain/kotlin/com/codebutler/farebot/app/core/nfc/ISO7816TagReader.kt`

Make their `readTag()` methods suspend. Since `AndroidCardScanner` already calls from a coroutine, this should be straightforward.

**Step 5: Update iOS — IosNfcScanner**

iOS runs card reading on a GCD worker queue using `dispatch_async(workerQueue)`. The reading happens synchronously on that queue. Use `runBlocking` to bridge:

```kotlin
private fun readTag(tag: Any): RawCard<*> = runBlocking {
    when (tag) {
        is NFCFeliCaTagProtocol -> readFelicaTag(tag)
        is NFCMiFareTagProtocol -> readMiFareTag(tag)
        is NFCISO15693TagProtocol -> readVicinityTag(tag)
        else -> throw Exception("Unsupported NFC tag type")
    }
}

private suspend fun readFelicaTag(tag: NFCFeliCaTagProtocol): RawCard<*> { ... }
private suspend fun readMiFareTag(tag: NFCMiFareTagProtocol): RawCard<*> { ... }
private suspend fun readVicinityTag(tag: NFCISO15693TagProtocol): RawCard<*> { ... }
```

**Step 6: Build all platforms**

Run: `./gradlew compileCommonMainKotlinMetadata`

Then try platform-specific builds:
- `./gradlew :app:android:assembleDebug` (may not work in devcontainer)
- `./gradlew :app:desktop:compileKotlinJvm`

Fix any remaining compilation errors.

**Step 7: Commit**

```bash
git add app/
git commit -m "refactor: update platform call sites for suspend card readers

Desktop: wrap card reading in runBlocking on poll thread.
Android: propagate suspend through tag reader chain.
iOS: use runBlocking on GCD worker queue."
```

---

### Task 8: Update tests

Tests use mock implementations of the NFC technology interfaces. These mocks need `suspend` on their overriding methods. Test functions that call card readers need to use `runTest`.

**Files:**
- Modify: `card/desfire/src/commonTest/kotlin/com/codebutler/farebot/card/desfire/DesfireProtocolTest.kt`
- Modify: `card/classic/src/commonTest/kotlin/com/codebutler/farebot/card/classic/ClassicCardReaderTest.kt`
- Modify: `card/ultralight/src/commonTest/kotlin/com/codebutler/farebot/card/ultralight/UltralightCardReaderTest.kt`
- Modify: `card/felica/src/commonTest/kotlin/com/codebutler/farebot/card/felica/FeliCaReaderTest.kt`
- Modify: `card/iso7816/src/commonTest/kotlin/com/codebutler/farebot/card/iso7816/ISO7816ProtocolTest.kt`
- Modify: `card/iso7816/src/commonTest/kotlin/com/codebutler/farebot/card/iso7816/ISO7816CardReaderTest.kt`
- Modify: `card/vicinity/src/commonTest/kotlin/com/codebutler/farebot/card/vicinity/VicinityCardReaderTest.kt`

**Step 1: Update mock implementations**

In each test file, find mock classes that implement `CardTransceiver`, `ClassicTechnology`, etc. and add `suspend` to their overriding methods:

```kotlin
// Example: DesfireProtocolTest.MockTransceiver
private class MockTransceiver : CardTransceiver {
    override suspend fun transceive(data: ByteArray): ByteArray = responses.removeFirst()
    // ...
}
```

**Step 2: Wrap test functions in runTest**

Tests that call suspend card readers need `runTest`:

```kotlin
import kotlinx.coroutines.test.runTest

@Test
fun testReadCard() = runTest {
    val result = DesfireCardReader.readCard(tagId, mockTransceiver)
    // assertions...
}
```

If `kotlinx-coroutines-test` is not already a test dependency, add it:

```kotlin
// In relevant build.gradle.kts files:
commonTest {
    dependencies {
        implementation(kotlin("test"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    }
}
```

**Step 3: Run all tests**

Run: `./gradlew allTests`

Expected: All existing tests pass (behavior unchanged, only API surface changed to suspend).

**Step 4: Commit**

```bash
git add card/
git commit -m "test: update test mocks and assertions for suspend interfaces

Mock NFC technology implementations now use suspend. Test functions
that call card readers wrapped in runTest."
```

---

### Task 9: Implement full card reading in WebCardScanner

This is the payoff — replace the "card reading not supported" error with actual card reading logic.

**Files:**
- Modify: `app/web/src/wasmJsMain/kotlin/com/codebutler/farebot/web/WebCardScanner.kt`

**Step 1: Add imports for card readers and technology adapters**

```kotlin
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.cepas.CEPASCardReader
import com.codebutler.farebot.card.classic.ClassicCardReader
import com.codebutler.farebot.card.felica.FeliCaReader
import com.codebutler.farebot.card.felica.PN533FeliCaTagAdapter
import com.codebutler.farebot.card.nfc.pn533.PN533
import com.codebutler.farebot.card.nfc.pn533.PN533CardInfo
import com.codebutler.farebot.card.nfc.pn533.PN533CardTransceiver
import com.codebutler.farebot.card.nfc.pn533.PN533ClassicTechnology
import com.codebutler.farebot.card.nfc.pn533.PN533CommunicateThruTransceiver
import com.codebutler.farebot.card.nfc.pn533.PN533UltralightTechnology
import com.codebutler.farebot.card.ultralight.UltralightCardReader
import com.codebutler.farebot.shared.nfc.ISO7816Dispatcher
```

**Step 2: Create a WebPN533 wrapper**

The `PN533` class takes a `PN533Transport`. Since `WebUsbPN533Transport` now properly implements the suspend `sendCommand()`, we can use `PN533(transport)` directly. However, the initialization in `pollLoop()` currently calls `transport.sendCommandAsync()` directly. Refactor to use `PN533`:

```kotlin
private suspend fun pollLoop(transport: WebUsbPN533Transport) {
    val pn533 = PN533(transport)

    // Initialize PN533
    pn533.sendAck()
    val fw = pn533.getFirmwareVersion()
    println("[WebUSB] PN53x firmware: $fw")
    pn533.samConfiguration()
    pn533.setMaxRetries()

    while (true) {
        var target = pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A)

        if (target == null) {
            target = pn533.inListPassiveTarget(
                baudRate = PN533.BAUD_RATE_212_FELICA,
                initiatorData = SENSF_REQ,
            )
        }

        if (target == null) {
            delay(POLL_INTERVAL_MS)
            continue
        }

        val tagId = when (target) {
            is PN533.TargetInfo.TypeA -> target.uid
            is PN533.TargetInfo.FeliCa -> target.idm
        }
        val cardTypeName = when (target) {
            is PN533.TargetInfo.TypeA -> PN533CardInfo.fromTypeA(target).cardType.name
            is PN533.TargetInfo.FeliCa -> CardType.FeliCa.name
        }

        _scannedTags.tryEmit(ScannedTag(id = tagId, techList = listOf(cardTypeName)))

        // Full card reading!
        try {
            val rawCard = readTarget(pn533, target)
            _scannedCards.tryEmit(rawCard)
        } catch (e: Exception) {
            println("[WebUSB] Read error: ${e.message}")
            _scanErrors.tryEmit(e)
        }

        // Release target
        try {
            pn533.inRelease(target.tg)
        } catch (_: PN533Exception) {}

        // Wait for card removal
        waitForRemoval(pn533)
    }
}
```

**Step 3: Implement readTarget**

Mirror the desktop `PN53xReaderBackend.readTarget()` logic:

```kotlin
private suspend fun readTarget(
    pn533: PN533,
    target: PN533.TargetInfo,
): RawCard<*> = when (target) {
    is PN533.TargetInfo.TypeA -> readTypeACard(pn533, target)
    is PN533.TargetInfo.FeliCa -> readFeliCaCard(pn533, target)
}

private suspend fun readTypeACard(
    pn533: PN533,
    target: PN533.TargetInfo.TypeA,
): RawCard<*> {
    val info = PN533CardInfo.fromTypeA(target)
    val tagId = target.uid
    println("[WebUSB] Type A card: type=${info.cardType}, SAK=0x${(target.sak.toInt() and 0xFF).toString(16).padStart(2, '0')}")

    return when (info.cardType) {
        CardType.MifareDesfire, CardType.ISO7816 -> {
            val transceiver = PN533CardTransceiver(pn533, target.tg)
            ISO7816Dispatcher.readCard(tagId, transceiver)
        }

        CardType.MifareClassic -> {
            val tech = PN533ClassicTechnology(pn533, target.tg, tagId, info)
            ClassicCardReader.readCard(tagId, tech, null)
        }

        CardType.MifareUltralight -> {
            val tech = PN533UltralightTechnology(pn533, target.tg, info)
            UltralightCardReader.readCard(tagId, tech)
        }

        CardType.CEPAS -> {
            val transceiver = PN533CardTransceiver(pn533, target.tg)
            CEPASCardReader.readCard(tagId, transceiver)
        }

        else -> {
            val transceiver = PN533CardTransceiver(pn533, target.tg)
            ISO7816Dispatcher.readCard(tagId, transceiver)
        }
    }
}

private suspend fun readFeliCaCard(
    pn533: PN533,
    target: PN533.TargetInfo.FeliCa,
): RawCard<*> {
    val tagId = target.idm
    println("[WebUSB] FeliCa card: IDm=${tagId.hex()}")
    val adapter = PN533FeliCaTagAdapter(pn533, tagId)
    return FeliCaReader.readTag(tagId, adapter)
}
```

**Step 4: Update waitForRemoval to use PN533**

```kotlin
private suspend fun waitForRemoval(pn533: PN533) {
    while (true) {
        delay(REMOVAL_POLL_INTERVAL_MS)
        val target = try {
            pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A)
                ?: pn533.inListPassiveTarget(
                    baudRate = PN533.BAUD_RATE_212_FELICA,
                    initiatorData = SENSF_REQ,
                )
        } catch (_: PN533Exception) {
            null
        }
        if (target == null) break
        try {
            pn533.inRelease(target.tg)
        } catch (_: PN533Exception) {}
    }
}
```

**Step 5: Remove the now-unnecessary duplicate helper methods**

Remove `inListPassiveTarget()`, `parseTypeATarget()`, `parseFeliCaTarget()` from WebCardScanner since we now use `PN533.inListPassiveTarget()` directly (which already has these methods).

**Step 6: Remove the "not yet supported" error**

Delete the code block that emits `UnsupportedOperationException("Detected ... card reading over WebUSB is in development...")`.

**Step 7: Commit**

```bash
git add app/web/src/wasmJsMain/kotlin/com/codebutler/farebot/web/WebCardScanner.kt
git commit -m "feat(web): implement full card reading over WebUSB

Replaces the detection-only stub with complete card reading for all
card types supported by PN533 USB readers: DESFire, MIFARE Classic,
MIFARE Ultralight, FeliCa, CEPAS, and ISO 7816.

Uses the same card reader pipeline as desktop/Android/iOS, now
possible because all NFC I/O interfaces are suspend-compatible."
```

---

### Task 10: Build verification and cleanup

**Step 1: Run all tests**

Run: `./gradlew allTests`

Expected: All tests pass.

**Step 2: Build web target**

Run: `./gradlew :app:web:wasmJsBrowserDistribution`

Expected: Successful build producing the web distribution.

**Step 3: Build Android (if possible in devcontainer)**

Run: `./gradlew :app:android:assembleDebug`

Expected: Successful build (confirms Android suspend changes are correct).

**Step 4: Build desktop**

Run: `./gradlew :app:desktop:jar` or `./gradlew :app:desktop:compileKotlinJvm`

Expected: Successful build.

**Step 5: Update WEB-REMAINING-WORK.md**

Mark item 1 (Full card reading over WebUSB) as complete:

```markdown
### 1. ~~Full card reading over WebUSB~~ DONE
~~Currently only card *detection* works...~~
Full card reading now works over WebUSB. All card types supported by PN533 readers
(DESFire, Classic, Ultralight, FeliCa, CEPAS, ISO 7816) can be read in the browser.
```

**Step 6: Final commit**

```bash
git add WEB-REMAINING-WORK.md
git commit -m "docs: mark WebUSB full card reading as complete"
```

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| `runBlocking` deadlock on iOS GCD | iOS already uses `dispatch_semaphore_wait` (blocking), so `runBlocking` on the worker queue is equivalent. Not on main thread. |
| `runBlocking` on Desktop thread | Desktop poll loop already blocks the thread with `Thread.sleep`. `runBlocking` is safe here. |
| `kotlinx-coroutines-test` missing | Check if already a dependency; add if needed (Task 8). |
| Suspend function overhead | Zero for implementations that don't actually suspend. Kotlin state machine is only created when suspension points exist. |
| PN533FeliCaTagAdapter move breaks Desktop | Desktop uses it from JVM. Moving to commonMain makes it available to all targets including JVM. No breakage. |
| ISO7816CardReader.AppConfig lambda types | Changing `(Protocol) -> T` to `suspend (Protocol) -> T` may need updates where lambdas are constructed. Check `ISO7816Dispatcher.buildAppConfigs()`. |

## Verification Checklist

- [ ] `./gradlew allTests` passes
- [ ] `./gradlew :app:web:wasmJsBrowserDistribution` succeeds
- [ ] `./gradlew :app:desktop:compileKotlinJvm` succeeds
- [ ] `./gradlew :app:android:assembleDebug` succeeds (if SDK available)
- [ ] WebCardScanner no longer emits "card reading in development" error
- [ ] WebCardScanner emits `RawCard<*>` through `scannedCards` flow on successful read
