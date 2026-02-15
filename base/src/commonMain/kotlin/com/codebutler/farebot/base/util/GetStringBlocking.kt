package com.codebutler.farebot.base.util

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

expect fun getStringBlocking(resource: StringResource): String

expect fun getStringBlocking(
    resource: StringResource,
    vararg formatArgs: Any,
): String

expect fun getPluralStringBlocking(
    resource: PluralStringResource,
    quantity: Int,
    vararg formatArgs: Any,
): String
