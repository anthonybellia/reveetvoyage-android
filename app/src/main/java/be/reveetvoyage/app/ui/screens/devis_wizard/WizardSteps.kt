package be.reveetvoyage.app.ui.screens.devis_wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.reveetvoyage.app.data.model.User
import be.reveetvoyage.app.ui.theme.*

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
// Step 2 — Voyageurs
// =====================================================================
@Composable
fun Step2VoyageursScreen(
    draft: DevisDraft,
    onUpdate: ((DevisDraft) -> DevisDraft) -> Unit,
) {
    StepHeader(
        title = "Qui voyage ?",
        subtitle = "Nombre de voyageurs et infos utiles (âges, mobilité…).",
    )

    WizardSectionLabel("Combien serez-vous ?")
    val options = listOf("1", "2", "3", "4", "5", "6+")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { v ->
            val selected = draft.nbPersonnes == v
            SelectableChip(
                label = v,
                selected = selected,
                onClick = {
                    onUpdate { it.copy(nbPersonnes = if (selected) "" else v) }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    WizardSectionLabel(
        "Détails sur les voyageurs",
        "Ages, prénoms, contraintes (allergies, mobilité réduite, etc.)",
    )
    WizardTextArea(
        value = draft.participants,
        onValueChange = { v -> onUpdate { it.copy(participants = v) } },
        placeholder = "Ex: Couple 30 ans + enfant 5 ans (allergique aux fruits à coque)",
        minLines = 3,
        maxLines = 6,
    )
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
// Step 4 — Destination
// =====================================================================
@Composable
fun Step4DestinationScreen(
    draft: DevisDraft,
    vm: DevisWizardViewModel,
) {
    val airportResults by vm.airportResults.collectAsState()
    val airportLoading by vm.airportLoading.collectAsState()

    StepHeader(
        title = "D'où et vers où ?",
        subtitle = "On organise le transport selon votre point de départ.",
    )

    WizardSectionLabel("Aéroport ou ville de départ")
    val shortcuts = listOf("BRU", "CRL", "CDG", "ORY", "AMS", "LGG")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(shortcuts) { code ->
            SelectableChip(
                label = code,
                selected = draft.lieuDepart.startsWith(code),
                onClick = {
                    vm.update { it.copy(lieuDepart = code) }
                    vm.clearAirportResults()
                },
            )
        }
    }

    WizardSingleLineField(
        value = draft.lieuDepart,
        onValueChange = { v ->
            vm.update { it.copy(lieuDepart = v) }
            vm.queryAirports(v)
        },
        placeholder = "Ex: Bruxelles, Paris, Charleroi…",
        leadingIcon = Icons.Default.FlightTakeoff,
    )

    if (draft.lieuDepart.length >= 2 && (airportResults.isNotEmpty() || airportLoading)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (airportLoading && airportResults.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        color = RevOrange,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            airportResults.take(6).forEach { ap ->
                AirportSuggestionRow(
                    airport = ap,
                    onClick = {
                        vm.update { it.copy(lieuDepart = "${ap.c} (${ap.v})") }
                        vm.clearAirportResults()
                    },
                )
            }
        }
    }

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

@Composable
private fun AirportSuggestionRow(airport: Airport, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = RevCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
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

    WizardSectionLabel("Type d'hébergement")
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
                        selected = draft.hebergement == label,
                        onClick = {
                            onUpdate {
                                it.copy(hebergement = if (it.hebergement == label) "" else label)
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
