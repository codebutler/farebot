package com.codebutler.farebot.shared.ui.navigation

import com.codebutler.farebot.card.CardType

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Keys : Screen("keys")
    data object AddKey : Screen("add_key?tagId={tagId}&cardType={cardType}") {
        fun createRoute(tagId: String? = null, cardType: CardType? = null): String = buildString {
            append("add_key")
            val params = mutableListOf<String>()
            if (tagId != null) params.add("tagId=$tagId")
            if (cardType != null) params.add("cardType=${cardType.name}")
            if (params.isNotEmpty()) append("?${params.joinToString("&")}")
        }
    }
    data object Card : Screen("card/{cardKey}") {
        fun createRoute(cardKey: String): String = "card/$cardKey"
    }
    data object CardAdvanced : Screen("card_advanced/{cardKey}") {
        fun createRoute(cardKey: String): String = "card_advanced/$cardKey"
    }
    data object TripMap : Screen("trip_map/{tripKey}") {
        fun createRoute(tripKey: String): String = "trip_map/$tripKey"
    }
}
