package be.reveetvoyage.app.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import be.reveetvoyage.app.data.model.SettlementBalance
import be.reveetvoyage.app.data.model.SettlementTransaction
import be.reveetvoyage.app.data.model.VoyageExpense
import be.reveetvoyage.app.data.model.VoyageParticipant
import be.reveetvoyage.app.ui.components.*
import be.reveetvoyage.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ============================================================
// Category mapping
// ============================================================
data class ExpenseCategory(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
)

val ExpenseCategories = listOf(
    ExpenseCategory("restaurant", "Restaurant", Icons.Default.Restaurant, RevOrange),
    ExpenseCategory("activite", "Activité", Icons.Default.DirectionsWalk, Color(0xFF10B981)),
    ExpenseCategory("transport", "Transport", Icons.Default.DirectionsCar, Color(0xFF3B82F6)),
    ExpenseCategory("hotel", "Hôtel", Icons.Default.Bed, Color(0xFF6366F1)),
    ExpenseCategory("shopping", "Shopping", Icons.Default.ShoppingBag, Color(0xFFEC4899)),
    ExpenseCategory("essence", "Essence", Icons.Default.LocalGasStation, Color(0xFFEF4444)),
    ExpenseCategory("cadeau", "Cadeau", Icons.Default.CardGiftcard, Color(0xFFF59E0B)),
    ExpenseCategory("autre", "Autre", Icons.Default.Circle, Color(0xFF6B7280)),
)

fun categoryFor(key: String): ExpenseCategory =
    ExpenseCategories.firstOrNull { it.key == key } ?: ExpenseCategories.last()

internal fun formatAmount(amount: Double, currency: String = "EUR"): String {
    val symbol = when (currency.uppercase()) {
        "EUR" -> "€"
        "USD" -> "$"
        "GBP" -> "£"
        else -> currency
    }
    // Format in US locale (1,234.56) then swap separators to FR style (1 234,56)
    val s = String.format(java.util.Locale.US, "%,.2f", amount)
        .replace(',', ' ').replace('.', ',')
    return "$s $symbol"
}

// ============================================================
// Main screen
// ============================================================
private enum class ExpensesTab(val label: String) {
    Depenses("Dépenses"),
    Solde("Solde"),
    Participants("Participants"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    voyageId: Int,
    onBack: () -> Unit,
    vm: ExpensesViewModel = hiltViewModel(),
) {
    val expenses by vm.expenses.collectAsState()
    val participants by vm.participants.collectAsState()
    val settlement by vm.settlement.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    var tab by remember { mutableStateOf(ExpensesTab.Depenses) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<VoyageExpense?>(null) }
    var actionSheetExpense by remember { mutableStateOf<VoyageExpense?>(null) }
    var pendingDelete by remember { mutableStateOf<VoyageExpense?>(null) }
    var pendingRemoveParticipant by remember { mutableStateOf<VoyageParticipant?>(null) }
    var showAddGuestDialog by remember { mutableStateOf(false) }
    var showInviteEmailDialog by remember { mutableStateOf(false) }

    LaunchedEffect(voyageId) { vm.load(voyageId) }

    // 5-second polling
    LaunchedEffect(voyageId) {
        while (isActive) {
            delay(5000)
            vm.refresh(voyageId)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        IOSTopBar(title = "Dépenses", onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(RevYellow.copy(alpha = .08f), RevBackground))),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Segmented control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExpensesTab.values().forEach { t ->
                        val selected = t == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(
                                    if (selected) Brush.horizontalGradient(listOf(RevOrange, RevRed))
                                    else Brush.horizontalGradient(listOf(RevCardBackground, RevCardBackground))
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = if (selected) Color.Transparent else Color(0x14000000),
                                    shape = CircleShape,
                                )
                                .clickable { tab = t }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                t.label,
                                color = if (selected) Color.White else RevBrown,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                when (tab) {
                    ExpensesTab.Depenses -> ExpensesTabContent(
                        expenses = expenses,
                        settlement = settlement,
                        isLoading = isLoading,
                        onExpenseTap = { actionSheetExpense = it },
                    )
                    ExpensesTab.Solde -> SoldeTabContent(
                        settlement = settlement,
                        isLoading = isLoading,
                    )
                    ExpensesTab.Participants -> ParticipantsTabContent(
                        participants = participants,
                        isLoading = isLoading,
                        onRemove = { pendingRemoveParticipant = it },
                        onAddGuest = { showAddGuestDialog = true },
                        onInviteEmail = { showInviteEmailDialog = true },
                    )
                }
            }

            // FAB
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(22.dp)
                    .size(56.dp)
                    .shadow(8.dp, CircleShape, spotColor = RevOrange.copy(alpha = 0.4f))
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(RevOrange, RevRed)))
                    .clickable { showAddSheet = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }

    // Add expense sheet
    if (showAddSheet) {
        AddExpenseSheet(
            participants = participants,
            initial = null,
            onDismiss = { showAddSheet = false },
            onSave = { title, amount, paidBy, splits, category, spentAt, description, locName, locLat, locLng ->
                vm.createExpense(
                    voyageId = voyageId,
                    title = title,
                    amount = amount,
                    paidByParticipantId = paidBy,
                    spentAt = spentAt,
                    description = description,
                    category = category,
                    splits = splits,
                    locationName = locName,
                    locationLatitude = locLat,
                    locationLongitude = locLng,
                ) { showAddSheet = false }
            },
            vm = vm,
        )
    }

    // Edit expense sheet
    editing?.let { exp ->
        AddExpenseSheet(
            participants = participants,
            initial = exp,
            onDismiss = { editing = null },
            onSave = { title, amount, paidBy, splits, category, spentAt, description, locName, locLat, locLng ->
                vm.updateExpense(
                    voyageId = voyageId,
                    expenseId = exp.id,
                    title = title,
                    amount = amount,
                    paidByParticipantId = paidBy,
                    spentAt = spentAt,
                    description = description,
                    category = category,
                    splits = splits,
                    locationName = locName,
                    locationLatitude = locLat,
                    locationLongitude = locLng,
                ) { editing = null }
            },
            vm = vm,
        )
    }

    // Action sheet on row tap
    actionSheetExpense?.let { exp ->
        IOSActionSheet(
            title = exp.title,
            actions = listOf(
                IOSAction("Modifier") { editing = exp },
                IOSAction("Supprimer", isDestructive = true) { pendingDelete = exp },
            ),
            onDismiss = { actionSheetExpense = null },
        )
    }

    // Delete confirmation
    pendingDelete?.let { exp ->
        IOSAlertDialog(
            title = "Supprimer la dépense ?",
            message = "Cette action est définitive.",
            confirmText = "Supprimer",
            cancelText = "Annuler",
            isDestructive = true,
            onConfirm = {
                vm.deleteExpense(voyageId, exp.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    // Remove participant confirmation
    pendingRemoveParticipant?.let { p ->
        IOSAlertDialog(
            title = "Retirer ${p.display_name} ?",
            message = "Le participant sera retiré du voyage.",
            confirmText = "Retirer",
            cancelText = "Annuler",
            isDestructive = true,
            onConfirm = {
                vm.removeParticipant(voyageId, p.id)
                pendingRemoveParticipant = null
            },
            onDismiss = { pendingRemoveParticipant = null },
        )
    }

    if (showAddGuestDialog) {
        TextInputDialog(
            title = "Ajouter un invité",
            placeholder = "Nom de l'invité",
            confirmText = "Ajouter",
            onConfirm = { name ->
                if (name.isNotBlank()) vm.addGuest(voyageId, name.trim())
                showAddGuestDialog = false
            },
            onDismiss = { showAddGuestDialog = false },
        )
    }

    if (showInviteEmailDialog) {
        TextInputDialog(
            title = "Inviter par email",
            placeholder = "email@exemple.com",
            confirmText = "Inviter",
            isEmail = true,
            onConfirm = { email ->
                if (email.isNotBlank()) vm.inviteByEmail(voyageId, email.trim())
                showInviteEmailDialog = false
            },
            onDismiss = { showInviteEmailDialog = false },
        )
    }
}

// ============================================================
// Dépenses tab
// ============================================================
@Composable
private fun ExpensesTabContent(
    expenses: List<VoyageExpense>,
    settlement: be.reveetvoyage.app.data.model.SettlementResponse?,
    isLoading: Boolean,
    onExpenseTap: (VoyageExpense) -> Unit,
) {
    if (isLoading && expenses.isEmpty()) {
        LoadingFull()
        return
    }
    if (expenses.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TotalCard(total = settlement?.total ?: 0.0, byCategory = settlement?.by_category ?: emptyMap())
            EmptyState(
                icon = Icons.Default.Receipt,
                title = "Aucune dépense",
                subtitle = "Appuie sur + pour ajouter la première.",
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TotalCard(total = settlement?.total ?: 0.0, byCategory = settlement?.by_category ?: emptyMap())
        }
        items(expenses, key = { it.id }) { exp ->
            ExpenseRow(exp, onClick = { onExpenseTap(exp) })
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun TotalCard(total: Double, byCategory: Map<String, Long>) {
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = 18) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Total dépensé", color = RevTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                formatAmount(total),
                color = RevBrown,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
            )
            if (byCategory.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(byCategory.entries.toList(), key = { it.key }) { (key, cents) ->
                        val cat = categoryFor(key)
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(cat.color.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(13.dp))
                            Text(
                                formatAmount(cents / 100.0),
                                color = cat.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(exp: VoyageExpense, onClick: () -> Unit) {
    val cat = categoryFor(exp.category)
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }, padding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(cat.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(exp.title, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Payé par ${exp.paid_by?.display_name ?: "—"}",
                    color = RevTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
                if (!exp.location_name.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.Place,
                            null, tint = RevOrange, modifier = Modifier.size(11.dp),
                        )
                        Text(
                            exp.location_name!!,
                            color = RevOrange,
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatAmount(exp.amount, exp.currency),
                    color = RevBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(2.dp))
                exp.spent_at?.take(10)?.let {
                    Text(it, color = RevTextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

// ============================================================
// Solde tab
// ============================================================
@Composable
private fun SoldeTabContent(
    settlement: be.reveetvoyage.app.data.model.SettlementResponse?,
    isLoading: Boolean,
) {
    if (isLoading && settlement == null) {
        LoadingFull()
        return
    }
    val balances = settlement?.balances.orEmpty()
    val transactions = settlement?.transactions.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (balances.isEmpty() && transactions.isEmpty()) {
            EmptyState(
                icon = Icons.Default.AccountBalance,
                title = "Pas encore de solde",
                subtitle = "Ajoute des dépenses pour voir qui doit quoi.",
            )
            return@Column
        }

        SectionTitle("Soldes par participant", Icons.Default.AccountBalance)
        GlassCard(modifier = Modifier.fillMaxWidth(), padding = 8) {
            Column {
                balances.forEachIndexed { i, b ->
                    if (i > 0) HorizontalDivider(color = Color(0x14000000), thickness = 0.5.dp)
                    BalanceRow(b)
                }
            }
        }

        SectionTitle("Pour équilibrer", Icons.Default.SwapHoriz)
        if (transactions.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Équilibré 🎉",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Personne ne doit rien à personne.",
                        color = RevTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 8) {
                Column {
                    transactions.forEachIndexed { i, t ->
                        if (i > 0) HorizontalDivider(color = Color(0x14000000), thickness = 0.5.dp)
                        TransactionRow(t)
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun BalanceRow(b: SettlementBalance) {
    val isCredit = b.balance_cents >= 0
    val color = if (isCredit) Color(0xFF10B981) else RevOrange
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isCredit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(b.name, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(
            (if (isCredit) "+" else "") + formatAmount(b.balance),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun TransactionRow(t: SettlementTransaction) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(t.from_name, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = RevOrange, modifier = Modifier.size(14.dp))
                Text(t.to_name, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text("doit à", color = RevTextSecondary, fontSize = 11.sp)
        }
        Text(
            formatAmount(t.amount),
            color = RevOrange,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

// ============================================================
// Participants tab
// ============================================================
@Composable
private fun ParticipantsTabContent(
    participants: List<VoyageParticipant>,
    isLoading: Boolean,
    onRemove: (VoyageParticipant) -> Unit,
    onAddGuest: () -> Unit,
    onInviteEmail: () -> Unit,
) {
    if (isLoading && participants.isEmpty()) {
        LoadingFull()
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("Participants (${participants.size})", Icons.Default.Group)
        if (participants.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Aucun participant pour le moment.",
                    color = RevTextSecondary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 8) {
                Column {
                    participants.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(color = Color(0x14000000), thickness = 0.5.dp)
                        ParticipantRow(p, onRemove = { onRemove(p) })
                    }
                }
            }
        }
        IOSButton(
            text = "Ajouter un invité",
            onClick = onAddGuest,
            icon = Icons.Default.PersonAdd,
            style = IOSButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        IOSButton(
            text = "Inviter par email",
            onClick = onInviteEmail,
            icon = Icons.Default.Email,
            style = IOSButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ParticipantRow(p: VoyageParticipant, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val initials = p.display_name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(RevYellow, RevOrange, RevRed))),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials.ifBlank { "?" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(p.display_name, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (p.is_guest) {
                Spacer(Modifier.height(2.dp))
                StatusBadge("Invité", BadgeKind.Neutral)
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.DeleteOutline, null, tint = RevRed, modifier = Modifier.size(20.dp))
        }
    }
}

// ============================================================
// Helper: simple text input dialog
// ============================================================
@Composable
private fun TextInputDialog(
    title: String,
    placeholder: String,
    confirmText: String,
    isEmail: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = RevCardBackground,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(title, color = RevBrown, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    IOSTextField(
                        value = value,
                        onValueChange = { value = it },
                        placeholder = placeholder,
                        icon = if (isEmail) Icons.Default.Email else Icons.Default.Person,
                        keyboardOptions = if (isEmail) androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email) else androidx.compose.foundation.text.KeyboardOptions.Default,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IOSButton(
                            text = "Annuler",
                            onClick = onDismiss,
                            style = IOSButtonStyle.Secondary,
                            modifier = Modifier.weight(1f),
                        )
                        IOSButton(
                            text = confirmText,
                            onClick = { onConfirm(value) },
                            style = IOSButtonStyle.Primary,
                            modifier = Modifier.weight(1f),
                            enabled = value.isNotBlank(),
                        )
                    }
                }
            }
        }
    }
}
