package com.codebutler.farebot.base.ui

import com.codebutler.farebot.base.util.StringResource
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

@Serializable
data class FareBotUiTree(
    val items: List<Item>
) {
    companion object {
        fun builder(stringResource: StringResource): Builder = Builder(stringResource)

        private fun buildItems(itemBuilders: List<Item.Builder>): List<Item> {
            return itemBuilders.map { it.build() }
        }
    }

    class Builder(private val stringResource: StringResource) {
        private val itemBuilders = mutableListOf<Item.Builder>()

        fun item(): Item.Builder {
            val builder = Item.builder(stringResource)
            itemBuilders.add(builder)
            return builder
        }

        fun build(): FareBotUiTree {
            return FareBotUiTree(buildItems(itemBuilders))
        }
    }

    @Serializable
    data class Item(
        val title: String,
        @Contextual val value: Any?,
        val children: List<Item>
    ) {
        companion object {
            fun builder(stringResource: StringResource): Builder = Builder(stringResource)
        }

        class Builder(private val stringResource: StringResource) {
            private var title: String = ""
            private var value: Any? = null
            private val childBuilders = mutableListOf<Builder>()

            fun title(text: String): Builder {
                this.title = text
                return this
            }

            fun title(textRes: ComposeStringResource): Builder {
                return title(stringResource.getString(textRes))
            }

            fun value(value: Any?): Builder {
                this.value = value
                return this
            }

            fun item(): Builder {
                val builder = Item.builder(stringResource)
                childBuilders.add(builder)
                return builder
            }

            fun item(title: String, value: Any?): Builder {
                return item()
                    .title(title)
                    .value(value)
            }

            fun item(title: ComposeStringResource, value: Any?): Builder {
                return item(stringResource.getString(title), value)
            }

            fun build(): Item {
                return Item(
                    title = title,
                    value = value,
                    children = buildItems(childBuilders)
                )
            }
        }
    }
}
