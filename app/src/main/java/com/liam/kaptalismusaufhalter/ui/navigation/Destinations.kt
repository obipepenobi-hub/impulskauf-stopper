package com.liam.kaptalismusaufhalter.ui.navigation

sealed class Destination(val route: String) {
    object Start : Destination("start")
    object Reift : Destination("reift")
    object NewWish : Destination("new_wish")
    object PiggyBank : Destination("piggy_bank")
    object Friends : Destination("friends")
    object Settings : Destination("settings")
    object ExcludedApps : Destination("excluded_apps")

    object Decision : Destination("decision/{wishId}") {
        const val ARG_WISH_ID = "wishId"
        fun route(wishId: Long) = "decision/$wishId"
    }
}

val BOTTOM_NAV_ITEMS = listOf(
    Destination.Start,
    Destination.Reift,
    Destination.NewWish,
    Destination.PiggyBank,
    Destination.Friends
)
