package com.codebutler.farebot.shared.platform

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun getNavigatorLanguage(): JsString = js("(navigator.language || '')")

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
actual fun getDeviceRegion(): String? {
    val language = getNavigatorLanguage().toString()
    val parts = language.split("-")
    return if (parts.size >= 2) parts.last().uppercase() else null
}
