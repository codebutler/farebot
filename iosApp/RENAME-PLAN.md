# Rename iosApp/ to farebot-ios/

Consistency with `farebot-android/`.

## Steps

1. Rename outer directory: `iosApp/` -> `farebot-ios/`
2. Rename inner source directory: `farebot-ios/iosApp/` -> `farebot-ios/FareBot/`
3. Rename entitlements file: `iosApp.entitlements` -> `FareBot.entitlements`
4. Update `project.yml`:
   - Source paths, target name, entitlements path, Info.plist path
5. Regenerate `FareBot.xcodeproj` via `xcodegen` (or manually update `project.pbxproj`)
6. Update `CLAUDE.md` module structure section
7. Update `README.md` references
