package be.reveetvoyage.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import be.reveetvoyage.app.ui.auth.AuthViewModel
import be.reveetvoyage.app.ui.auth.LoginScreen
import be.reveetvoyage.app.ui.auth.RegisterScreen
import be.reveetvoyage.app.ui.main.MainScreen
import be.reveetvoyage.app.ui.splash.SplashScreen
import kotlinx.coroutines.delay

@Composable
fun RootScreen() {
    val navController = rememberNavController()
    val authVm: AuthViewModel = hiltViewModel()
    val isAuthenticated by authVm.isAuthenticated.collectAsState()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            LaunchedEffect(Unit) {
                authVm.bootstrap()
                delay(2500)
                val dest = if (authVm.isAuthenticated.value) "main" else "login"
                navController.navigate(dest) {
                    popUpTo("splash") { inclusive = true }
                }
            }
            SplashScreen()
        }
        composable("login") {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate("register") },
            )
        }
        composable("register") {
            RegisterScreen(
                onRegistered = {
                    navController.navigate("main") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable("main") {
            MainScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
