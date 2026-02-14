package com.codebutler.farebot.base.util

actual fun getSystemLanguage(): String =
    java.util.Locale
        .getDefault()
        .language
