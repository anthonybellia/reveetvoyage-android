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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.random.Random
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
    private val notifier: be.reveetvoyage.app.notifications.NotificationScheduler,
    private val userRepo: be.reveetvoyage.app.data.repo.UserRepository,
) : ViewModel() {
    private val _voyage = MutableStateFlow<Voyage?>(null)
    val voyage: StateFlow<Voyage?> = _voyage.asStateFlow()
    private val _etapes = MutableStateFlow<List<VoyageEtape>>(emptyList())
    val etapes: StateFlow<List<VoyageEtape>> = _etapes.asStateFlow()
    private val _toggling = MutableStateFlow<Set<Int>>(emptySet())
    val toggling: StateFlow<Set<Int>> = _toggling.asStateFlow()

    fun load(id: Int) {
        viewModelScope.launch {
            runCatching { repo.detail(id) }.onSuccess { v ->
                _voyage.value = v
                _etapes.value = v.etapes.orEmpty()

                // Schedule local reminders if user opted in
                val notifEnabled = userRepo.currentUser.value?.notif_voyages ?: true
                if (notifEnabled) notifier.scheduleVoyage(v)
                else notifier.cancelVoyage(v.id)
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

// ============================================================
// VoyageHero — map + weather card overlay for the voyage destination
// ============================================================
@Composable
private fun VoyageHero(etape: VoyageEtape) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var coords by remember(etape.id) {
        mutableStateOf<Pair<Double, Double>?>(
            if (etape.hasCoordinates) Pair(etape.latitude!!, etape.longitude!!) else null
        )
    }
    val weatherVm: be.reveetvoyage.app.ui.components.WeatherViewModel = hiltViewModel(key = "weather-voyage-${etape.id}")
    val weather by weatherVm.weather.collectAsState()
    val weatherLoading by weatherVm.loading.collectAsState()
    val locationLabel by weatherVm.locationLabel.collectAsState()

    LaunchedEffect(etape.id, etape.adresse, etape.lieu) {
        if (coords == null) {
            val query = listOfNotNull(etape.adresse, etape.lieu)
                .filter { it.isNotBlank() }.joinToString(", ")
            if (query.isNotBlank()) {
                val resolved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val gc = android.location.Geocoder(context, java.util.Locale.getDefault())
                        @Suppress("DEPRECATION")
                        gc.getFromLocationName(query, 1)?.firstOrNull()?.let { Pair(it.latitude, it.longitude) }
                    } catch (t: Throwable) { null }
                }
                if (resolved != null) coords = resolved
            }
        }
        coords?.let { (lat, lng) ->
            val city = etape.lieu?.takeIf { it.isNotBlank() }
                ?: be.reveetvoyage.app.ui.components.reverseGeocodeCity(context, lat, lng)
                ?: etape.titre
            weatherVm.load(lat, lng, city)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        coords?.let { (lat, lng) ->
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                androidx.compose.ui.viewinterop.AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        org.osmdroid.views.MapView(ctx).apply {
                            setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                            setMultiTouchControls(false)
                            isClickable = false
                            controller.setZoom(11.0)
                            controller.setCenter(org.osmdroid.util.GeoPoint(lat, lng))
                            val marker = org.osmdroid.views.overlay.Marker(this)
                            marker.position = org.osmdroid.util.GeoPoint(lat, lng)
                            marker.setAnchor(
                                org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                                org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM,
                            )
                            overlays.add(marker)
                        }
                    },
                )
            }
        }
        Box(modifier = Modifier.padding(horizontal = 18.dp)) {
            be.reveetvoyage.app.ui.components.WeatherCard(
                weather = weather,
                locationLabel = locationLabel,
                isLoading = weatherLoading,
            )
        }
    }
}

// ============================================================
// Celebration overlay — particle burst when an étape is completed
// ============================================================
private data class CelebrationParticle(
    val id: Int,
    val icon: ImageVector,
    val color: Color,
    val initialDx: Float,
    val targetDx: Float,
    val targetDy: Float,
    val rotationDelta: Float,
    val finalScale: Float,
)

@Composable
private fun CelebrationOverlay(burst: Int) {
    if (burst == 0) return
    val density = LocalDensity.current
    val icons = remember {
        listOf(Icons.Default.Star, Icons.Default.Favorite, Icons.Default.AutoAwesome, Icons.Default.Flight)
    }
    val colors = remember { listOf(RevYellow, RevOrange, RevRed) }

    val particles = remember(burst) {
        (0 until 18).map { idx ->
            CelebrationParticle(
                id = idx,
                icon = icons[Random.nextInt(icons.size)],
                color = colors[Random.nextInt(colors.size)],
                initialDx = Random.nextFloat() * 60f - 30f,
                targetDx = Random.nextFloat() * 360f - 180f,
                targetDy = -(Random.nextFloat() * 220f + 80f),
                rotationDelta = Random.nextFloat() * 540f + 180f,
                finalScale = Random.nextFloat() * 0.6f + 0.8f,
            )
        }
    }

    val animation = remember(burst) { Animatable(0f) }
    LaunchedEffect(burst) {
        animation.snapTo(0f)
        animation.animateTo(1f, animationSpec = tween(durationMillis = 1100, easing = EaseOutCubic))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val centerXpx = with(density) { (maxWidth / 2).toPx() }
            val centerYpx = with(density) { (maxHeight / 2).toPx() }
            val progress = animation.value

            particles.forEach { p ->
                val dxNow  = p.initialDx + (p.targetDx - p.initialDx) * progress
                val dyNow  = p.targetDy * progress
                val xPx    = centerXpx + with(density) { dxNow.dp.toPx() }
                val yPx    = centerYpx + with(density) { dyNow.dp.toPx() }
                val scale  = 0.4f + (p.finalScale - 0.4f) * progress
                val rot    = p.rotationDelta * progress
                val alpha  = (1f - progress).coerceIn(0f, 1f)

                Icon(
                    imageVector = p.icon,
                    contentDescription = null,
                    tint = p.color.copy(alpha = alpha),
                    modifier = Modifier
                        .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                        .scale(scale)
                        .rotate(rot)
                        .size(28.dp)
                )
            }
        }
    }
}

@Composable
fun VoyageDetailScreen(
    voyageId: Int,
    onBack: () -> Unit,
    onOpenEtape: (Int) -> Unit = {},
    onOpenExpenses: (Int) -> Unit = {},
    vm: VoyageDetailViewModel = hiltViewModel(),
) {
    val voyage by vm.voyage.collectAsState()
    val etapes by vm.etapes.collectAsState()
    val toggling by vm.toggling.collectAsState()
    var pendingToggle by remember { mutableStateOf<VoyageEtape?>(null) }
    var celebrationBurst by remember { mutableStateOf(0) }
    var previousCompletedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(etapes) {
        val nowCompleted = etapes.filter { it.is_completed }.map { it.id }.toSet()
        if (previousCompletedIds.isNotEmpty() && (nowCompleted - previousCompletedIds).isNotEmpty()) {
            celebrationBurst++
        }
        previousCompletedIds = nowCompleted
    }

    LaunchedEffect(voyageId) { vm.load(voyageId) }

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        be.reveetvoyage.app.ui.components.IOSTopBar(
            title = voyage?.reference ?: "Voyage",
            onBack = onBack,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(RevYellow.copy(alpha = .10f), RevBackground))),
        ) {
            voyage?.let { v ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    val firstGeoEtape = etapes.firstOrNull { it.hasCoordinates }
                        ?: etapes.firstOrNull { !it.adresse.isNullOrBlank() || !it.lieu.isNullOrBlank() }
                    if (firstGeoEtape != null) {
                        VoyageHero(etape = firstGeoEtape)
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                    HeaderCard(v)
                    ProgressCard(done = etapes.count { it.is_completed }, total = etapes.size, value = vm.progress)
                    IOSButton(
                        text = "Dépenses partagées",
                        onClick = { onOpenExpenses(voyageId) },
                        icon = Icons.Default.Receipt,
                        style = IOSButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                                onToggle = { pendingToggle = etape },
                                onOpenDetail = { onOpenEtape(etape.id) },
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    }
                }
            } ?: LoadingFull()
        }

        CelebrationOverlay(burst = celebrationBurst)

        // Confirmation dialog (iOS-style)
        pendingToggle?.let { e ->
            be.reveetvoyage.app.ui.components.IOSAlertDialog(
                title = if (e.is_completed) "Marquer non effectuée ?" else "As-tu bien réalisé cette étape ?",
                message = if (!e.is_completed) "Tu peux passer à l'étape suivante. On te rappellera les prochaines." else null,
                confirmText = if (e.is_completed) "Marquer non effectuée" else "Oui, c'est fait",
                cancelText = "Annuler",
                isDestructive = e.is_completed,
                onConfirm = {
                    val wasCompleted = e.is_completed
                    vm.toggle(voyageId, e); pendingToggle = null
                    if (!wasCompleted) celebrationBurst++
                },
                onDismiss = { pendingToggle = null },
            )
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
            v.participants?.takeIf { it.isNotEmpty() }?.let { ps ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Group, null, tint = RevOrange, modifier = Modifier.size(11.dp))
                        Text("VOYAGEURS (${ps.size})", color = RevOrange,
                             fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(ps) { name ->
                            Row(modifier = Modifier
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(RevYellow.copy(alpha = .25f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Default.Person, null, tint = RevBrown, modifier = Modifier.size(12.dp))
                                Text(name, color = RevBrown, fontSize = 12.sp,
                                     fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
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
    onOpenDetail: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(modifier = Modifier.width(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.width(2.dp).height(14.dp)
                .background(if (isFirst) Color.Transparent else RevOrange.copy(alpha = .3f)))
            CheckBubble(etape, isToggling, onToggle)
            Box(modifier = Modifier.width(2.dp).weight(1f).heightIn(min = 30.dp)
                .background(if (isLast) Color.Transparent else RevOrange.copy(alpha = .3f)))
        }
        EtapeContent(
            etape,
            modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 12.dp)
                .clickable(onClick = onOpenDetail),
        )
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
