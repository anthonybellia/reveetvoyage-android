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
import androidx.compose.ui.layout.ContentScale
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
import be.reveetvoyage.app.data.api.ApiConfig
import be.reveetvoyage.app.data.model.User
import be.reveetvoyage.app.data.model.Voyage
import be.reveetvoyage.app.data.model.VoyageEtape
import be.reveetvoyage.app.data.repo.UserRepository
import be.reveetvoyage.app.data.repo.VoyageRepository
import be.reveetvoyage.app.ui.components.*
import be.reveetvoyage.app.ui.screens.admin.AdminBanner
import be.reveetvoyage.app.ui.screens.admin.OwnerRow
import be.reveetvoyage.app.ui.theme.*
import coil.compose.AsyncImage
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
class VoyagesViewModel @Inject constructor(
    private val repo: VoyageRepository,
    userRepo: UserRepository,
) : ViewModel() {
    private val _voyages = MutableStateFlow<List<Voyage>>(emptyList())
    val voyages: StateFlow<List<Voyage>> = _voyages.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val currentUser: StateFlow<User?> = userRepo.currentUser
    // Admin "effectif" : vrai admin ET pas en mode aperçu utilisateur.
    val isAdmin: StateFlow<Boolean> = userRepo.isAdmin

    init {
        reload()
        // Recharge automatiquement quand un refresh global est demandé
        // (ex : acceptation d'une invitation depuis un autre écran).
        viewModelScope.launch {
            repo.refreshSignal.collect { signal -> if (signal > 0) reload() }
        }
    }

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
    val user by vm.currentUser.collectAsState()
    val isAdmin by vm.isAdmin.collectAsState()
    var filter by remember { mutableStateOf(VoyageFilter.All) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(RevYellow.copy(alpha = .08f), RevBackground)))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            if (isAdmin) {
                Box(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
                    AdminBanner()
                }
            }
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
                                VoyageCard(v, isAdmin = isAdmin, onClick = { onOpenVoyage(v.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoyageCard(v: Voyage, isAdmin: Boolean = false, onClick: () -> Unit) {
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
                // Ligne propriétaire réservée à l'admin (masquée en mode aperçu utilisateur).
                if (isAdmin) OwnerRow(owner = v.owner)
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

    // ===== Membres / collaboration =====
    val currentUser: StateFlow<User?> = userRepo.currentUser
    // Admin "effectif" : vrai admin ET pas en mode aperçu utilisateur.
    val isAdmin: StateFlow<Boolean> = userRepo.isAdmin
    private val _members = MutableStateFlow<List<be.reveetvoyage.app.data.model.VoyageMember>>(emptyList())
    val members: StateFlow<List<be.reveetvoyage.app.data.model.VoyageMember>> = _members.asStateFlow()
    private val _pending = MutableStateFlow<List<be.reveetvoyage.app.data.model.PendingInvite>>(emptyList())
    val pending: StateFlow<List<be.reveetvoyage.app.data.model.PendingInvite>> = _pending.asStateFlow()
    private val _membersLoading = MutableStateFlow(false)
    val membersLoading: StateFlow<Boolean> = _membersLoading.asStateFlow()
    private val _inviting = MutableStateFlow(false)
    val inviting: StateFlow<Boolean> = _inviting.asStateFlow()
    // Feedback éphémère affiché sous le champ d'invitation (succès ou erreur).
    private val _memberFeedback = MutableStateFlow<String?>(null)
    val memberFeedback: StateFlow<String?> = _memberFeedback.asStateFlow()
    private val _removingMemberIds = MutableStateFlow<Set<Int>>(emptySet())
    val removingMemberIds: StateFlow<Set<Int>> = _removingMemberIds.asStateFlow()

    // ===== Autocomplete invitation =====
    // Compte trouvé pour l'email saisi (carte de confirmation), null sinon.
    private val _inviteMatch = MutableStateFlow<be.reveetvoyage.app.data.model.UserSearchResult?>(null)
    val inviteMatch: StateFlow<be.reveetvoyage.app.data.model.UserSearchResult?> = _inviteMatch.asStateFlow()
    private val _inviteSearching = MutableStateFlow(false)
    val inviteSearching: StateFlow<Boolean> = _inviteSearching.asStateFlow()
    private var searchJob: kotlinx.coroutines.Job? = null

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
        loadMembers(id)
    }

    fun loadMembers(voyageId: Int) {
        viewModelScope.launch {
            _membersLoading.value = true
            runCatching { repo.members(voyageId) }.onSuccess { resp ->
                _members.value = resp.members
                _pending.value = resp.pending
            }
            _membersLoading.value = false
        }
    }

    fun invite(voyageId: Int, email: String) {
        val cleaned = email.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            _inviting.value = true
            _memberFeedback.value = null
            when (val res = repo.inviteMember(voyageId, cleaned)) {
                is be.reveetvoyage.app.data.repo.InviteResult.Success -> {
                    _memberFeedback.value = "Invitation traitée."
                    clearInviteSearch()
                    loadMembers(voyageId)
                }
                is be.reveetvoyage.app.data.repo.InviteResult.Forbidden ->
                    _memberFeedback.value = "Action réservée au propriétaire."
                is be.reveetvoyage.app.data.repo.InviteResult.Conflict ->
                    _memberFeedback.value = res.message
                is be.reveetvoyage.app.data.repo.InviteResult.Error ->
                    _memberFeedback.value = res.message
            }
            _inviting.value = false
        }
    }

    fun clearMemberFeedback() { _memberFeedback.value = null }

    // Recherche debouncée (~400ms) d'un compte par email exact pendant la saisie.
    // Met à jour _inviteMatch (carte de confirmation) ou le remet à null.
    fun onInviteEmailChanged(email: String) {
        val cleaned = email.trim()
        searchJob?.cancel()
        if (!cleaned.contains('@') || cleaned.length < 5) {
            _inviteMatch.value = null
            _inviteSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _inviteSearching.value = true
            kotlinx.coroutines.delay(400)
            _inviteMatch.value = repo.searchUser(cleaned)
            _inviteSearching.value = false
        }
    }

    fun clearInviteSearch() {
        searchJob?.cancel()
        _inviteMatch.value = null
        _inviteSearching.value = false
    }

    fun removeMember(voyageId: Int, userId: Int) {
        viewModelScope.launch {
            _removingMemberIds.value = _removingMemberIds.value + userId
            runCatching { repo.removeMember(voyageId, userId) }
                .onSuccess { loadMembers(voyageId) }
                .onFailure { _memberFeedback.value = "Suppression impossible." }
            _removingMemberIds.value = _removingMemberIds.value - userId
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
    onOpenPacking: (Int) -> Unit = {},
    vm: VoyageDetailViewModel = hiltViewModel(),
) {
    val voyage by vm.voyage.collectAsState()
    val etapes by vm.etapes.collectAsState()
    val toggling by vm.toggling.collectAsState()
    val currentUser by vm.currentUser.collectAsState()
    val isAdmin by vm.isAdmin.collectAsState()
    val members by vm.members.collectAsState()
    val pending by vm.pending.collectAsState()
    val membersLoading by vm.membersLoading.collectAsState()
    val inviting by vm.inviting.collectAsState()
    val memberFeedback by vm.memberFeedback.collectAsState()
    val removingMemberIds by vm.removingMemberIds.collectAsState()
    val inviteMatch by vm.inviteMatch.collectAsState()
    val inviteSearching by vm.inviteSearching.collectAsState()
    var pendingToggle by remember { mutableStateOf<VoyageEtape?>(null) }
    var celebrationBurst by remember { mutableStateOf(0) }
    var previousCompletedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Invite d'arrivée géolocalisée (Feature 2) : étape la plus proche, non
    // terminée, dans un rayon de 200 m, non encore proposée dans la session.
    var arrivalEtape by remember { mutableStateOf<VoyageEtape?>(null) }
    var arrivalHandled by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val arrivalContext = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(etapes) {
        val nowCompleted = etapes.filter { it.is_completed }.map { it.id }.toSet()
        if (previousCompletedIds.isNotEmpty() && (nowCompleted - previousCompletedIds).isNotEmpty()) {
            celebrationBurst++
        }
        previousCompletedIds = nowCompleted

        if (arrivalEtape == null) {
            val here = LocationHelper.lastKnownLocation(arrivalContext)
            if (here != null) {
                arrivalEtape = etapes
                    .filter { !it.is_completed && it.id !in arrivalHandled && it.latitude != null && it.longitude != null }
                    .map { it to haversineMeters(here.first, here.second, it.latitude!!, it.longitude!!) }
                    .filter { it.second <= 200.0 }
                    .minByOrNull { it.second }?.first
            }
        }
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
                    // Récapitulatif de tous les billets du voyage (toutes étapes confondues).
                    TicketsRecapCard(etapes = etapes)
                    IOSButton(
                        text = "Dépenses partagées",
                        onClick = { onOpenExpenses(voyageId) },
                        icon = Icons.Default.Receipt,
                        style = IOSButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    IOSButton(
                        text = "Liste de bagage",
                        onClick = { onOpenPacking(voyageId) },
                        icon = Icons.Default.Inventory2,
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
                            // Trajet inter-étapes (N → N+1), affiché entre deux
                            // étapes consécutives, comme sur le web.
                            if (index < etapes.lastIndex && etape.hasConnector) {
                                TimelineConnector(etape)
                            }
                        }
                    }

                    // ===== Section Voyageurs / Membres (collaboration) =====
                    // En mode aperçu utilisateur, l'admin perd les contrôles de gestion
                    // (invitation / retrait) mais reste propriétaire si c'est son voyage.
                    val isOwner = v.owner?.id != null && v.owner?.id == currentUser?.id
                    VoyageMembersSection(
                        members = members,
                        pending = pending,
                        isLoading = membersLoading,
                        canManage = isOwner || isAdmin,
                        currentUserId = currentUser?.id,
                        inviting = inviting,
                        feedback = memberFeedback,
                        removingMemberIds = removingMemberIds,
                        inviteMatch = inviteMatch,
                        inviteSearching = inviteSearching,
                        onInvite = { email -> vm.invite(voyageId, email) },
                        onEmailChanged = { email -> vm.onInviteEmailChanged(email) },
                        onClearFeedback = { vm.clearMemberFeedback() },
                        onRemove = { userId -> vm.removeMember(voyageId, userId) },
                    )

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

        // Invite d'arrivée géolocalisée (Feature 2).
        arrivalEtape?.let { e ->
            be.reveetvoyage.app.ui.components.IOSAlertDialog(
                title = "Vous êtes arrivé à ${e.titre} ?",
                message = e.lieu?.takeIf { it.isNotBlank() }
                    ?.let { "Vous semblez être à proximité de $it." }
                    ?: "Vous semblez être à proximité de cette étape.",
                confirmText = "Oui, marquer terminée",
                cancelText = "Pas encore",
                isDestructive = false,
                onConfirm = {
                    val wasCompleted = e.is_completed
                    vm.toggle(voyageId, e)
                    if (!wasCompleted) celebrationBurst++
                    arrivalHandled = arrivalHandled + e.id
                    arrivalEtape = null
                },
                onDismiss = {
                    arrivalHandled = arrivalHandled + e.id
                    arrivalEtape = null
                },
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
            OwnerRow(owner = v.owner)
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

// ============================================================
// TicketsRecapCard — récapitulatif de tous les billets du voyage
// (agrège les tickets de toutes les étapes). Chaque ligne montre une
// icône (PDF / image), le titre de l'étape parente et ouvre le billet.
// Masquée s'il n'y a aucun billet.
// ============================================================
// Distance en mètres entre deux points GPS (formule de Haversine).
private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2)
    return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

// TimelineConnector — label de trajet inter-étapes (N → N+1) affiché
// entre deux étapes consécutives dans la timeline : icône transport +
// mode · distance · durée. Calqué sur le rendu web.
@Composable
private fun TimelineConnector(etape: VoyageEtape) {
    val icon = when (etape.connector_mode) {
        "car" -> Icons.Default.DirectionsCar
        "train" -> Icons.Default.Train
        "plane" -> Icons.Default.Flight
        "bus" -> Icons.Default.DirectionsBus
        "navette" -> Icons.Default.AirportShuttle
        "taxi" -> Icons.Default.LocalTaxi
        "walk" -> Icons.Default.DirectionsWalk
        else -> Icons.Default.Place
    }
    val label = when (etape.connector_mode) {
        "car" -> "Voiture"
        "train" -> "Train"
        "plane" -> "Avion"
        "bus" -> "Bus"
        "navette" -> "Navette"
        "taxi" -> "Taxi"
        "walk" -> "À pied"
        else -> "Trajet"
    }
    // La distance peut arriver avec un séparateur résiduel ("1061 km · ").
    val distance = etape.connector_distance?.trim()?.trim('·', ' ')?.takeIf { it.isNotBlank() }
    val duration = etape.connector_duration?.trim()?.takeIf { it.isNotBlank() }
    val text = listOfNotNull(label, distance, duration).joinToString(" · ")

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(50), color = RevOrange.copy(alpha = 0.10f)) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = RevTextSecondary, modifier = Modifier.size(14.dp))
                Text(text, color = RevTextSecondary, fontSize = 11.sp,
                    fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TicketsRecapCard(etapes: List<VoyageEtape>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Aplatit tous les billets en gardant le titre de l'étape parente.
    val allTickets = etapes.flatMap { etape ->
        etape.tickets.map { ticket -> etape to ticket }
    }
    if (allTickets.isEmpty()) return

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Billets du voyage", Icons.Default.ConfirmationNumber)
            allTickets.forEach { (etape, ticket) ->
                // Visuel : photo de couverture de l'étape si disponible,
                // sinon icône du type d'étape.
                val coverUrl = (etape.cover ?: etape.image)?.takeIf { it.isNotBlank() }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = RevOrange.copy(alpha = 0.08f),
                    onClick = { openEtapeUrl(context, ticket.url) },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (coverUrl != null) {
                            AsyncImage(
                                model = resolveEtapeUrl(coverUrl),
                                contentDescription = etape.titre,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                    .background(RevOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(stepIcon(etape.type), null, tint = RevOrange, modifier = Modifier.size(22.dp))
                            }
                        }
                        // Nom de l'étape + passager attribué (plus de nom de fichier / token).
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                etape.titre,
                                color = RevBrown, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                maxLines = 1,
                            )
                            ticket.participant_name?.takeIf { it.isNotBlank() }?.let { pax ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Default.Person, null, tint = RevOrange,
                                        modifier = Modifier.size(12.dp))
                                    Text(pax, color = RevOrange, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                        Icon(Icons.Default.OpenInNew, null, tint = RevOrange, modifier = Modifier.size(20.dp))
                    }
                }
            }
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
            onOpenDetail = onOpenDetail,
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

// Icône Material pour chaque type d'étape (jeu complet aligné sur le web).
private fun stepIcon(type: String): ImageVector = when (type) {
    "vol", "vol_aller", "vol_retour" -> Icons.Default.Flight
    "train" -> Icons.Default.Train
    "hotel" -> Icons.Default.Hotel
    "activite" -> Icons.Default.DirectionsWalk
    "restaurant" -> Icons.Default.Restaurant
    "brunch" -> Icons.Default.BrunchDining
    "petit_dej" -> Icons.Default.BakeryDining
    "cafe" -> Icons.Default.LocalCafe
    "bar" -> Icons.Default.LocalBar
    "transfert" -> Icons.Default.DirectionsCar
    "visite" -> Icons.Default.Museum
    "monument" -> Icons.Default.AccountBalance
    "croisiere" -> Icons.Default.DirectionsBoat
    "note" -> Icons.Default.Description
    "spa" -> Icons.Default.Spa
    "shopping" -> Icons.Default.ShoppingBag
    "plage" -> Icons.Default.BeachAccess
    "sport" -> Icons.Default.DirectionsRun
    "spectacle" -> Icons.Default.TheaterComedy
    "document" -> Icons.Default.InsertDriveFile
    else -> Icons.Default.Place
}

// Libellé français pour chaque type d'étape.
private fun stepLabel(type: String): String = when (type) {
    "vol", "vol_aller" -> "Vol aller"
    "vol_retour" -> "Vol retour"
    "train" -> "Train"
    "hotel" -> "Hôtel"
    "activite" -> "Activité"
    "restaurant" -> "Restaurant"
    "brunch" -> "Brunch"
    "petit_dej" -> "Petit-déjeuner"
    "cafe" -> "Café"
    "bar" -> "Bar"
    "transfert" -> "Transfert"
    "visite" -> "Visite"
    "monument" -> "Monument"
    "croisiere" -> "Croisière"
    "note" -> "Note"
    "spa" -> "Spa"
    "shopping" -> "Shopping"
    "plage" -> "Plage"
    "sport" -> "Sport"
    "spectacle" -> "Spectacle"
    "document" -> "Document"
    else -> type.replaceFirstChar { it.uppercase() }
}

@Composable
private fun EtapeContent(
    etape: VoyageEtape,
    onOpenDetail: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Cover de l'étape : on privilégie `cover`, sinon on retombe sur `image`.
    val coverUrl = (etape.cover ?: etape.image)?.takeIf { it.isNotBlank() }
    GlassCard(modifier = modifier.alpha(if (etape.is_completed) 0.75f else 1f), padding = 14) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Vignette de couverture (à la place d'une carte si présente),
                // taille/placement proches de la mini-carte.
                if (coverUrl != null) {
                    AsyncImage(
                        model = resolveEtapeUrl(coverUrl),
                        contentDescription = etape.titre,
                        modifier = Modifier
                            .size(width = 64.dp, height = 64.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
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
                // Badge billet : indique la présence de tickets attachés.
                if (etape.hasTickets) {
                    Icon(
                        Icons.Default.ConfirmationNumber,
                        contentDescription = "Billets",
                        tint = RevOrange,
                        modifier = Modifier.size(18.dp),
                    )
                }
                etape.date?.take(10)?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(it, color = RevOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        // Heure optionnelle : on n'affiche rien (ni séparateur) si elle est vide.
                        etape.heure?.takeIf { h -> h.isNotBlank() }?.let { h ->
                            Text(h, color = RevTextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
            // Aperçu note : description sinon contenu_html nettoyé de ses balises,
            // pour ne pas masquer les étapes dont le contenu vit dans contenu_html.
            etape.notePreview.takeIf { it.isNotBlank() }?.let {
                Text(it, color = RevTextSecondary, fontSize = 12.sp, maxLines = 3)
            }
            if (etape.cout != null && etape.cout > 0) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Euro, null, tint = RevOrange, modifier = Modifier.size(11.dp))
                    Text("${etape.cout.toInt()} €", color = RevOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Boutons rapides : ouvrir le premier billet, ou afficher les infos détaillées.
            val firstTicketUrl = etape.tickets.firstOrNull()?.url
            val hasInfos = etape.notePreview.isNotBlank()
            if (firstTicketUrl != null || hasInfos) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (firstTicketUrl != null) {
                        EtapeChip(
                            icon = Icons.Default.ConfirmationNumber,
                            label = "Billet",
                            onClick = { openEtapeUrl(context, firstTicketUrl) },
                        )
                    }
                    if (hasInfos) {
                        EtapeChip(
                            icon = Icons.Default.Info,
                            label = "Infos",
                            onClick = onOpenDetail,
                        )
                    }
                }
            }
        }
    }
}

// Petite puce cliquable utilisée dans la ligne d'étape (billet / infos).
@Composable
private fun EtapeChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(RevOrange.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = RevOrange, modifier = Modifier.size(14.dp))
        Text(label, color = RevOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Résout une URL relative renvoyée par l'API en URL absolue.
private fun resolveEtapeUrl(url: String): String =
    if (url.startsWith("http")) url else ApiConfig.SITE_BASE + url

// Ouvre une URL de billet/document via ACTION_VIEW (lecteur PDF / navigateur).
private fun openEtapeUrl(context: android.content.Context, url: String) {
    val resolved = resolveEtapeUrl(url)
    try {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(resolved),
        ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context, "Impossible d'ouvrir le billet", android.widget.Toast.LENGTH_SHORT,
        ).show()
    }
}

// ============================================================
// VoyageMembersSection — invitation + liste des membres / invitations
// Le champ d'invitation et les boutons de suppression ne s'affichent
// que pour le propriétaire du voyage ou un admin (canManage).
// ============================================================
@Composable
fun VoyageMembersSection(
    members: List<be.reveetvoyage.app.data.model.VoyageMember>,
    pending: List<be.reveetvoyage.app.data.model.PendingInvite>,
    isLoading: Boolean,
    canManage: Boolean,
    currentUserId: Int?,
    inviting: Boolean,
    feedback: String?,
    removingMemberIds: Set<Int>,
    inviteMatch: be.reveetvoyage.app.data.model.UserSearchResult?,
    inviteSearching: Boolean,
    onInvite: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onClearFeedback: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    // Vrai si l'email saisi ressemble à une adresse complète (heuristique simple).
    val emailLooksValid = email.contains('@') && email.contains('.') && email.length >= 5

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Voyageurs & membres", Icons.Default.Group)

        // Bloc d'invitation : visible uniquement pour le propriétaire / admin.
        if (canManage) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Inviter une personne par e-mail",
                        color = RevBrown,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    be.reveetvoyage.app.ui.components.IOSTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (feedback != null) onClearFeedback()
                            onEmailChanged(it)
                        },
                        placeholder = "adresse@email.com",
                        icon = Icons.Default.Email,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                        ),
                    )

                    // Aperçu autocomplete : compte trouvé, recherche en cours, ou
                    // proposition d'invitation par email si aucun compte exact.
                    when {
                        inviteSearching -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp, color = RevOrange,
                                )
                                Text("Recherche du compte…", color = RevTextSecondary, fontSize = 12.sp)
                            }
                        }
                        inviteMatch != null -> {
                            InviteMatchCard(inviteMatch)
                        }
                        emailLooksValid -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Default.MailOutline, null,
                                    tint = RevTextSecondary, modifier = Modifier.size(16.dp))
                                Text("Cette personne n'a pas encore de compte : invitation par e-mail.",
                                    color = RevTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    IOSButton(
                        text = if (inviteMatch != null) "Ajouter ${inviteMatch.displayName}" else "Inviter",
                        onClick = {
                            onInvite(email)
                            email = ""
                        },
                        icon = Icons.Default.PersonAdd,
                        style = IOSButtonStyle.Primary,
                        isLoading = inviting,
                        enabled = email.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    feedback?.let {
                        Text(it, color = RevTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        when {
            isLoading && members.isEmpty() && pending.isEmpty() -> {
                GlassCard {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp, color = RevOrange)
                    }
                }
            }
            members.isEmpty() && pending.isEmpty() -> {
                GlassCard {
                    Text(
                        "Aucun membre pour le moment.",
                        color = RevTextSecondary,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
            }
            else -> {
                // Membres actifs
                members.forEach { m ->
                    MemberRow(
                        name = m.name,
                        email = m.email,
                        roleLabel = if (m.is_owner) "Propriétaire" else memberRoleLabel(m.role),
                        isOwner = m.is_owner,
                        isPending = false,
                        // On peut retirer un membre non-propriétaire et qui n'est pas soi-même.
                        canRemove = canManage && !m.is_owner && m.id != currentUserId,
                        isRemoving = m.id in removingMemberIds,
                        onRemove = { onRemove(m.id) },
                    )
                }
                // Invitations en attente (grisées)
                pending.forEach { p ->
                    MemberRow(
                        name = p.email,
                        email = "Invitation envoyée",
                        roleLabel = memberRoleLabel(p.role),
                        isOwner = false,
                        isPending = true,
                        canRemove = false,
                        isRemoving = false,
                        onRemove = {},
                    )
                }
            }
        }
    }
}

// Carte de confirmation affichée quand un compte correspond exactement à
// l'email saisi : avatar + nom + email, pour rassurer avant l'ajout.
@Composable
private fun InviteMatchCard(match: be.reveetvoyage.app.data.model.UserSearchResult) {
    val avatarUrl: String? = match.avatar_url?.takeIf { it.isNotBlank() }?.let {
        if (it.startsWith("http")) it else ApiConfig.SITE_BASE + it
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RevOrange.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(RevYellow, RevOrange, RevRed))),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = (match.displayName.firstOrNull() ?: '?').uppercase(),
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(match.displayName, color = RevBrown, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, maxLines = 1)
            Text(match.email, color = RevTextSecondary, fontSize = 12.sp, maxLines = 1)
        }
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
    }
}

private fun memberRoleLabel(role: String): String = when (role) {
    "owner" -> "Propriétaire"
    "admin" -> "Admin"
    "collaborator" -> "Collaborateur"
    "viewer" -> "Lecteur"
    else -> role.replaceFirstChar { it.uppercase() }
}

@Composable
private fun MemberRow(
    name: String,
    email: String,
    roleLabel: String,
    isOwner: Boolean,
    isPending: Boolean,
    canRemove: Boolean,
    isRemoving: Boolean,
    onRemove: () -> Unit,
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth().alpha(if (isPending) 0.55f else 1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(
                    Brush.linearGradient(listOf(RevYellow.copy(alpha = .55f), RevOrange.copy(alpha = .55f)))
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isPending) Icons.Default.MailOutline else Icons.Default.Person,
                    null, tint = Color.White, modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(email, color = RevTextSecondary, fontSize = 12.sp)
            }
            // Chip de rôle
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isOwner) RevOrange.copy(alpha = .20f) else RevYellow.copy(alpha = .20f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    roleLabel,
                    color = if (isOwner) RevOrange else RevBrown,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (canRemove) {
                if (isRemoving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = RevRed)
                } else {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Retirer",
                            tint = RevRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
