package com.codebutler.farebot.shared.platform

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

actual fun getDeviceRegion(): String? = NSLocale.currentLocale.countryCode
