package com.codebutler.farebot.base.util

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

actual fun getStringBlocking(resource: StringResource): String =
    throw UnsupportedOperationException("runBlocking is not available on wasmJs; use suspend getString() instead")

actual fun getStringBlocking(
    resource: StringResource,
    vararg formatArgs: Any,
): String =
    throw UnsupportedOperationException("runBlocking is not available on wasmJs; use suspend getString() instead")

actual fun getPluralStringBlocking(
    resource: PluralStringResource,
    quantity: Int,
    vararg formatArgs: Any,
): String =
    throw UnsupportedOperationException("runBlocking is not available on wasmJs; use suspend getPluralString() instead")
