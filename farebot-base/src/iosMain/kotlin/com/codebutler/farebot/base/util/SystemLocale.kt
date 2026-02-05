package com.codebutler.farebot.base.util

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun getSystemLanguage(): String = NSLocale.currentLocale.languageCode
