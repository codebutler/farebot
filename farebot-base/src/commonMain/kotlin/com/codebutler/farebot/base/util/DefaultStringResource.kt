package com.codebutler.farebot.base.util

import org.jetbrains.compose.resources.StringResource as ComposeStringResource

/**
 * Default cross-platform [StringResource] implementation backed by
 * Compose Multiplatform resources. Resolves strings synchronously
 * via [getStringBlocking].
 */
class DefaultStringResource : StringResource {
    override fun getString(resource: ComposeStringResource): String =
        getStringBlocking(resource)

    override fun getString(resource: ComposeStringResource, vararg formatArgs: Any): String =
        getStringBlocking(resource, *formatArgs)
}
