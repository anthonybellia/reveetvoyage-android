package be.reveetvoyage.app.ui.screens.packing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import be.reveetvoyage.app.data.model.PackingCategory
import be.reveetvoyage.app.data.model.VoyagePackingItem
import be.reveetvoyage.app.ui.components.*
import be.reveetvoyage.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ============================================================
// Icon + colour helpers for packing categories
// ============================================================
fun iconForPackingCategory(iconKey: String): ImageVector = when (iconKey.lowercase()) {
    "documents"   -> Icons.Default.Description
    "clothes"     -> Icons.Default.Checkroom
    "toiletries"  -> Icons.Default.Bathtub
    "electronics" -> Icons.Default.Bolt
    "health"      -> Icons.Default.MedicalServices
    "misc"        -> Icons.Default.Inventory2
    else          -> Icons.Default.Label
}

fun colorForPackingCategory(color: String?): Color = color?.let {
    runCatching {
        val argb = android.graphics.Color.parseColor(it)
        Color(
            red   = android.graphics.Color.red(argb)   / 255f,
            green = android.graphics.Color.green(argb) / 255f,
            blue  = android.graphics.Color.blue(argb)  / 255f,
            alpha = android.graphics.Color.alpha(argb) / 255f,
        )
    }.getOrNull()
} ?: RevOrange

// ============================================================
// PackingScreen
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingScreen(
    voyageId: Int,
    onBack: () -> Unit,
    onOpenTemplate: () -> Unit,
    vm: PackingViewModel = hiltViewModel(),
) {
    val categories by vm.categories.collectAsState()
    val items by vm.items.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<VoyagePackingItem?>(null) }

    LaunchedEffect(voyageId) { vm.load(voyageId) }

    // 5-second polling (same as ExpensesScreen)
    LaunchedEffect(voyageId) {
        while (isActive) {
            delay(5000)
            vm.refresh(voyageId)
        }
    }

    val isGenerating by vm.isGenerating.collectAsState()

    val grouped = remember(items, categories) { vm.grouped(categories) }
    val (checked, total) = remember(items) { vm.progress }

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        IOSTopBar(
            title = "Bagages",
            onBack = onBack,
            trailing = {
                TextButton(onClick = onOpenTemplate) {
                    Text(
                        "Ma liste",
                        color = RevOrange,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(RevYellow.copy(alpha = .08f), RevBackground))),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Progress bar
                if (total > 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Préparation",
                                color = RevBrown,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                            Text(
                                "$checked / $total",
                                color = RevOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (if (total > 0) checked.toFloat() / total else 0f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = RevOrange,
                            trackColor = Color.Gray.copy(alpha = .15f),
                        )
                    }
                }

                when {
                    isLoading && items.isEmpty() -> LoadingFull()
                    items.isEmpty() -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            null,
                            tint = RevOrange.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Liste de bagage vide", color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Génère une liste classique pour démarrer, ou appuie sur + pour ajouter un article.",
                            color = RevTextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        GenerateClassicButton(
                            isGenerating = isGenerating,
                            onClick = { vm.generateClassic(voyageId) },
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item(key = "generate-classic") {
                                GenerateClassicButton(
                                    isGenerating = isGenerating,
                                    onClick = { vm.generateClassic(voyageId) },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                )
                            }
                            grouped.forEach { (category, catItems) ->
                                item(key = "header-${category?.id ?: "none"}") {
                                    CategoryHeader(category)
                                }
                                items(catItems, key = { it.id }) { item ->
                                    PackingItemRow(
                                        item = item,
                                        onToggle = { vm.toggle(voyageId, item) },
                                        onDelete = { pendingDelete = item },
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
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

    if (showAddSheet) {
        AddPackingItemSheet(
            categories = categories,
            onDismiss = { showAddSheet = false },
            onSave = { label, categoryId, keepForNext ->
                vm.addItem(voyageId, label, categoryId, keepForNext) { showAddSheet = false }
            },
        )
    }

    pendingDelete?.let { item ->
        IOSAlertDialog(
            title = "Supprimer \"${item.label}\" ?",
            message = "Cette action est définitive.",
            confirmText = "Supprimer",
            cancelText = "Annuler",
            isDestructive = true,
            onConfirm = {
                vm.deleteItem(voyageId, item.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

// ============================================================
// "Générer une liste classique" button
// ============================================================
@Composable
private fun GenerateClassicButton(
    isGenerating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isGenerating,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, RevOrange),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = RevOrange),
    ) {
        if (isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = RevOrange,
            )
        } else {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Générer une liste classique", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

// ============================================================
// Category header row
// ============================================================
@Composable
private fun CategoryHeader(category: PackingCategory?) {
    val icon = category?.let { iconForPackingCategory(it.icon_key) } ?: Icons.Default.Label
    val color = category?.let { colorForPackingCategory(it.color) } ?: RevTextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            category?.name ?: "Autres",
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

// ============================================================
// Single item row with checkbox
// ============================================================
@Composable
private fun PackingItemRow(
    item: VoyagePackingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = 4) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
        ) {
            Checkbox(
                checked = item.is_checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = RevOrange,
                    uncheckedColor = RevTextSecondary.copy(alpha = .5f),
                    checkmarkColor = Color.White,
                ),
            )
            Text(
                item.label,
                color = if (item.is_checked) RevTextSecondary else RevBrown,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
                textDecoration = if (item.is_checked) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    null,
                    tint = RevRed.copy(alpha = .7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
