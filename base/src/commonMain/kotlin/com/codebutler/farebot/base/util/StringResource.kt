package com.codebutler.farebot.base.util

import org.jetbrains.compose.resources.StringResource as ComposeStringResource

/**
 * Platform-agnostic string resource abstraction.
 * Wraps Compose Multiplatform resources for synchronous string resolution.
 */
interface StringResource {
    fun getString(resource: ComposeStringResource): String

    fun getString(
        resource: ComposeStringResource,
        vararg formatArgs: Any,
    ): String
}
