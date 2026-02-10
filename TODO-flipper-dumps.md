# Flipper Dump TODO

Card dumps needed to verify Metrodroid port correctness. Ordered by risk (biggest rewrites first).

## Already have dumps

- [x] Clipper (DESFire) — `flipper/Clipper.nfc`
- [x] ORCA (DESFire) — `flipper/ORCA.nfc`
- [x] Suica (FeliCa) — `flipper/Suica.nfc`
- [x] PASMO (FeliCa) — `flipper/PASMO.nfc`
- [x] ICOCA (FeliCa) — `flipper/ICOCA.nfc`
- [x] EasyCard (Classic) — `easycard/deadbeef.mfc`

## High priority — full rewrites, no test coverage

- [ ] **OV-chipkaart Classic** (Mifare Classic 4K) — full EN1545 rewrite, trip dedup, subscriptions, autocharge
- [ ] **OV-chipkaart Ultralight** (Ultralight) — single-use disposable OVC cards
- [ ] **HSL v1** (DESFire) — Helsinki region, APP_ID 0x1120ef, old format
- [ ] **HSL v2** (DESFire) — Helsinki region, APP_ID 0x1120ef, new file structure
- [ ] **HSL Waltti** (DESFire) — Waltti region (Oulu, Lahti, etc.), APP_ID 0x10ab
- [ ] **HSL Ultralight** (Ultralight) — Helsinki single-use tickets
- [ ] **Snapper** (ISO7816/KSX6924) — Wellington NZ, full impl from stub. Note: Flipper may not support ISO7816 KSX6924 reads; may need Android NFC dump instead.

## Medium priority — minor fixes, lower risk

- [ ] **SeqGo** (DESFire) — added system code check + refills
- [ ] **KMT** (Classic) — added transaction counter + last amount to info display

## Nice to have — safe cast fixes only

- [ ] **Clipper with locked files** (DESFire) — to test the `as?` safe cast fallback path (existing dump may already work)
