package be.reveetvoyage.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.model.Devis
import be.reveetvoyage.app.data.model.DevisNote
import be.reveetvoyage.app.data.model.DevisUpdateRequest
import be.reveetvoyage.app.data.repo.DevisRepository
import be.reveetvoyage.app.data.repo.UserRepository
import be.reveetvoyage.app.ui.components.*
import be.reveetvoyage.app.ui.screens.admin.OwnerRow
import be.reveetvoyage.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Statuts admin disponibles (alignés sur le backend + l'écran liste).
private val DEVIS_STATUSES = listOf("nouveau", "en_cours", "valide", "refuse", "archive")

@HiltViewModel
class DevisDetailViewModel @Inject constructor(
    private val repo: DevisRepository,
    userRepo: UserRepository,
) : ViewModel() {
    private val _devis = MutableStateFlow<Devis?>(null)
    val devis: StateFlow<Devis?> = _devis.asStateFlow()

    private val _notes = MutableStateFlow<List<DevisNote>>(emptyList())
    val notes: StateFlow<List<DevisNote>> = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Action en cours (busy generique) pour griser les boutons d'action admin.
    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Vrai si l'opération de suppression a réussi → l'écran peut faire popBackStack.
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Id du voyage créé après conversion (pour afficher la confirmation).
    private val _convertedVoyageId = MutableStateFlow<Int?>(null)
    val convertedVoyageId: StateFlow<Int?> = _convertedVoyageId.asStateFlow()

    // Admin effectif (vrai admin ET pas en mode aperçu).
    val isAdmin: StateFlow<Boolean> = userRepo.isAdmin

    private var loadedId: Int = 0

    fun load(id: Int) {
        loadedId = id
        viewModelScope.launch {
            _isLoading.value = true
            _devis.value = runCatching { repo.detail(id) }.getOrNull()
            _isLoading.value = false
        }
    }

    fun loadNotes(id: Int) {
        viewModelScope.launch { _notes.value = repo.notes(id) }
    }

    fun changeStatut(statut: String) = runAction {
        _devis.value = repo.update(loadedId, DevisUpdateRequest(statut = statut))
    }

    fun saveEdit(req: DevisUpdateRequest) = runAction {
        _devis.value = repo.update(loadedId, req)
    }

    fun convert() = runAction {
        _convertedVoyageId.value = repo.convertToVoyage(loadedId)
        // Recharge le devis pour récupérer voyage_id (masque le bouton convertir).
        _devis.value = runCatching { repo.detail(loadedId) }.getOrNull() ?: _devis.value
    }

    fun delete() = runAction {
        if (repo.delete(loadedId)) _deleted.value = true
    }

    fun addNote(contenu: String) {
        val text = contenu.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            runCatching { repo.addNote(loadedId, text) }
                .onSuccess { _notes.value = listOf(it) + _notes.value }
                .onFailure { _errorMessage.value = "Impossible d'ajouter la note." }
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            runCatching { repo.deleteNote(loadedId, noteId) }
                .onSuccess { _notes.value = _notes.value.filterNot { n -> n.id == noteId } }
                .onFailure { _errorMessage.value = "Impossible de supprimer la note." }
        }
    }

    fun clearError() { _errorMessage.value = null }
    fun clearConvertedVoyage() { _convertedVoyageId.value = null }

    // Wrapper commun : busy + capture d'erreur réseau/HTTP.
    private fun runAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            runCatching { block() }.onFailure {
                _errorMessage.value = it.message ?: "Une erreur est survenue."
            }
            _isBusy.value = false
        }
    }
}

@Composable
fun DevisDetailScreen(
    devisId: Int,
    onBack: () -> Unit,
    vm: DevisDetailViewModel = hiltViewModel(),
) {
    val devis by vm.devis.collectAsState()
    val notes by vm.notes.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val isBusy by vm.isBusy.collectAsState()
    val isAdmin by vm.isAdmin.collectAsState()
    val deleted by vm.deleted.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()
    val convertedVoyageId by vm.convertedVoyageId.collectAsState()

    LaunchedEffect(devisId) { vm.load(devisId) }
    // Les notes ne sont chargées que pour l'admin effectif (endpoint admin-only).
    LaunchedEffect(devisId, isAdmin) { if (isAdmin) vm.loadNotes(devisId) }
    // Quitte l'écran après une suppression réussie.
    LaunchedEffect(deleted) { if (deleted) onBack() }

    var showStatusSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showConvertConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        IOSTopBar(
            title = devis?.let { "Demande #${it.id}" } ?: "Demande",
            onBack = onBack,
        )
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(RevYellow.copy(alpha = .08f), RevBackground))
        )) {
            when {
                isLoading && devis == null -> LoadingFull()
                devis == null -> EmptyState(
                    icon = Icons.Default.ErrorOutline,
                    title = "Demande introuvable",
                    subtitle = "Elle a peut-être été supprimée.",
                )
                else -> {
                    val d = devis!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        HeroCard(d, isAdmin)
                        DetailsCard(d)

                        val prefs = preferencesItems(d)
                        if (prefs.isNotEmpty()) PreferencesCard(prefs)

                        d.message?.takeIf { it.isNotBlank() }?.let { MessageCard(it) }

                        // ===== Bloc admin : actions + notes (masqué en aperçu utilisateur) =====
                        if (isAdmin) {
                            AdminActionsCard(
                                devis = d,
                                isBusy = isBusy,
                                onChangeStatus = { showStatusSheet = true },
                                onEdit = { showEditSheet = true },
                                onConvert = { showConvertConfirm = true },
                                onDelete = { showDeleteConfirm = true },
                            )
                            NotesCard(
                                notes = notes,
                                onAdd = { vm.addNote(it) },
                                onDelete = { vm.deleteNote(it) },
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // ===== Action sheets / dialogs (admin uniquement) =====
    if (isAdmin && showStatusSheet && devis != null) {
        val current = devis!!.statut
        IOSActionSheet(
            title = "Changer le statut",
            actions = DEVIS_STATUSES.map { s ->
                val label = devisStatutLabel(s).first
                IOSAction(
                    label = if (s == current) "$label ✓" else label,
                    onClick = { if (s != current) vm.changeStatut(s) },
                )
            },
            onDismiss = { showStatusSheet = false },
        )
    }

    if (isAdmin && showEditSheet && devis != null) {
        DevisEditDialog(
            devis = devis!!,
            isBusy = isBusy,
            onSave = { vm.saveEdit(it); showEditSheet = false },
            onDismiss = { showEditSheet = false },
        )
    }

    if (isAdmin && showConvertConfirm && devis != null) {
        IOSAlertDialog(
            title = "Convertir en voyage ?",
            message = "Un voyage sera créé et lié à cette demande.",
            confirmText = "Créer le voyage",
            cancelText = "Annuler",
            onConfirm = { vm.convert(); showConvertConfirm = false },
            onDismiss = { showConvertConfirm = false },
        )
    }

    if (isAdmin && showDeleteConfirm && devis != null) {
        val d = devis!!
        IOSAlertDialog(
            title = "Supprimer cette demande ?",
            message = "La demande de ${d.prenom} ${d.nom} sera supprimée définitivement.",
            confirmText = "Supprimer",
            cancelText = "Annuler",
            isDestructive = true,
            onConfirm = { vm.delete(); showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    // Confirmation de conversion réussie.
    convertedVoyageId?.let { vid ->
        IOSAlertDialog(
            title = "Voyage créé !",
            message = "Le voyage #$vid a été créé. Tu peux le retrouver dans l'onglet Voyages.",
            confirmText = "OK",
            cancelText = null,
            onConfirm = { vm.clearConvertedVoyage() },
            onDismiss = { vm.clearConvertedVoyage() },
        )
    }

    // Erreur générique.
    errorMessage?.let { msg ->
        IOSAlertDialog(
            title = "Erreur",
            message = msg,
            confirmText = "OK",
            cancelText = null,
            onConfirm = { vm.clearError() },
            onDismiss = { vm.clearError() },
        )
    }
}

// ============================================================
// Hero card : statut + titre + destination + propriétaire (admin)
// ============================================================
@Composable
private fun HeroCard(d: Devis, isAdmin: Boolean) {
    val (statutLabel, statutKind) = devisStatutLabel(d.statut)
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = 20) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(statutLabel, statutKind)
                Text(
                    d.titre_voyage ?: d.destination ?: d.destination_souhaitee ?: "Demande de voyage",
                    color = RevBrown, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                )
                (d.destination ?: d.destination_souhaitee)?.takeIf { it.isNotBlank() }?.let {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Place, null, tint = RevTextSecondary, modifier = Modifier.size(13.dp))
                        Text(it, color = RevTextSecondary, fontSize = 14.sp)
                    }
                }
                if (isAdmin) {
                    OwnerRow(owner = d.owner)
                    listOfNotNull(d.email.takeIf { it.isNotBlank() }, d.telephone?.takeIf { it.isNotBlank() })
                        .takeIf { it.isNotEmpty() }
                        ?.let { Text(it.joinToString(" · "), color = RevTextSecondary, fontSize = 12.sp) }
                }
            }
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(RevOrange, RevRed))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(heroIcon(d.type_voyage), null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
    }
}

private fun heroIcon(type: String?): ImageVector = when (type) {
    "couple" -> Icons.Default.Favorite
    "famille" -> Icons.Default.FamilyRestroom
    "amis" -> Icons.Default.Groups
    "solo" -> Icons.Default.Person
    "lune_de_miel" -> Icons.Default.AutoAwesome
    else -> Icons.Default.Flight
}

// ============================================================
// Details card
// ============================================================
private data class DRow(val icon: ImageVector, val label: String, val value: String)

@Composable
private fun DetailsCard(d: Devis) {
    val rows = buildList {
        d.nb_personnes?.let { add(DRow(Icons.Default.Group, "Voyageurs", "$it personne(s)")) }
        (d.dates_souhaitees ?: d.flexible_dates)?.takeIf { it.isNotBlank() }
            ?.let { add(DRow(Icons.Default.CalendarMonth, "Dates", it)) }
        d.duree?.takeIf { it.isNotBlank() }?.let { add(DRow(Icons.Default.Schedule, "Durée", it)) }
        d.lieu_depart?.takeIf { it.isNotBlank() }?.let { add(DRow(Icons.Default.FlightTakeoff, "Départ depuis", it)) }
        d.budget?.takeIf { it.isNotBlank() }?.let { add(DRow(Icons.Default.Euro, "Budget", it)) }
        d.type_voyage?.takeIf { it.isNotBlank() }
            ?.let { add(DRow(Icons.Default.Favorite, "Type de voyage", it.replace('_', ' ').replaceFirstChar(Char::titlecase))) }
        d.montant_estime?.takeIf { it > 0 }?.let { add(DRow(Icons.Default.RequestQuote, "Montant estimé", "${it.toInt()} €")) }
        d.date_depart_prevue?.take(10)?.takeIf { it.isNotBlank() }?.let { add(DRow(Icons.Default.Event, "Départ prévu", it)) }
        d.date_retour_prevue?.take(10)?.takeIf { it.isNotBlank() }?.let { add(DRow(Icons.Default.EventAvailable, "Retour prévu", it)) }
    }
    if (rows.isEmpty()) return
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Détails", Icons.Default.Info)
            rows.forEach { r ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(r.icon, null, tint = RevOrange, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(r.label, color = RevTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(r.value, color = RevBrown, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private fun preferencesItems(d: Devis): List<Pair<String, String>> = buildList {
    d.cadre?.takeIf { it.isNotBlank() }?.let { add("Cadre" to it) }
    d.hebergement?.takeIf { it.isNotBlank() }?.let { add("Hébergement" to it) }
    d.activites?.takeIf { it.isNotBlank() }?.let { add("Activités" to it) }
    d.activites_eviter?.takeIf { it.isNotBlank() }?.let { add("À éviter" to it) }
    d.imperatifs?.takeIf { it.isNotBlank() }?.let { add("Impératifs" to it) }
    d.evenement?.takeIf { it.isNotBlank() }?.let { add("Événement" to it) }
    d.besoins_specifiques?.takeIf { it.isNotBlank() }?.let { add("Besoins spécifiques" to it) }
}

@Composable
private fun PreferencesCard(items: List<Pair<String, String>>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Préférences", Icons.Default.Tune)
            items.forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, color = RevTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(value, color = RevBrown, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Message du client", Icons.Default.FormatQuote)
            Text(message, color = RevBrown, fontSize = 14.sp)
        }
    }
}

// ============================================================
// Admin actions
// ============================================================
@Composable
private fun AdminActionsCard(
    devis: Devis,
    isBusy: Boolean,
    onChangeStatus: () -> Unit,
    onEdit: () -> Unit,
    onConvert: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Actions admin", Icons.Default.VerifiedUser)
            IOSButton(
                text = "Changer le statut",
                icon = Icons.Default.SwapHoriz,
                style = IOSButtonStyle.Secondary,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                onClick = onChangeStatus,
            )
            IOSButton(
                text = "Modifier la demande",
                icon = Icons.Default.Edit,
                style = IOSButtonStyle.Secondary,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                onClick = onEdit,
            )
            // Convertir uniquement si aucun voyage n'est encore lié.
            if (devis.voyage_id == null) {
                IOSButton(
                    text = "Convertir en voyage",
                    icon = Icons.Default.Flight,
                    style = IOSButtonStyle.Primary,
                    isLoading = isBusy,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onConvert,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                    Text("Voyage lié #${devis.voyage_id}", color = Color(0xFF2E7D32),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            IOSButton(
                text = "Supprimer la demande",
                icon = Icons.Default.Delete,
                style = IOSButtonStyle.Destructive,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                onClick = onDelete,
            )
        }
    }
}

// ============================================================
// Notes internes threadées
// ============================================================
@Composable
private fun NotesCard(
    notes: List<DevisNote>,
    onAdd: (String) -> Unit,
    onDelete: (Int) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Notes internes", Icons.Default.StickyNote2, "${notes.size}")

            if (notes.isEmpty()) {
                Text("Aucune note pour le moment.", color = RevTextSecondary, fontSize = 13.sp)
            } else {
                notes.forEach { note ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = RevOrange.copy(alpha = 0.06f),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(note.contenu, color = RevBrown, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    note.author?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, color = RevOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    note.created_at?.take(10)?.let {
                                        Text(it, color = RevTextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                            IconButton(
                                onClick = { onDelete(note.id) },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(Icons.Default.Close, "Supprimer la note",
                                    tint = RevTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            IOSTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "Ajouter une note interne…",
                icon = Icons.Default.Edit,
            )
            IOSButton(
                text = "Ajouter la note",
                icon = Icons.Default.Add,
                style = IOSButtonStyle.Secondary,
                enabled = draft.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = { onAdd(draft); draft = "" },
            )
        }
    }
}

// ============================================================
// Edit dialog : champs éditables du devis (PUT)
// ============================================================
@Composable
private fun DevisEditDialog(
    devis: Devis,
    isBusy: Boolean,
    onSave: (DevisUpdateRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var titre by remember { mutableStateOf(devis.titre_voyage ?: "") }
    var destination by remember { mutableStateOf(devis.destination ?: devis.destination_souhaitee ?: "") }
    var dates by remember { mutableStateOf(devis.dates_souhaitees ?: "") }
    var duree by remember { mutableStateOf(devis.duree ?: "") }
    var nbPersonnes by remember { mutableStateOf(devis.nb_personnes?.toString() ?: "") }
    var budget by remember { mutableStateOf(devis.budget ?: "") }
    var montant by remember { mutableStateOf(devis.montant_estime?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = RevCardBackground,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // En-tête
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Modifier la demande", color = RevBrown,
                        fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Fermer", tint = RevTextSecondary)
                    }
                }
                HorizontalDivider(color = Color(0x14000000), thickness = 0.5.dp)

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EditField("Titre du voyage", titre, { titre = it }, Icons.Default.Title)
                    EditField("Destination", destination, { destination = it }, Icons.Default.Place)
                    EditField("Dates souhaitées", dates, { dates = it }, Icons.Default.CalendarMonth)
                    EditField("Durée", duree, { duree = it }, Icons.Default.Schedule)
                    EditField("Nombre de personnes", nbPersonnes, { nbPersonnes = it.filter(Char::isDigit) },
                        Icons.Default.Group, KeyboardType.Number)
                    EditField("Budget", budget, { budget = it }, Icons.Default.Euro)
                    EditField("Montant estimé (€)", montant, { montant = it.filter { c -> c.isDigit() || c == '.' } },
                        Icons.Default.RequestQuote, KeyboardType.Number)
                }

                HorizontalDivider(color = Color(0x14000000), thickness = 0.5.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IOSButton(
                        text = "Annuler",
                        style = IOSButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                    IOSButton(
                        text = "Enregistrer",
                        style = IOSButtonStyle.Primary,
                        isLoading = isBusy,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSave(
                                DevisUpdateRequest(
                                    titre_voyage = titre.trim().ifBlank { null },
                                    destination = destination.trim().ifBlank { null },
                                    dates_souhaitees = dates.trim().ifBlank { null },
                                    duree = duree.trim().ifBlank { null },
                                    nb_personnes = nbPersonnes.trim().toIntOrNull(),
                                    budget = budget.trim().ifBlank { null },
                                    montant_estime = montant.trim().toDoubleOrNull(),
                                )
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = RevTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        IOSTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = label,
            icon = icon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
    }
}
