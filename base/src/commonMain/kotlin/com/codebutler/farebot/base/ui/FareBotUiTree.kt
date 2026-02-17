package com.codebutler.farebot.base.ui

import com.codebutler.farebot.base.util.FormattedString

data class FareBotUiTree(
    val items: List<Item>,
) {
    data class Item(
        val title: FormattedString,
        val value: Any? = null,
        val children: List<Item> = emptyList(),
    )
}
