package be.reveetvoyage.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.model.VoyageInvitation
import be.reveetvoyage.app.data.repo.VoyageRepository
import be.reveetvoyage.app.ui.components.*
import be.reveetvoyage.app.ui.theme.*
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvitationsViewModel @Inject constructor(
    private val repo: VoyageRepository,
) : ViewModel() {
    private val _invitations = MutableStateFlow<List<VoyageInvitation>>(emptyList())
    val invitations: StateFlow<List<VoyageInvitation>> = _invitations

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // Ids des voyages dont l'invitation est en cours de traitement (accept/decline).
    private val _processing = MutableStateFlow<Set<Int>>(emptySet())
    val processing: StateFlow<Set<Int>> = _processing

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _invitations.value = repo.invitations()
            _loading.value = false
        }
    }

    // Accepte l'invitation puis la retire de la liste. `onAccepted` permet de
    // déclencher un refresh des voyages côté appelant (la liste change).
    fun accept(voyageId: Int, onAccepted: () -> Unit) {
        viewModelScope.launch {
            _processing.value = _processing.value + voyageId
            val ok = repo.acceptInvitation(voyageId)
            _processing.value = _processing.value - voyageId
            if (ok) {
                _invitations.value = _invitations.value.filterNot { it.voyage.id == voyageId }
                onAccepted()
            }
        }
    }

    fun decline(voyageId: Int) {
        viewModelScope.launch {
            _processing.value = _processing.value + voyageId
            val ok = repo.declineInvitation(voyageId)
            _processing.value = _processing.value - voyageId
            if (ok) {
                _invitations.value = _invitations.value.filterNot { it.voyage.id == voyageId }
            }
        }
    }
}

@Composable
fun InvitationsScreen(
    onBack: () -> Unit,
    onAccepted: () -> Unit = {},
    vm: InvitationsViewModel = hiltViewModel(),
) {
    val invitations by vm.invitations.collectAsState()
    val loading by vm.loading.collectAsState()
    val processing by vm.processing.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        IOSTopBar(title = "Mes invitations", onBack = onBack)

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(RevYellow.copy(alpha = .06f), RevBackground))
            )
        ) {
            when {
                loading && invitations.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RevOrange)
                    }
                }
                invitations.isEmpty() -> {
                    EmptyState(
                        Icons.Default.MailOutline,
                        "Aucune invitation",
                        "Les invitations à rejoindre un voyage apparaîtront ici.",
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(invitations, key = { it.voyage.id }) { inv ->
                            InvitationCard(
                                invitation = inv,
                                isProcessing = inv.voyage.id in processing,
                                onAccept = { vm.accept(inv.voyage.id, onAccepted) },
                                onDecline = { vm.decline(inv.voyage.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitationCard(
    invitation: VoyageInvitation,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val v = invitation.voyage
    val imageUrl: String? = v.image?.takeIf { it.isNotBlank() }?.let {
        if (it.startsWith("http")) it else be.reveetvoyage.app.data.api.ApiConfig.SITE_BASE + it
    }
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0) {
        Column {
            // Image de couverture (si fournie).
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    v.titre ?: v.destination ?: "Voyage",
                    color = RevBrown, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2,
                )
                v.destination?.takeIf { it.isNotBlank() }?.let {
                    InvitationMetaRow(Icons.Default.Place, it)
                }
                val dates = formatInvitationDates(v.date_depart, v.date_retour)
                if (dates != null) InvitationMetaRow(Icons.Default.CalendarToday, dates)
                invitation.inviter_name?.takeIf { it.isNotBlank() }?.let {
                    InvitationMetaRow(Icons.Default.Person, "Invité par $it")
                }
                StatusBadge(invitationRoleLabel(invitation.role), BadgeKind.Brand)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IOSButton(
                        text = "Refuser",
                        onClick = onDecline,
                        style = IOSButtonStyle.Secondary,
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f),
                    )
                    IOSButton(
                        text = "Accepter",
                        onClick = onAccept,
                        style = IOSButtonStyle.Primary,
                        isLoading = isProcessing,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitationMetaRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = RevTextSecondary, modifier = Modifier.size(14.dp))
        Text(text, color = RevTextSecondary, fontSize = 12.sp, maxLines = 1)
    }
}

private fun invitationRoleLabel(role: String): String = when (role) {
    "owner" -> "Propriétaire"
    "admin" -> "Admin"
    "collaborator" -> "Collaborateur"
    "viewer" -> "Lecteur"
    else -> role.replaceFirstChar { it.uppercase() }
}

// Formate "YYYY-MM-DD" → "DD/MM/YYYY" et combine départ/retour si présents.
private fun formatInvitationDates(depart: String?, retour: String?): String? {
    val d = depart?.let { prettyDate(it) }
    val r = retour?.let { prettyDate(it) }
    return when {
        d != null && r != null -> "$d → $r"
        d != null -> "À partir du $d"
        else -> null
    }
}

private fun prettyDate(iso: String): String? {
    // Prend la partie date d'un éventuel "YYYY-MM-DD HH:MM:SS".
    val datePart = iso.take(10)
    val parts = datePart.split("-")
    if (parts.size != 3) return iso
    val (y, m, day) = parts
    if (y.length != 4) return iso
    return "$day/$m/$y"
}
