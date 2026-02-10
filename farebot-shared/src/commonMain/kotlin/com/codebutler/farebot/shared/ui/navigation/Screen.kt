package com.codebutler.farebot.shared.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Keys : Screen("keys")
    data object AddKey : Screen("add_key")
    data object Help : Screen("help")
    data object Settings : Screen("settings")
    data object Card : Screen("card/{cardKey}") {
        fun createRoute(cardKey: String): String = "card/$cardKey"
    }
    data object CardAdvanced : Screen("card_advanced/{cardKey}") {
        fun createRoute(cardKey: String): String = "card_advanced/$cardKey"
    }
    data object TripMap : Screen("trip_map/{tripKey}") {
        fun createRoute(tripKey: String): String = "trip_map/$tripKey"
    }
    data object CardsMap : Screen("cards_map")
}
