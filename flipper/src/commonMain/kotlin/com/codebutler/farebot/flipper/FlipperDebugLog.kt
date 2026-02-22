package com.codebutler.farebot.flipper

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FlipperDebugLog {
    /** Bump this on every meaningful code change to confirm the correct build is installed. */
    const val BUILD_VERSION = 2

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    var fileWriter: ((String) -> Unit)? = null

    fun log(msg: String) {
        _lines.value = _lines.value + msg
        fileWriter?.invoke(msg)
    }

    fun clear() {
        _lines.value = emptyList()
        log("=== FlipperDebugLog build=$BUILD_VERSION ===")
    }
}
