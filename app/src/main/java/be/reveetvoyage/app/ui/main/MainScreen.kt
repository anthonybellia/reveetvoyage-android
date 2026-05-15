package be.reveetvoyage.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Airplanemode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import be.reveetvoyage.app.ui.screens.DevisScreen
import be.reveetvoyage.app.ui.screens.HomeScreen
import be.reveetvoyage.app.ui.screens.PassengersScreen
import be.reveetvoyage.app.ui.screens.SettingsScreen
import be.reveetvoyage.app.ui.screens.VoyagesScreen
import be.reveetvoyage.app.ui.theme.RevOrange

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Accueil", Icons.Default.Home),
    Voyages("voyages", "Voyages", Icons.Default.Airplanemode),
    Devis("devis", "Devis", Icons.Default.Description),
    Passengers("passengers", "Passagers", Icons.Default.Group),
    Profile("profile", "Profil", Icons.Default.Person),
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Tab.Home.route

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RevOrange,
                            selectedTextColor = RevOrange,
                            indicatorColor = RevOrange.copy(alpha = 0.12f),
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Home.route)       { HomeScreen() }
            composable(Tab.Voyages.route)    { VoyagesScreen() }
            composable(Tab.Devis.route)      { DevisScreen() }
            composable(Tab.Passengers.route) { PassengersScreen() }
            composable(Tab.Profile.route)    { SettingsScreen(onLogout = onLogout) }
        }
    }
}
