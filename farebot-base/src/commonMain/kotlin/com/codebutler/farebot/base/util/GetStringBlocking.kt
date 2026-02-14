package com.codebutler.farebot.base.util

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

fun getStringBlocking(resource: StringResource): String = runBlocking { getString(resource) }

fun getStringBlocking(
    resource: StringResource,
    vararg formatArgs: Any,
): String = runBlocking { getString(resource, *formatArgs) }

fun getPluralStringBlocking(
    resource: PluralStringResource,
    quantity: Int,
    vararg formatArgs: Any,
): String = runBlocking { getPluralString(resource, quantity, *formatArgs) }
