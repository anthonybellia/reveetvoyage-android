package be.reveetvoyage.app.ui.screens.devis_wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import be.reveetvoyage.app.data.model.Passenger
import be.reveetvoyage.app.data.model.PassengerRequest
import be.reveetvoyage.app.data.model.User
import be.reveetvoyage.app.ui.components.IOSButton
import be.reveetvoyage.app.ui.components.IOSButtonStyle
import be.reveetvoyage.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =====================================================================
// Step 1 — Identité
// =====================================================================
@Composable
fun Step1IdentiteScreen(
    draft: DevisDraft,
    user: User?,
    onUpdate: ((DevisDraft) -> DevisDraft) -> Unit,
) {
    StepHeader(
        title = "Faisons connaissance",
        subtitle = "Confirmez vos coordonnées avant qu'on prépare votre voyage.",
    )

    if (user == null) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = RevOrange) }
        return
    }

    ReadonlyRow("Nom complet", "${user.prenom} ${user.nom}", icon = Icons.Default.Person)
    ReadonlyRow("Email", user.email, icon = Icons.Default.Email)

    if (user.phone.isNullOrBlank()) {
        WizardSectionLabel(
            "Téléphone",
            "On en aura besoin pour vous joindre rapidement.",
        )
        WizardSingleLineField(
            value = draft.phone,
            onValueChange = { v -> onUpdate { it.copy(phone = v) } },
            placeholder = "+32 4XX XX XX XX",
            leadingIcon = Icons.Default.Phone,
            keyboardType = KeyboardType.Phone,
        )
    } else {
        ReadonlyRow("Téléphone", user.phone, icon = Icons.Default.Phone)
    }
}

// =====================================================================
// Step 2 — Voyageurs (multi-select + create-on-the-fly + picker sheet)
// =====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step2VoyageursScreen(
    draft: DevisDraft,
    vm: DevisWizardViewModel,
) {
    val context = LocalContext.current
    var showFormSheet by remember { mutableStateOf(false) }
    var showPickerSheet by remember { mutableStateOf(false) }

    StepHeader(
        title = "Qui voyage avec vous ?",
        subtitle = "Liez vos voyageurs enregistrés ou ajoutez-en un en quelques clics.",
    )

    if (draft.selectedPassengers.isNotEmpty()) {
        WizardSectionLabel("Voyageurs sélectionnés (${draft.selectedPassengers.size})")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            draft.selectedPassengers.forEach { p ->
                SelectedPassengerCard(passenger = p, onRemove = { vm.removePassenger(p) })
            }
        }
    }

    WizardSectionLabel(
        if (draft.selectedPassengers.isEmpty()) "Ajouter des voyageurs" else "Ajouter d'autres voyageurs",
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        WizardActionRow(
            icon = Icons.Default.DocumentScanner,
            title = "Scanner pièce d'identité",
            subtitle = "Passeport ou CNI — auto-remplit le formulaire",
            onClick = {
                Toast.makeText(
                    context,
                    "Scan ML Kit bientôt disponible — ajoutez-le manuellement pour l'instant",
                    Toast.LENGTH_LONG,
                ).show()
                showFormSheet = true
            },
        )
        WizardActionRow(
            icon = Icons.Default.GroupAdd,
            title = "Choisir parmi mes voyageurs",
            subtitle = "Sélectionnez dans votre liste enregistrée",
            onClick = {
                vm.loadMyPassengers()
                showPickerSheet = true
            },
        )
        WizardActionRow(
            icon = Icons.Default.PersonAddAlt1,
            title = "Ajouter manuellement",
            subtitle = "Saisissez prénom, nom et infos essentielles",
            onClick = { showFormSheet = true },
        )
    }

    WizardSectionLabel(
        "Détails complémentaires",
        "Optionnel — accompagnants non-enregistrés, allergies, contraintes…",
    )
    WizardTextArea(
        value = draft.participants,
        onValueChange = { v -> vm.update { it.copy(participants = v) } },
        placeholder = "Ex: voyage avec bébé 8 mois (besoin lit parapluie), allergie fruits à coque…",
        minLines = 3,
        maxLines = 5,
    )

    if (showFormSheet) {
        PassengerFormSheet(
            vm = vm,
            onDismiss = { showFormSheet = false },
        )
    }
    if (showPickerSheet) {
        PassengerPickerSheet(
            vm = vm,
            alreadySelectedIds = draft.selectedPassengers.map { it.id }.toSet(),
            onDismiss = { showPickerSheet = false },
        )
    }
}

@Composable
private fun SelectedPassengerCard(passenger: Passenger, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = RevCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(RevYellow, RevOrange))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initialsOf(passenger),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = passenger.full_name.ifBlank { "${passenger.prenom} ${passenger.nom}" },
                    color = RevBrown,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                )
                val sub = listOfNotNull(
                    passenger.nationalite?.takeIf { it.isNotBlank() },
                    passenger.date_naissance?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (sub.isNotEmpty()) {
                    Text(sub, color = RevTextSecondary, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Retirer",
                    tint = RevTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun WizardActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = RevCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(RevOrange.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = RevOrange, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = RevTextSecondary, fontSize = 12.sp)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = RevTextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun initialsOf(p: Passenger): String {
    val a = p.prenom.firstOrNull()?.toString() ?: ""
    val b = p.nom.firstOrNull()?.toString() ?: ""
    return (a + b).uppercase().ifBlank { "?" }
}

// ---------- Bottom sheet : ajout manuel d'un passager ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassengerFormSheet(
    vm: DevisWizardViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val createError by vm.passengerCreateError.collectAsState()
    val context = LocalContext.current

    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var dateNaissance by remember { mutableStateOf("") }
    var typeDoc by remember { mutableStateOf("passeport") }
    var numDoc by remember { mutableStateOf("") }
    var nationalite by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    LaunchedEffect(createError) {
        createError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.consumePassengerError()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RevBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Nouveau voyageur",
                color = RevBrown,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Text(
                "Il sera enregistré dans votre carnet et lié à cette demande.",
                color = RevTextSecondary,
                fontSize = 13.sp,
            )

            WizardSectionLabel("Prénom *")
            WizardSingleLineField(
                value = prenom,
                onValueChange = { prenom = it },
                placeholder = "Sophie",
                leadingIcon = Icons.Default.Person,
            )

            WizardSectionLabel("Nom *")
            WizardSingleLineField(
                value = nom,
                onValueChange = { nom = it },
                placeholder = "Dupont",
                leadingIcon = Icons.Default.Badge,
            )

            WizardSectionLabel("Date de naissance", "Format JJ/MM/AAAA")
            WizardSingleLineField(
                value = dateNaissance,
                onValueChange = { dateNaissance = it },
                placeholder = "15/04/1985",
                leadingIcon = Icons.Default.Cake,
                keyboardType = KeyboardType.Number,
            )

            WizardSectionLabel("Type de document")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("passeport" to "Passeport", "cni" to "CNI", "autre" to "Autre").forEach { (key, label) ->
                    SelectableChip(
                        label = label,
                        selected = typeDoc == key,
                        onClick = { typeDoc = key },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            WizardSectionLabel("Numéro de document")
            WizardSingleLineField(
                value = numDoc,
                onValueChange = { numDoc = it },
                placeholder = "Optionnel",
                leadingIcon = Icons.Default.CreditCard,
            )

            WizardSectionLabel("Nationalité")
            WizardSingleLineField(
                value = nationalite,
                onValueChange = { nationalite = it },
                placeholder = "Belge, Française…",
                leadingIcon = Icons.Default.Public,
            )

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IOSButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    style = IOSButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                IOSButton(
                    text = "Ajouter",
                    onClick = onClick@{
                        if (submitting) return@onClick
                        if (prenom.isBlank() || nom.isBlank()) {
                            Toast.makeText(
                                context,
                                "Prénom et nom sont obligatoires",
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@onClick
                        }
                        submitting = true
                        scope.launch {
                            val ok = vm.createPassenger(
                                PassengerRequest(
                                    nom = nom.trim(),
                                    prenom = prenom.trim(),
                                    date_naissance = dateNaissance.trim().ifBlank { null },
                                    type_doc = typeDoc,
                                    num_doc = numDoc.trim().ifBlank { null },
                                    nationalite = nationalite.trim().ifBlank { null },
                                )
                            )
                            submitting = false
                            if (ok) onDismiss()
                        }
                    },
                    style = IOSButtonStyle.Primary,
                    modifier = Modifier.weight(2f),
                    isLoading = submitting,
                )
            }
        }
    }
}

// ---------- Bottom sheet : sélection parmi voyageurs existants ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassengerPickerSheet(
    vm: DevisWizardViewModel,
    alreadySelectedIds: Set<Int>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val passengers by vm.myPassengers.collectAsState()
    val loading by vm.passengersLoading.collectAsState()
    val picked = remember { mutableStateMapOf<Int, Boolean>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RevBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp, max = 600.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Mes voyageurs",
                color = RevBrown,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Text(
                "Sélectionnez ceux qui partent avec vous.",
                color = RevTextSecondary,
                fontSize = 13.sp,
            )

            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = RevOrange) }
                }
                passengers.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.PersonOff,
                            null,
                            tint = RevOrange.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp),
                        )
                        Text("Aucun voyageur enregistré", color = RevTextSecondary)
                        Text(
                            "Utilisez 'Ajouter manuellement' pour en créer.",
                            color = RevTextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(passengers, key = { it.id }) { p ->
                            val alreadyIn = alreadySelectedIds.contains(p.id)
                            val checked = alreadyIn || (picked[p.id] == true)
                            PickerRow(
                                passenger = p,
                                checked = checked,
                                disabled = alreadyIn,
                                onToggle = {
                                    if (!alreadyIn) picked[p.id] = !(picked[p.id] ?: false)
                                },
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IOSButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    style = IOSButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                IOSButton(
                    text = "Ajouter (${picked.count { it.value }})",
                    onClick = {
                        passengers.filter { picked[it.id] == true && !alreadySelectedIds.contains(it.id) }
                            .forEach { vm.addPassenger(it) }
                        onDismiss()
                    },
                    style = IOSButtonStyle.Primary,
                    modifier = Modifier.weight(2f),
                    enabled = picked.any { it.value },
                )
            }
        }
    }
}

@Composable
private fun PickerRow(
    passenger: Passenger,
    checked: Boolean,
    disabled: Boolean,
    onToggle: () -> Unit,
) {
    val bgAlpha = if (disabled) 0.5f else 1f
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !disabled) { onToggle() },
        shape = RoundedCornerShape(12.dp),
        color = RevCardBackground.copy(alpha = bgAlpha),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (checked) RevOrange else Color(0x14000000),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(RevYellow, RevOrange))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initialsOf(passenger),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    passenger.full_name.ifBlank { "${passenger.prenom} ${passenger.nom}" },
                    color = RevBrown,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                val sub = listOfNotNull(
                    passenger.nationalite?.takeIf { it.isNotBlank() },
                    passenger.date_naissance?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (sub.isNotEmpty()) {
                    Text(sub, color = RevTextSecondary, fontSize = 11.sp)
                }
                if (disabled) {
                    Text(
                        "Déjà ajouté",
                        color = RevOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (checked) RevOrange else Color(0x14000000)),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// =====================================================================
// Step 3 — Dates
// =====================================================================
@Composable
fun Step3DatesScreen(
    draft: DevisDraft,
    onUpdate: ((DevisDraft) -> DevisDraft) -> Unit,
) {
    StepHeader(
        title = "Quand partez-vous ?",
        subtitle = "Donnez-nous des dates ou une période approximative.",
    )

    WizardSectionLabel("Dates ou période souhaitées")
    WizardTextArea(
        value = draft.datesSouhaitees,
        onValueChange = { v -> onUpdate { it.copy(datesSouhaitees = v) } },
        placeholder = "Ex: Du 12 au 26 juillet 2026 — ou \"première quinzaine d'août\"",
        minLines = 2,
        maxLines = 4,
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = RevCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Je suis flexible sur les dates",
                    color = RevBrown,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    "Active si on peut décaler de quelques jours pour un meilleur prix.",
                    color = RevTextSecondary,
                    fontSize = 12.sp,
                )
            }
            Switch(
                checked = draft.flexibleDates,
                onCheckedChange = { v -> onUpdate { it.copy(flexibleDates = v) } },
                colors = SwitchDefaults.colors(checkedTrackColor = RevOrange),
            )
        }
    }

    WizardSectionLabel("Durée")
    val durees = listOf(
        "Weekend (2-3j)", "Court séjour (4-6j)", "1 semaine",
        "10-15 jours", "3 semaines", "Plus d'1 mois",
    )
    FlowChips(
        items = durees,
        isSelected = { it == draft.duree },
        onToggle = { v ->
            onUpdate { it.copy(duree = if (it.duree == v) "" else v) }
        },
    )
}

// =====================================================================
// Step 4 — Destination (multi-airports départ + retour, à la iOS)
// =====================================================================
@Composable
fun Step4DestinationScreen(
    draft: DevisDraft,
    vm: DevisWizardViewModel,
) {
    StepHeader(
        title = "D'où et vers où ?",
        subtitle = "Plusieurs aéroports possibles côté départ ET côté retour.",
    )

    AirportMultiSelector(
        title = "Aéroports de départ",
        placeholder = "Bruxelles, Paris, Amsterdam…",
        selection = draft.lieuxDepart,
        onChange = { newList -> vm.update { it.copy(lieuxDepart = newList) } },
        searchAirports = vm::searchAirportsImmediate,
    )

    AirportMultiSelector(
        title = "Aéroports de retour",
        placeholder = "Même que départ si vol aller-retour",
        selection = draft.lieuxRetour,
        onChange = { newList -> vm.update { it.copy(lieuxRetour = newList) } },
        searchAirports = vm::searchAirportsImmediate,
    )

    WizardSectionLabel(
        "Destination souhaitée",
        "Une idée précise ou plusieurs envies — on est preneurs.",
    )
    WizardTextArea(
        value = draft.destination,
        onValueChange = { v -> vm.update { it.copy(destination = v) } },
        placeholder = "Ex: Bali, Maroc, road-trip Écosse, hésite entre Grèce et Croatie…",
        minLines = 2,
        maxLines = 4,
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = RevCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Ouvert aux suggestions",
                    color = RevBrown,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    "Matilda peut vous proposer des alternatives surprenantes.",
                    color = RevTextSecondary,
                    fontSize = 12.sp,
                )
            }
            Switch(
                checked = draft.ouvertSuggestions,
                onCheckedChange = { v -> vm.update { it.copy(ouvertSuggestions = v) } },
                colors = SwitchDefaults.colors(checkedTrackColor = RevOrange),
            )
        }
    }
}

/**
 * Sélecteur multi-aéroports avec :
 * - Chips des sélectionnés (X pour enlever)
 * - TextField + autocomplete debounced 300ms (local au composant)
 * - Raccourcis BRU/CRL/CDG/ORY/AMS/LGG
 * - Liste de résultats (add/check icon)
 */
@Composable
private fun AirportMultiSelector(
    title: String,
    placeholder: String,
    selection: List<Airport>,
    onChange: (List<Airport>) -> Unit,
    searchAirports: suspend (String) -> List<Airport>,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Airport>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val shortcuts = listOf("BRU", "CRL", "CDG", "ORY", "AMS", "LGG")

    fun isSelected(code: String): Boolean = selection.any { it.c.equals(code, ignoreCase = true) }

    fun add(a: Airport) {
        if (!isSelected(a.c)) onChange(selection + a)
        query = ""
        results = emptyList()
    }

    fun remove(code: String) {
        onChange(selection.filterNot { it.c.equals(code, ignoreCase = true) })
    }

    // Debounce 300ms sur query
    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            results = emptyList()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        delay(300)
        val res = searchAirports(trimmed)
        results = res
        loading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        WizardSectionLabel(title)

        if (selection.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                selection.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { a ->
                            SelectedAirportPill(
                                airport = a,
                                onRemove = { remove(a.c) },
                            )
                        }
                    }
                }
            }
        }

        WizardSingleLineField(
            value = query,
            onValueChange = { query = it },
            placeholder = placeholder,
            leadingIcon = Icons.Default.FlightTakeoff,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(shortcuts) { code ->
                ShortcutChip(
                    code = code,
                    selected = isSelected(code),
                    onClick = {
                        if (isSelected(code)) {
                            remove(code)
                        } else {
                            // hit API to fetch the canonical Airport for this code
                            scope.launch {
                                val res = searchAirports(code)
                                val match = res.firstOrNull { it.c.equals(code, ignoreCase = true) }
                                    ?: res.firstOrNull()
                                if (match != null) add(match)
                            }
                        }
                    },
                )
            }
        }

        if (loading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(
                    color = RevOrange,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Text("Recherche…", color = RevTextSecondary, fontSize = 12.sp)
            }
        } else if (results.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                results.take(6).forEach { ap ->
                    AirportResultRow(
                        airport = ap,
                        already = isSelected(ap.c),
                        onClick = { if (!isSelected(ap.c)) add(ap) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedAirportPill(airport: Airport, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = RevYellow.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, RevOrange.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Default.FlightTakeoff,
                null,
                tint = RevOrange,
                modifier = Modifier.size(12.dp),
            )
            Text(
                airport.c,
                color = RevBrown,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(22.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Retirer",
                    tint = RevTextSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ShortcutChip(code: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) RevOrange else RevYellow.copy(alpha = 0.35f),
        onClick = onClick,
    ) {
        Text(
            code,
            color = if (selected) Color.White else RevBrown,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun AirportResultRow(airport: Airport, already: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !already) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = RevCardBackground,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (already) RevOrange.copy(alpha = 0.4f) else Color(0x14000000),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(RevOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    airport.c,
                    color = RevOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${airport.v} — ${airport.n}",
                    color = RevBrown,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
                Text(
                    airport.p,
                    color = RevTextSecondary,
                    fontSize = 11.sp,
                )
            }
            Icon(
                if (already) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint = RevOrange,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// =====================================================================
// Step 5 — Séjour
// =====================================================================
@Composable
fun Step5SejourScreen(
    draft: DevisDraft,
    onUpdate: ((DevisDraft) -> DevisDraft) -> Unit,
) {
    StepHeader(
        title = "Quel cadre vous fait rêver ?",
        subtitle = "Sélectionnez le ou les cadres et l'hébergement souhaité.",
    )

    WizardSectionLabel("Cadre du séjour", "Plusieurs choix possibles.")
    val cadres = listOf("Plage", "Montagne", "Ville", "Campagne", "Désert", "Exotique")
    FlowChipsMulti(
        items = cadres,
        selected = draft.cadre,
        onToggle = { v ->
            onUpdate {
                val newSet = if (it.cadre.contains(v)) it.cadre - v else it.cadre + v
                it.copy(cadre = newSet)
            }
        },
    )

    WizardSectionLabel("Type(s) d'hébergement", "Plusieurs choix possibles")
    val hebergements = listOf(
        Triple("Hôtel", Icons.Default.Hotel, RevOrange),
        Triple("Riad", Icons.Default.Apartment, Color(0xFFB07A3B)),
        Triple("Lodge", Icons.Default.Cabin, Color(0xFF5E8B5C)),
        Triple("Camping", Icons.Default.Forest, Color(0xFF4A7A3C)),
        Triple("Villa", Icons.Default.Villa, Color(0xFFC76426)),
        Triple("Appartement", Icons.Default.House, Color(0xFF5C7CFA)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        hebergements.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, icon, color) ->
                    SelectableCard(
                        title = label,
                        selected = draft.hebergement.contains(label),
                        onClick = {
                            onUpdate {
                                val newSet = if (it.hebergement.contains(label)) it.hebergement - label else it.hebergement + label
                                it.copy(hebergement = newSet)
                            }
                        },
                        icon = icon,
                        iconColor = color,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    WizardSectionLabel(
        "Besoins spécifiques",
        "PMR, suite familiale, espace bébé, accès direct plage…",
    )
    WizardTextArea(
        value = draft.besoinsSpecifiques,
        onValueChange = { v -> onUpdate { it.copy(besoinsSpecifiques = v) } },
        placeholder = "Optionnel — décrivez ce qui est important pour vous.",
        minLines = 3,
    )
}

// =====================================================================
// Step 6 — Activités
// =====================================================================
@Composable
fun Step6ActivitesScreen(
    draft: DevisDraft,
    onUpdate: ((DevisDraft) -> DevisDraft) -> Unit,
) {
    StepHeader(
        title = "Que voulez-vous vivre ?",
        subtitle = "Tagguez vos envies d'activités et de découvertes.",
    )

    WizardSectionLabel("Activités souhaitées", "Plusieurs choix possibles.")
    val activites = listOf(
        "Plongée", "Randonnée", "Gastronomie", "Culture",
        "Spa", "Aventure", "Famille", "Romantique",
    )
    FlowChipsMulti(
        items = activites,
        selected = draft.activites,
        onToggle = { v ->
            onUpdate {
                val newSet = if (it.activites.contains(v)) it.activites - v else it.activites + v
                it.copy(activites = newSet)
            }
        },
    )

    WizardSectionLabel(
        "À éviter",
        "Activités, ambiances ou contextes que vous ne voulez surtout pas.",
    )
    WizardTextArea(
        value = draft.activitesEviter,
        onValueChange = { v -> onUpdate { it.copy(activitesEviter = v) } },
        placeholder = "Optionnel — ex: foule, soirées tardives, sports extrêmes…",
        minLines = 3,
    )
}

// =====================================================================
// Step 7 — Détails
// =====================================================================
@Composable
fun Step7DetailsScreen(
    draft: DevisDraft,
    onUpdate: ((DevisDraft) -> DevisDraft) -> Unit,
) {
    StepHeader(
        title = "Dernière étape",
        subtitle = "Budget et derniers détails pour finaliser votre demande.",
    )

    WizardSectionLabel("Budget par personne", "Sélectionnez la fourchette.")
    val budgets = listOf(
        Triple("Économique", "Moins de 1500 €", Icons.Default.Savings),
        Triple("Moyen", "Entre 1500 € et 3000 €", Icons.Default.AccountBalanceWallet),
        Triple("Confort", "Entre 3000 € et 5000 €", Icons.Default.Star),
        Triple("Luxe", "Plus de 5000 €", Icons.Default.Diamond),
        Triple("Sans limite", "Le rêve avant tout", Icons.Default.AutoAwesome),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        budgets.forEach { (label, sub, icon) ->
            SelectableCard(
                title = label,
                subtitle = sub,
                selected = draft.budget == label,
                onClick = {
                    onUpdate {
                        it.copy(budget = if (it.budget == label) "" else label)
                    }
                },
                icon = icon,
            )
        }
    }

    WizardSectionLabel(
        "Impératifs",
        "Contraintes pratiques (vol direct uniquement, dates fixes…)",
    )
    WizardTextArea(
        value = draft.imperatifs,
        onValueChange = { v -> onUpdate { it.copy(imperatifs = v) } },
        placeholder = "Optionnel",
        minLines = 2,
        maxLines = 4,
    )

    WizardSectionLabel(
        "Événement spécial",
        "Anniversaire, lune de miel, demande en mariage…",
    )
    WizardSingleLineField(
        value = draft.evenement,
        onValueChange = { v -> onUpdate { it.copy(evenement = v) } },
        placeholder = "Optionnel",
        leadingIcon = Icons.Default.Celebration,
    )

    WizardSectionLabel(
        "Un dernier mot pour Matilda ?",
        "Optionnel — ajoutez tout ce qui pourrait aider.",
    )
    WizardTextArea(
        value = draft.message,
        onValueChange = { v -> onUpdate { it.copy(message = v) } },
        placeholder = "Optionnel",
        minLines = 3,
        maxLines = 6,
    )
}

// =====================================================================
// Helpers — header + flow chips wrapping
// =====================================================================

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            title,
            color = RevBrown,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
        )
        Text(subtitle, color = RevTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

/**
 * Flow-row simulé : enchaîne les chips et passe à la ligne automatiquement.
 * Compose Material 3 1.x n'a pas encore FlowRow stable, donc on découpe en
 * lignes de N selon une heuristique simple (3 par ligne max).
 */
@Composable
private fun FlowChips(
    items: List<String>,
    isSelected: (String) -> Boolean,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    SelectableChip(
                        label = item,
                        selected = isSelected(item),
                        onClick = { onToggle(item) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FlowChipsMulti(
    items: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    SelectableChip(
                        label = item,
                        selected = selected.contains(item),
                        onClick = { onToggle(item) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
