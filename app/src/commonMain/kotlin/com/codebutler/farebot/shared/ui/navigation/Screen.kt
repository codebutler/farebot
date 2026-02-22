package com.codebutler.farebot.shared.ui.navigation

sealed class Screen(
    val route: String,
) {
    data object Home : Screen("home")

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

    data object Flipper : Screen("flipper")
}
