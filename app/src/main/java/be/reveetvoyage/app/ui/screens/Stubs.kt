package be.reveetvoyage.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import be.reveetvoyage.app.ui.theme.RevBrown
import be.reveetvoyage.app.ui.theme.RevOrange

@Composable
fun VoyagesScreen() = StubScreen(title = "Voyages", body = "À venir : carnet de voyage avec timeline étapes cochables.")

@Composable
fun DevisScreen() = StubScreen(title = "Mes demandes", body = "À venir : devis sectionnés par statut.")

@Composable
fun PassengersScreen() = StubScreen(title = "Passagers", body = "À venir : CRUD passagers + scan doc.")

@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Profil", style = MaterialTheme.typography.headlineMedium, color = RevBrown)
        Text("Édition profil, mot de passe, langue, notifications, messagerie : à venir.")
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = RevOrange),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Se déconnecter", color = androidx.compose.ui.graphics.Color.White)
        }
    }
}

@Composable
private fun StubScreen(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = RevBrown)
        Text(body)
    }
}
