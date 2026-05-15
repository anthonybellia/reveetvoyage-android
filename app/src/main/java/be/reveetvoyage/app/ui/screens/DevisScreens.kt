package be.reveetvoyage.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import be.reveetvoyage.app.data.model.Devis
import be.reveetvoyage.app.data.repo.DevisRepository
import be.reveetvoyage.app.ui.components.*
import be.reveetvoyage.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevisViewModel @Inject constructor(private val repo: DevisRepository) : ViewModel() {
    private val _devis = MutableStateFlow<List<Devis>>(emptyList())
    val devis: StateFlow<List<Devis>> = _devis.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _isLoading.value = true
            _devis.value = runCatching { repo.list() }.getOrDefault(emptyList())
            _isLoading.value = false
        }
    }
}

@Composable
fun DevisScreen(vm: DevisViewModel = hiltViewModel()) {
    val devis by vm.devis.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    val pending = devis.filter { it.statut in listOf("nouveau", "en_cours") }
    val validated = devis.filter { it.statut == "valide" }
    val others = devis.filter { it.statut in listOf("refuse", "archive") }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(RevYellow.copy(alpha = .08f), RevBackground))
        )
    ) {
        when {
            isLoading && devis.isEmpty() -> LoadingFull()
            devis.isEmpty() -> EmptyState(
                icon = Icons.Default.Description,
                title = "Aucune demande de voyage",
                subtitle = "Tes futures demandes apparaîtront ici",
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    if (pending.isNotEmpty()) {
                        item { SectionTitle("En attente", Icons.Default.Schedule, "${pending.size}") }
                        items(pending, key = { "p${it.id}" }) { DevisCard(it) }
                    }
                    if (validated.isNotEmpty()) {
                        item { SectionTitle("Validés", Icons.Default.CheckCircle, "${validated.size}") }
                        items(validated, key = { "v${it.id}" }) { DevisCard(it) }
                    }
                    if (others.isNotEmpty()) {
                        item { SectionTitle("Archivés", Icons.Default.Archive, "${others.size}") }
                        items(others, key = { "o${it.id}" }) { DevisCard(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DevisCard(d: Devis) {
    val (statutLabel, statutKind) = devisStatutLabel(d.statut)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        d.titre_voyage ?: d.destination ?: d.destination_souhaitee ?: "Demande",
                        color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    )
                    (d.destination ?: d.destination_souhaitee)?.let {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Place, null, tint = RevTextSecondary, modifier = Modifier.size(12.dp))
                            Text(it, color = RevTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
                StatusBadge(statutLabel, statutKind)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                d.nb_personnes?.let { Chip(Icons.Default.Person, "$it") }
                d.duree?.let { Chip(Icons.Default.CalendarMonth, it) }
                d.type_voyage?.let { Chip(Icons.Default.Favorite, it.replace('_', ' ').replaceFirstChar(Char::titlecase)) }
            }
        }
    }
}

@Composable
private fun Chip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(RevOrange.copy(alpha = .10f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = RevOrange, modifier = Modifier.size(11.dp))
        Text(text, color = RevOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
