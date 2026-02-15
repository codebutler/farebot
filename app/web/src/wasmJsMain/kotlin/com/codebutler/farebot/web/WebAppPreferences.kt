@file:OptIn(ExperimentalWasmJsInterop::class)

package com.codebutler.farebot.web

import com.codebutler.farebot.shared.platform.AppPreferences
import kotlin.js.ExperimentalWasmJsInterop

private fun jsLocalStorageGetItem(key: JsString): JsString? =
    js("localStorage.getItem(key)")

private fun jsLocalStorageSetItem(key: JsString, value: JsString) {
    js("localStorage.setItem(key, value)")
}

class WebAppPreferences : AppPreferences {
    override fun getBoolean(
        key: String,
        default: Boolean,
    ): Boolean {
        val value = jsLocalStorageGetItem(key.toJsString())
        return value?.toString()?.toBooleanStrictOrNull() ?: default
    }

    override fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        jsLocalStorageSetItem(key.toJsString(), value.toString().toJsString())
    }
}
