package com.liam.kaptalismusaufhalter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.liam.kaptalismusaufhalter.ui.screens.decision.DecisionScreen
import com.liam.kaptalismusaufhalter.ui.screens.friends.FriendsScreen
import com.liam.kaptalismusaufhalter.ui.screens.newwish.NewWishScreen
import com.liam.kaptalismusaufhalter.ui.screens.piggybank.PiggyBankScreen
import com.liam.kaptalismusaufhalter.ui.screens.reift.ReiftScreen
import com.liam.kaptalismusaufhalter.ui.screens.settings.ExcludedAppsScreen
import com.liam.kaptalismusaufhalter.ui.screens.settings.SettingsScreen
import com.liam.kaptalismusaufhalter.ui.screens.start.StartScreen

@Composable
fun AppNavGraph(navController: NavHostController, onSettingsClick: () -> Unit) {
    NavHost(navController = navController, startDestination = Destination.Start.route) {
        composable(Destination.Start.route) {
            StartScreen(
                onWishClick = { id -> navController.navigate(Destination.Decision.route(id)) },
                onSeeAllClick = { navController.navigate(Destination.Reift.route) },
                onSettingsClick = onSettingsClick
            )
        }
        composable(Destination.Reift.route) {
            ReiftScreen(onWishClick = { id -> navController.navigate(Destination.Decision.route(id)) })
        }
        composable(Destination.NewWish.route) {
            NewWishScreen(
                onSaved = { navController.navigate(Destination.Reift.route) { popUpTo(Destination.Start.route) } },
                onBack = { navController.navigate(Destination.Start.route) { popUpTo(Destination.Start.route) } }
            )
        }
        composable(Destination.PiggyBank.route) {
            PiggyBankScreen()
        }
        composable(Destination.Friends.route) {
            FriendsScreen()
        }
        composable(Destination.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onExcludedAppsClick = { navController.navigate(Destination.ExcludedApps.route) }
            )
        }
        composable(Destination.ExcludedApps.route) {
            ExcludedAppsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Destination.Decision.route,
            arguments = listOf(navArgument(Destination.Decision.ARG_WISH_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val wishId = backStackEntry.arguments?.getLong(Destination.Decision.ARG_WISH_ID) ?: return@composable
            DecisionScreen(wishId = wishId, onDone = { navController.popBackStack() })
        }
    }
}
