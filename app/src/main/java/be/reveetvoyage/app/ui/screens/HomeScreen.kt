package be.reveetvoyage.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.R
import be.reveetvoyage.app.data.api.ApiService
import be.reveetvoyage.app.data.model.Devis
import be.reveetvoyage.app.data.model.User
import be.reveetvoyage.app.data.model.Voyage
import be.reveetvoyage.app.data.repo.MessageRepository
import be.reveetvoyage.app.data.repo.UserRepository
import be.reveetvoyage.app.ui.components.*
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
    private val userRepo: UserRepository,
    private val messageRepo: MessageRepository,
) : ViewModel() {
    private val _voyages = MutableStateFlow<List<Voyage>>(emptyList())
    val voyages: StateFlow<List<Voyage>> = _voyages.asStateFlow()

    private val _devis = MutableStateFlow<List<Devis>>(emptyList())
    val devis: StateFlow<List<Devis>> = _devis.asStateFlow()

    private val _unread = MutableStateFlow(0)
    val unread: StateFlow<Int> = _unread.asStateFlow()

    val currentUser: StateFlow<User?> = userRepo.currentUser

    init { load() }

    fun load() {
        viewModelScope.launch {
            userRepo.refresh()
            _voyages.value = runCatching { api.voyages(perPage = 5).data }.getOrDefault(emptyList())
            _devis.value = runCatching { api.devis(perPage = 5).data }.getOrDefault(emptyList())
            _unread.value = runCatching { messageRepo.unreadCount() }.getOrDefault(0)
        }
    }
}

@Composable
fun HomeScreen(
    onOpenVoyage: (Int) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenNewVoyageRequest: () -> Unit,
    onOpenPassengers: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val voyages by vm.voyages.collectAsState()
    val devis by vm.devis.collectAsState()
    val unread by vm.unread.collectAsState()
    val user by vm.currentUser.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(RevYellow.copy(alpha = .15f), RevBackground))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            HeroHeader(user, unread, onOpenNotifications)
            QuickActions(
                onNewVoyage = onOpenNewVoyageRequest,
                onPassengers = onOpenPassengers,
            )

            // Voyages section
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionTitle("Mes voyages", Icons.Default.Flight,
                    if (voyages.isEmpty()) null else "${voyages.size}")
                if (voyages.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                               modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            Icon(Icons.Default.Luggage, null, tint = RevOrange.copy(alpha = .6f),
                                 modifier = Modifier.size(32.dp))
                            Text("Aucun voyage programmé", color = RevTextSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    voyages.take(3).forEach { v ->
                        VoyageCard(v, onClick = { onOpenVoyage(v.id) })
                    }
                }
            }

            // Devis pending section
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionTitle("En attente de validation", Icons.Default.Schedule,
                    if (devis.isEmpty()) null else "${devis.size}")
                if (devis.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                               modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            Icon(Icons.Default.Description, null, tint = RevOrange.copy(alpha = .6f),
                                 modifier = Modifier.size(32.dp))
                            Text("Aucune demande en cours", color = RevTextSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    devis.take(3).forEach { d -> DevisRowMini(d) }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HeroHeader(user: User?, unread: Int, onBellClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()) {
        AvatarView(user?.prenom ?: "", user?.nom ?: "", avatarPath = user?.avatar, size = 52)
        Column(modifier = Modifier.weight(1f)) {
            Text("Bonjour,", color = RevTextSecondary, fontSize = 13.sp)
            Text(user?.prenom ?: "—", color = RevBrown, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(RevCardBackground)
            .clickable(onClick = onBellClick), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Notifications, null, tint = RevBrown, modifier = Modifier.size(20.dp))
            if (unread > 0) {
                Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp)
                    .clip(CircleShape).background(RevRed)
                    .padding(horizontal = 5.dp, vertical = 2.dp)) {
                    Text("$unread", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QuickActions(onNewVoyage: () -> Unit, onPassengers: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        QuickCard(
            title = "Nouveau voyage",
            subtitle = "Demande un devis",
            icon = Icons.Default.FlightTakeoff,
            colors = listOf(RevYellow, RevOrange),
            modifier = Modifier.weight(1f),
            onClick = onNewVoyage,
        )
        QuickCard(
            title = "Mes passagers",
            subtitle = "Gérer la liste",
            icon = Icons.Default.Group,
            colors = listOf(RevOrange, RevRed),
            modifier = Modifier.weight(1f),
            onClick = onPassengers,
        )
    }
}

@Composable
private fun QuickCard(
    title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: List<Color>, modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(colors)).clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = .18f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White)
            }
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = Color.White.copy(alpha = .85f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DevisRowMini(d: Devis) {
    val (label, kind) = devisStatutLabel(d.statut)
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(RevOrange.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Description, null, tint = RevOrange, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(d.titre_voyage ?: d.destination ?: d.destination_souhaitee ?: "Demande",
                     color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                d.type_voyage?.let {
                    Text(it.replace('_', ' ').replaceFirstChar(Char::titlecase),
                         color = RevTextSecondary, fontSize = 11.sp)
                }
            }
            StatusBadge(label, kind)
        }
    }
}
