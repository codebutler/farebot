package com.codebutler.farebot.base.ui

import com.codebutler.farebot.base.util.FormattedString
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

@Serializable
data class FareBotUiTree(
    val items: List<Item>,
) {
    companion object {
        fun builder(): Builder = Builder()

        private suspend fun buildItems(itemBuilders: List<Item.Builder>): List<Item> =
            itemBuilders.map { it.build() }
    }

    class Builder {
        private val itemBuilders = mutableListOf<Item.Builder>()

        fun item(): Item.Builder {
            val builder = Item.builder()
            itemBuilders.add(builder)
            return builder
        }

        suspend fun build(): FareBotUiTree = FareBotUiTree(buildItems(itemBuilders))
    }

    @Serializable
    data class Item(
        val title: String,
        @Contextual val value: Any?,
        val children: List<Item>,
    ) {
        companion object {
            fun builder(): Builder = Builder()
        }

        class Builder {
            private var title: FormattedString = FormattedString("")
            private var value: Any? = null
            private val childBuilders = mutableListOf<Builder>()

            fun title(text: String): Builder {
                this.title = FormattedString(text)
                return this
            }

            fun title(textRes: StringResource): Builder {
                this.title = FormattedString(textRes)
                return this
            }

            fun title(formattedString: FormattedString): Builder {
                this.title = formattedString
                return this
            }

            fun value(value: Any?): Builder {
                this.value = value
                return this
            }

            fun item(): Builder {
                val builder = Item.builder()
                childBuilders.add(builder)
                return builder
            }

            fun item(
                title: String,
                value: Any?,
            ): Builder =
                item()
                    .title(title)
                    .value(value)

            fun item(
                title: StringResource,
                value: Any?,
            ): Builder =
                item().also {
                    it.title = FormattedString(title)
                    it.value(value)
                }

            suspend fun build(): Item =
                Item(
                    title = title.resolveAsync(),
                    value = value,
                    children = buildItems(childBuilders),
                )
        }
    }
}
