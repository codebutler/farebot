package com.codebutler.farebot.base.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource

sealed class FormattedString {
    data class Literal(val value: String) : FormattedString()

    data class Resource(
        val resource: StringResource,
        val args: List<Any> = emptyList(),
    ) : FormattedString()

    data class Plural(
        val resource: PluralStringResource,
        val quantity: Int,
        val args: List<Any> = emptyList(),
    ) : FormattedString()

    /** Concatenation of multiple FormattedStrings. */
    data class Concat(val parts: List<FormattedString>) : FormattedString()

    operator fun plus(other: FormattedString): FormattedString =
        Concat(
            when (this) {
                is Concat -> parts + other
                else -> listOf(this, other)
            },
        )

    @Composable
    private fun resolveArg(arg: Any): Any =
        if (arg is FormattedString) arg.resolve() else arg

    @Composable
    fun resolve(): String =
        when (this) {
            is Literal -> value
            is Resource -> {
                if (args.isEmpty()) {
                    stringResource(resource)
                } else {
                    val resolved = mutableListOf<Any>()
                    for (arg in args) resolved.add(resolveArg(arg))
                    stringResource(resource, *resolved.toTypedArray())
                }
            }
            is Plural -> {
                val resolved = mutableListOf<Any>()
                for (arg in args) resolved.add(resolveArg(arg))
                pluralStringResource(resource, quantity, *resolved.toTypedArray())
            }
            is Concat -> {
                val sb = StringBuilder()
                for (part in parts) sb.append(part.resolve())
                sb.toString()
            }
        }

    /**
     * Returns a plain string representation without requiring Compose or coroutine context.
     * For Literal values, returns the string directly.
     * For Resource/Plural values, returns a placeholder since resolution requires async context.
     */
    fun toPlainString(): String =
        when (this) {
            is Literal -> value
            is Resource -> if (args.isEmpty()) "?" else args.joinToString(", ") {
                if (it is FormattedString) it.toPlainString() else it.toString()
            }
            is Plural -> if (args.isEmpty()) "?" else args.joinToString(", ") {
                if (it is FormattedString) it.toPlainString() else it.toString()
            }
            is Concat -> parts.joinToString("") { it.toPlainString() }
        }

    private suspend fun resolveArgAsync(arg: Any): Any =
        if (arg is FormattedString) arg.resolveAsync() else arg

    suspend fun resolveAsync(): String =
        when (this) {
            is Literal -> value
            is Resource -> {
                if (args.isEmpty()) {
                    getString(resource)
                } else {
                    val resolved = mutableListOf<Any>()
                    for (arg in args) resolved.add(resolveArgAsync(arg))
                    getString(resource, *resolved.toTypedArray())
                }
            }
            is Plural -> {
                val resolved = mutableListOf<Any>()
                for (arg in args) resolved.add(resolveArgAsync(arg))
                getPluralString(resource, quantity, *resolved.toTypedArray())
            }
            is Concat -> {
                val sb = StringBuilder()
                for (part in parts) sb.append(part.resolveAsync())
                sb.toString()
            }
        }

    companion object {
        operator fun invoke(value: String) = Literal(value)

        operator fun invoke(resource: StringResource) = Resource(resource)

        operator fun invoke(
            resource: StringResource,
            vararg args: Any,
        ) = Resource(resource, args.toList())

        fun plural(
            resource: PluralStringResource,
            quantity: Int,
            vararg args: Any,
        ) = Plural(resource, quantity, args.toList())
    }
}
