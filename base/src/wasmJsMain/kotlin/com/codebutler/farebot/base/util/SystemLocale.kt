package com.codebutler.farebot.base.util

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun getNavigatorLanguage(): JsString = js("(navigator.language || navigator.userLanguage || 'en')")

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
actual fun getSystemLanguage(): String =
    getNavigatorLanguage()
        .toString()
        .substringBefore("-")
