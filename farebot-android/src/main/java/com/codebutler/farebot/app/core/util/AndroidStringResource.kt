package com.codebutler.farebot.app.core.util

import com.codebutler.farebot.base.util.StringResource
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource as ComposeStringResource
import org.jetbrains.compose.resources.getString as composeGetString

class AndroidStringResource : StringResource {
    override fun getString(resource: ComposeStringResource): String =
        runBlocking { composeGetString(resource) }

    override fun getString(resource: ComposeStringResource, vararg formatArgs: Any): String =
        runBlocking { composeGetString(resource, *formatArgs) }
}
