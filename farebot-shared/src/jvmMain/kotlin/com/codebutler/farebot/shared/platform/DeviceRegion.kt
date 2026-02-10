package com.codebutler.farebot.shared.platform

actual fun getDeviceRegion(): String? = java.util.Locale.getDefault().country.takeIf { it.isNotEmpty() }
