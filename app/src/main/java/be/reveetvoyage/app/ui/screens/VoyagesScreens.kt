package be.reveetvoyage.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.model.Voyage
import be.reveetvoyage.app.data.model.VoyageEtape
import be.reveetvoyage.app.data.repo.VoyageRepository
import be.reveetvoyage.app.ui.components.*
import be.reveetvoyage.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================
// VoyagesScreen — list with filter chips (All/Upcoming/Past)
// ============================================================
@HiltViewModel
class VoyagesViewModel @Inject constructor(private val repo: VoyageRepository) : ViewModel() {
    private val _voyages = MutableStateFlow<List<Voyage>>(emptyList())
    val voyages: StateFlow<List<Voyage>> = _voyages.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _isLoading.value = true
            _voyages.value = runCatching { repo.list() }.getOrDefault(emptyList())
            _isLoading.value = false
        }
    }
}

private enum class VoyageFilter(val label: String) {
    All("Tous"), Upcoming("À venir"), Past("Terminés");
    fun apply(v: Voyage): Boolean = when (this) {
        All -> true
        Upcoming -> v.statut in listOf("en_preparation", "confirme", "en_cours")
        Past -> v.statut in listOf("termine", "annule")
    }
}

@Composable
fun VoyagesScreen(onOpenVoyage: (Int) -> Unit, vm: VoyagesViewModel = hiltViewModel()) {
    val voyages by vm.voyages.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    var filter by remember { mutableStateOf(VoyageFilter.All) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(RevYellow.copy(alpha = .08f), RevBackground)))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            // Filter chips
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VoyageFilter.values().forEach { f ->
                    val selected = f == filter
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (selected) Brush.horizontalGradient(listOf(RevOrange, RevRed))
                                else Brush.horizontalGradient(listOf(RevCardBackground, RevCardBackground))
                            )
                            .clickable { filter = f }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            f.label,
                            color = if (selected) Color.White else RevBrown,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            when {
                isLoading && voyages.isEmpty() -> LoadingFull()
                voyages.isEmpty() -> EmptyState(
                    icon = Icons.Default.Flight,
                    title = "Aucun voyage pour le moment",
                )
                else -> {
                    val filtered = voyages.filter(filter::apply)
                    if (filtered.isEmpty()) {
                        EmptyState(Icons.Default.Flight, "Aucun voyage ${filter.label.lowercase()}")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                        ) {
                            items(filtered, key = { it.id }) { v ->
                                VoyageCard(v, onClick = { onOpenVoyage(v.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoyageCard(v: Voyage, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(
                    Brush.linearGradient(listOf(RevYellow.copy(alpha = .6f), RevOrange.copy(alpha = .6f)))
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Flight, null, tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(v.titre, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(v.destination, color = RevTextSecondary, fontSize = 12.sp)
            }
            StatusBadge(v.statut_label, voyageStatutKind(v.statut))
            Icon(Icons.Default.ChevronRight, null, tint = RevTextSecondary.copy(alpha = .5f))
        }
    }
}

// ============================================================
// VoyageDetailScreen — header + progress + timeline étapes cochables
// ============================================================
@HiltViewModel
class VoyageDetailViewModel @Inject constructor(
    private val repo: VoyageRepository,
) : ViewModel() {
    private val _voyage = MutableStateFlow<Voyage?>(null)
    val voyage: StateFlow<Voyage?> = _voyage.asStateFlow()
    private val _etapes = MutableStateFlow<List<VoyageEtape>>(emptyList())
    val etapes: StateFlow<List<VoyageEtape>> = _etapes.asStateFlow()
    private val _toggling = MutableStateFlow<Set<Int>>(emptySet())
    val toggling: StateFlow<Set<Int>> = _toggling.asStateFlow()

    fun load(id: Int) {
        viewModelScope.launch {
            runCatching { repo.detail(id) }.onSuccess {
                _voyage.value = it
                _etapes.value = it.etapes.orEmpty()
            }
        }
    }

    fun toggle(voyageId: Int, etape: VoyageEtape) {
        viewModelScope.launch {
            _toggling.value = _toggling.value + etape.id
            // optimistic
            _etapes.value = _etapes.value.map {
                if (it.id == etape.id) it.copy(is_completed = !it.is_completed) else it
            }
            runCatching { repo.toggleEtape(voyageId, etape.id) }
                .onSuccess { updated ->
                    _etapes.value = _etapes.value.map { if (it.id == updated.id) updated else it }
                }
                .onFailure {
                    // revert
                    _etapes.value = _etapes.value.map {
                        if (it.id == etape.id) it.copy(is_completed = !it.is_completed) else it
                    }
                }
            _toggling.value = _toggling.value - etape.id
        }
    }

    val progress: Float
        get() = if (_etapes.value.isEmpty()) 0f
        else _etapes.value.count { it.is_completed }.toFloat() / _etapes.value.size
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoyageDetailScreen(voyageId: Int, onBack: () -> Unit, vm: VoyageDetailViewModel = hiltViewModel()) {
    val voyage by vm.voyage.collectAsState()
    val etapes by vm.etapes.collectAsState()
    val toggling by vm.toggling.collectAsState()

    LaunchedEffect(voyageId) { vm.load(voyageId) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(voyage?.reference ?: "Voyage", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.linearGradient(listOf(RevYellow.copy(alpha = .10f), RevBackground)))
        ) {
            voyage?.let { v ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HeaderCard(v)
                    ProgressCard(done = etapes.count { it.is_completed }, total = etapes.size, value = vm.progress)
                    SectionTitle("Étapes du voyage", Icons.AutoMirrored.Filled.List)
                    if (etapes.isEmpty()) {
                        GlassCard {
                            Text("Aucune étape n'a été ajoutée à ce voyage.",
                                 color = RevTextSecondary,
                                 modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                        }
                    } else {
                        etapes.forEachIndexed { index, etape ->
                            EtapeRow(
                                etape = etape,
                                isFirst = index == 0,
                                isLast = index == etapes.lastIndex,
                                isToggling = etape.id in toggling,
                                onToggle = { vm.toggle(v.id, etape) }
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            } ?: LoadingFull()
        }
    }
}

@Composable
private fun HeaderCard(v: Voyage) {
    GlassCard(padding = 18) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(v.titre, color = RevBrown, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Place, null, tint = RevTextSecondary, modifier = Modifier.size(16.dp))
                        Text(v.destination, color = RevTextSecondary, fontSize = 14.sp)
                    }
                }
                StatusBadge(v.statut_label, voyageStatutKind(v.statut))
            }
            if (v.date_depart != null || v.date_retour != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    DateChip("Départ", v.date_depart, Icons.Default.FlightTakeoff)
                    DateChip("Retour", v.date_retour, Icons.Default.FlightLand)
                }
            }
            if (v.montant_total > 0) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CreditCard, null, tint = RevOrange, modifier = Modifier.size(16.dp))
                    Column {
                        Text("€${v.montant_paye.toInt()} / €${v.montant_total.toInt()}",
                             color = RevBrown, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Acompte : €${v.montant_acompte.toInt()}",
                             color = RevTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateChip(label: String, iso: String?, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = RevOrange, modifier = Modifier.size(12.dp))
            Text(label.uppercase(), color = RevOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(iso?.take(10) ?: "—", color = RevBrown, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProgressCard(done: Int, total: Int, value: Float) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progression du planning", color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("$done / $total", color = RevOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            LinearProgressIndicator(
                progress = { value.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = RevOrange,
                trackColor = Color.Gray.copy(alpha = .15f),
            )
        }
    }
}

@Composable
private fun EtapeRow(
    etape: VoyageEtape,
    isFirst: Boolean,
    isLast: Boolean,
    isToggling: Boolean,
    onToggle: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(modifier = Modifier.width(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.width(2.dp).height(14.dp)
                .background(if (isFirst) Color.Transparent else RevOrange.copy(alpha = .3f)))
            CheckBubble(etape, isToggling, onToggle)
            Box(modifier = Modifier.width(2.dp).weight(1f).heightIn(min = 30.dp)
                .background(if (isLast) Color.Transparent else RevOrange.copy(alpha = .3f)))
        }
        EtapeContent(etape, modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 12.dp))
    }
}

@Composable
private fun CheckBubble(etape: VoyageEtape, isToggling: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(
                if (etape.is_completed) Brush.linearGradient(listOf(RevOrange, RevRed))
                else Brush.linearGradient(listOf(RevCardBackground, RevCardBackground))
            )
            .clickable(enabled = !isToggling, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isToggling -> CircularProgressIndicator(modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp, color = if (etape.is_completed) Color.White else RevOrange)
            etape.is_completed -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            else -> Icon(stepIcon(etape.type), null, tint = RevOrange, modifier = Modifier.size(14.dp))
        }
    }
}

private fun stepIcon(type: String) = when (type) {
    "vol", "vol_aller", "vol_retour" -> Icons.Default.Flight
    "hotel" -> Icons.Default.Hotel
    "activite" -> Icons.Default.DirectionsWalk
    "transfert" -> Icons.Default.DirectionsCar
    "restaurant" -> Icons.Default.Restaurant
    "note" -> Icons.Default.Description
    "document" -> Icons.Default.InsertDriveFile
    else -> Icons.Default.Circle
}

@Composable
private fun EtapeContent(etape: VoyageEtape, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.alpha(if (etape.is_completed) 0.75f else 1f), padding = 14) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        etape.titre,
                        color = if (etape.is_completed) RevTextSecondary else RevBrown,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        textDecoration = if (etape.is_completed) TextDecoration.LineThrough else TextDecoration.None,
                    )
                    etape.lieu?.takeIf { it.isNotBlank() }?.let {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Place, null, tint = RevTextSecondary, modifier = Modifier.size(11.dp))
                            Text(it, color = RevTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
                etape.date?.take(10)?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(it, color = RevOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        etape.heure?.let { h -> Text(h, color = RevTextSecondary, fontSize = 10.sp) }
                    }
                }
            }
            etape.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = RevTextSecondary, fontSize = 12.sp, maxLines = 3)
            }
            if (etape.cout != null && etape.cout > 0) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Euro, null, tint = RevOrange, modifier = Modifier.size(11.dp))
                    Text("${etape.cout.toInt()} €", color = RevOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
