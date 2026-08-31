package com.liam.kaptalismusaufhalter.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.liam.kaptalismusaufhalter.ui.navigation.Destination

private data class NavItem(val destination: Destination, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

private val NAV_ITEMS = listOf(
    NavItem(Destination.Start, Icons.Filled.Home, "Start"),
    NavItem(Destination.Reift, Icons.Filled.Spa, "Reift"),
    NavItem(Destination.NewWish, Icons.Filled.Add, "Neu"),
    NavItem(Destination.PiggyBank, Icons.Filled.Savings, "Schwein"),
    NavItem(Destination.Friends, Icons.Filled.Group, "Freunde")
)

@Composable
fun BottomNavBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        NAV_ITEMS.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { androidx.compose.material3.Text(item.label) }
            )
        }
    }
}
