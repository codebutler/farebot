package com.codebutler.farebot.shared.ui.navigation

import com.codebutler.farebot.card.CardType

sealed class Screen(
    val route: String,
) {
    data object Home : Screen("home")

    data object Keys : Screen("keys")

    data object AddKey : Screen("add_key?tagId={tagId}&cardType={cardType}") {
        fun createRoute(
            tagId: String? = null,
            cardType: CardType? = null,
        ): String =
            buildString {
                append("add_key")
                val params = mutableListOf<String>()
                if (tagId != null) params.add("tagId=$tagId")
                if (cardType != null) params.add("cardType=${cardType.name}")
                if (params.isNotEmpty()) append("?${params.joinToString("&")}")
            }
    }

    data object Card : Screen("card/{cardKey}?scanIdsKey={scanIdsKey}&currentScanId={currentScanId}") {
        fun createRoute(
            cardKey: String,
            scanIdsKey: String? = null,
            currentScanId: String? = null,
        ): String =
            buildString {
                append("card/$cardKey")
                val params = mutableListOf<String>()
                if (scanIdsKey != null) params.add("scanIdsKey=$scanIdsKey")
                if (currentScanId != null) params.add("currentScanId=$currentScanId")
                if (params.isNotEmpty()) append("?${params.joinToString("&")}")
            }
    }

    data object CardAdvanced : Screen("card_advanced/{cardKey}") {
        fun createRoute(cardKey: String): String = "card_advanced/$cardKey"
    }

    data object SampleCard : Screen("sample_card/{cardKey}/{cardName}") {
        fun createRoute(
            cardKey: String,
            cardName: String,
        ): String = "sample_card/$cardKey/$cardName"
    }

    data object TripMap : Screen("trip_map/{tripKey}") {
        fun createRoute(tripKey: String): String = "trip_map/$tripKey"
    }
}
