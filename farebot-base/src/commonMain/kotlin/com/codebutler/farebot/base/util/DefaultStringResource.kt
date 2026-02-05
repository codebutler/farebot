package com.codebutler.farebot.base.util

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource as ComposeStringResource
import org.jetbrains.compose.resources.getString as composeGetString

/**
 * Default cross-platform [StringResource] implementation backed by
 * Compose Multiplatform resources. Resolves strings synchronously
 * via [runBlocking].
 */
class DefaultStringResource : StringResource {
    override fun getString(resource: ComposeStringResource): String =
        runBlocking { composeGetString(resource) }

    override fun getString(resource: ComposeStringResource, vararg formatArgs: Any): String =
        runBlocking { composeGetString(resource, *formatArgs) }
}
