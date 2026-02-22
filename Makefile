IOS_DEVICE_ID := $(shell xcrun xctrace list devices 2>/dev/null | grep -v Simulator | grep -E '\([0-9A-F-]+\)$$' | grep -v Mac | head -1 | grep -oE '[0-9A-F]{8}-[0-9A-F]{16}')
IOS_DERIVED_DATA := build/ios-derived-data
IOS_APP_PATH := $(IOS_DERIVED_DATA)/Build/Products/Debug-iphoneos/FareBot.app
IOS_SIM_APP_PATH := $(IOS_DERIVED_DATA)/Build/Products/Debug-iphonesimulator/FareBot.app

.PHONY: android android-install ios ios-sim ios-install desktop web web-run test clean help

## Android

android: ## Build Android debug APK
	./gradlew :app:android:assembleDebug

android-install: android ## Build and install on connected Android device
	adb install -r app/android/build/outputs/apk/debug/app-android-debug.apk

## iOS

ios: ## Build iOS app for physical device
	./gradlew :app:linkDebugFrameworkIosArm64
	xcodebuild -project app/ios/FareBot.xcodeproj -scheme FareBot \
		-derivedDataPath $(IOS_DERIVED_DATA) \
		-destination 'id=$(IOS_DEVICE_ID)' -allowProvisioningUpdates build

ios-sim: ## Build iOS app for simulator
	./gradlew --no-daemon :app:linkDebugFrameworkIosSimulatorArm64
	xcodebuild -project app/ios/FareBot.xcodeproj -scheme FareBot \
		-derivedDataPath $(IOS_DERIVED_DATA) \
		-destination 'platform=iOS Simulator,name=iPhone 16' build

ios-install: ios ## Build and install on connected iOS device
	xcrun devicectl device install app --device $(IOS_DEVICE_ID) "$(IOS_APP_PATH)"

## Desktop

desktop: ## Run macOS desktop app (experimental)
	./gradlew :app:desktop:run

## Web

web: ## Build web app (experimental, WebAssembly)
	./gradlew :app:web:wasmJsBrowserDistribution

web-run: ## Run web app dev server with hot reload
	./gradlew :app:web:wasmJsBrowserDevelopmentRun

## Tests

test: ## Run all tests
	./gradlew allTests -x linkDebugTestIosSimulatorArm64 -x linkDebugTestIosX64

## Utility

clean: ## Clean all build artifacts
	./gradlew clean
	rm -rf $(IOS_DERIVED_DATA)

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
