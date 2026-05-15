package be.reveetvoyage.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.api.ApiService
import be.reveetvoyage.app.data.model.Devis
import be.reveetvoyage.app.data.model.User
import be.reveetvoyage.app.data.model.Voyage
import be.reveetvoyage.app.data.repo.AuthRepository
import be.reveetvoyage.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: ApiService,
    private val authRepo: AuthRepository,
) : ViewModel() {
    private val _voyages = MutableStateFlow<List<Voyage>>(emptyList())
    val voyages: StateFlow<List<Voyage>> = _voyages.asStateFlow()

    private val _devis = MutableStateFlow<List<Devis>>(emptyList())
    val devis: StateFlow<List<Devis>> = _devis.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _user.value = authRepo.loadCurrentUser()
            _voyages.value = runCatching { api.voyages(perPage = 5).data }.getOrDefault(emptyList())
            _devis.value = runCatching { api.devis(perPage = 5).data }.getOrDefault(emptyList())
        }
    }
}

@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val voyages by vm.voyages.collectAsState()
    val devis by vm.devis.collectAsState()
    val user by vm.user.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(RevYellow.copy(alpha = 0.15f), RevBackground))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroHeader(user)
            SectionTitle("Mes voyages")

            if (voyages.isEmpty()) {
                EmptyCard("Aucun voyage programmé")
            } else {
                voyages.take(3).forEach { VoyageCard(it) }
            }

            SectionTitle("En attente de validation")

            if (devis.isEmpty()) {
                EmptyCard("Aucune demande en cours")
            } else {
                devis.take(3).forEach { DevisRow(it) }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroHeader(user: User?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(RevYellow, RevOrange, RevRed))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${user?.prenom?.firstOrNull() ?: '?'}${user?.nom?.firstOrNull() ?: '?'}",
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
        Column {
            Text("Bonjour,", color = RevTextSecondary, fontSize = 13.sp)
            Text(user?.prenom ?: "—", color = RevBrown, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = RevBrown,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
    )
}

@Composable
private fun EmptyCard(text: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RevCardBackground),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = RevTextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun VoyageCard(v: Voyage) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RevCardBackground),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(RevYellow.copy(alpha = .6f), RevOrange.copy(alpha = .6f))))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(v.titre, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(v.destination, color = RevTextSecondary, fontSize = 12.sp)
            }
            StatusPill(label = v.statut_label, color = RevOrange)
        }
    }
}

@Composable
private fun DevisRow(d: Devis) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RevCardBackground),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    d.titre_voyage ?: d.destination ?: d.destination_souhaitee ?: "Demande",
                    color = RevBrown,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                d.type_voyage?.let {
                    Text(it.replace('_', ' ').replaceFirstChar(Char::titlecase), color = RevTextSecondary, fontSize = 11.sp)
                }
            }
            StatusPill(label = d.statut.replaceFirstChar(Char::titlecase), color = RevYellow)
        }
    }
}

@Composable
private fun StatusPill(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
