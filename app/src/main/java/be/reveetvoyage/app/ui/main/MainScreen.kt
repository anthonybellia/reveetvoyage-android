package be.reveetvoyage.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import be.reveetvoyage.app.ui.screens.*
import be.reveetvoyage.app.ui.theme.RevOrange

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Accueil", Icons.Default.Home),
    Voyages("voyages", "Voyages", Icons.Default.Flight),
    Devis("devis", "Devis", Icons.Default.Description),
    Passengers("passengers", "Passagers", Icons.Default.Group),
    Profile("profile", "Profil", Icons.Default.Person),
}

private val tabRoutes = Tab.values().map { it.route }.toSet()

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            // Hide bottom bar on detail / sub-screens
            if (currentRoute in tabRoutes) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
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
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            // Tabs
            composable(Tab.Home.route) {
                HomeScreen(
                    onOpenVoyage = { id -> navController.navigate("voyage/$id") },
                    onOpenNotifications = { navController.navigate("notifications") },
                    onOpenMessages = { navController.navigate("messages?draft=") },
                    onOpenNewVoyageRequest = {
                        val draft = "Bonjour ! J'aimerais faire une demande de voyage. " +
                            "Voici mes critères :\n\n" +
                            "• Destination : \n• Dates souhaitées : \n" +
                            "• Nombre de personnes : \n" +
                            "• Type (couple/famille/amis/solo/lune de miel) : \n" +
                            "• Budget approximatif : \n\nMerci !"
                        val encoded = java.net.URLEncoder.encode(draft, "UTF-8")
                        navController.navigate("messages?draft=$encoded")
                    },
                    onOpenPassengers = {
                        navController.navigate(Tab.Passengers.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Tab.Voyages.route) {
                VoyagesScreen(onOpenVoyage = { id -> navController.navigate("voyage/$id") })
            }
            composable(Tab.Devis.route) { DevisScreen() }
            composable(Tab.Passengers.route) { PassengersScreen() }
            composable(Tab.Profile.route) {
                SettingsScreen(
                    onLogout = onLogout,
                    onOpenEditProfile = { navController.navigate("edit-profile") },
                    onOpenChangePassword = { navController.navigate("change-password") },
                    onOpenLanguage = { navController.navigate("language") },
                    onOpenNotifications = { navController.navigate("notif-settings") },
                    onOpenMessages = { navController.navigate("messages?draft=") },
                    onOpenPage = { slug, title ->
                        val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                        navController.navigate("page/$slug/$encodedTitle")
                    },
                )
            }

            // Sub-routes
            composable("voyage/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toIntOrNull() ?: 0
                VoyageDetailScreen(
                    voyageId = id,
                    onBack = { navController.popBackStack() },
                    onOpenEtape = { etapeId -> navController.navigate("voyage/$id/etape/$etapeId") },
                )
            }
            composable("voyage/{voyageId}/etape/{etapeId}") { entry ->
                val voyageId = entry.arguments?.getString("voyageId")?.toIntOrNull() ?: 0
                val etapeId = entry.arguments?.getString("etapeId")?.toIntOrNull() ?: 0
                EtapeDetailScreen(
                    voyageId = voyageId, etapeId = etapeId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("edit-profile") { EditProfileScreen(onBack = { navController.popBackStack() }) }
            composable("change-password") { ChangePasswordScreen(onBack = { navController.popBackStack() }) }
            composable("language") { LanguageScreen(onBack = { navController.popBackStack() }) }
            composable("notif-settings") { NotificationsSettingsScreen(onBack = { navController.popBackStack() }) }
            composable("notifications") {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMessages = { navController.navigate("messages?draft=") },
                )
            }
            composable(
                "messages?draft={draft}",
                arguments = listOf(androidx.navigation.navArgument("draft") {
                    type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null
                })
            ) { entry ->
                val raw = entry.arguments?.getString("draft")
                val decoded = raw?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                MessagesScreen(
                    onBack = { navController.popBackStack() },
                    initialDraft = decoded,
                    onOpenFiles = { navController.navigate("files") },
                )
            }
            composable("files") {
                FilesScreen(onBack = { navController.popBackStack() })
            }
            composable("page/{slug}/{title}") { entry ->
                val slug = entry.arguments?.getString("slug") ?: ""
                val title = java.net.URLDecoder.decode(entry.arguments?.getString("title") ?: "", "UTF-8")
                PageScreen(slug = slug, fallbackTitle = title, onBack = { navController.popBackStack() })
            }
        }
    }
}
