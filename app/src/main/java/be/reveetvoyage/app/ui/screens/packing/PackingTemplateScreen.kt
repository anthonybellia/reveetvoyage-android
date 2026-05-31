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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import be.reveetvoyage.app.data.model.PackingCategory
import be.reveetvoyage.app.data.model.PackingTemplateItem
import be.reveetvoyage.app.ui.components.*
import be.reveetvoyage.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingTemplateScreen(
    onBack: () -> Unit,
    vm: PackingTemplateViewModel = hiltViewModel(),
) {
    val categories by vm.categories.collectAsState()
    val items by vm.items.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PackingTemplateItem?>(null) }
    var pendingDelete by remember { mutableStateOf<PackingTemplateItem?>(null) }

    val grouped = remember(items, categories) { vm.grouped(categories) }

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        IOSTopBar(title = "Ma liste de base", onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(RevYellow.copy(alpha = .08f), RevBackground))),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Info blurb
                Text(
                    "Ces articles seront proposés automatiquement pour chaque nouveau voyage.",
                    color = RevTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )

                when {
                    isLoading && items.isEmpty() -> LoadingFull()
                    items.isEmpty() -> EmptyState(
                        icon = Icons.Default.Inventory2,
                        title = "Liste de base vide",
                        subtitle = "Appuie sur + pour créer ta liste type.",
                    )
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            grouped.forEach { (category, catItems) ->
                                item(key = "header-${category?.id ?: "none"}") {
                                    TemplateCategoryHeader(category)
                                }
                                items(catItems, key = { it.id }) { item ->
                                    TemplateItemRow(
                                        item = item,
                                        onEdit = { editing = item },
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

    // Add sheet
    if (showAddSheet) {
        TemplateItemSheet(
            categories = categories,
            initial = null,
            onDismiss = { showAddSheet = false },
            onSave = { label, categoryId ->
                vm.addItem(label, categoryId) { showAddSheet = false }
            },
        )
    }

    // Edit sheet
    editing?.let { item ->
        TemplateItemSheet(
            categories = categories,
            initial = item,
            onDismiss = { editing = null },
            onSave = { label, categoryId ->
                vm.updateItem(item.id, label, categoryId) { editing = null }
            },
        )
    }

    // Delete confirmation
    pendingDelete?.let { item ->
        IOSAlertDialog(
            title = "Supprimer \"${item.label}\" ?",
            message = "L'article sera retiré de ta liste de base.",
            confirmText = "Supprimer",
            cancelText = "Annuler",
            isDestructive = true,
            onConfirm = {
                vm.deleteItem(item.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

// ============================================================
// Category header
// ============================================================
@Composable
private fun TemplateCategoryHeader(category: PackingCategory?) {
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
// Template item row
// ============================================================
@Composable
private fun TemplateItemRow(
    item: PackingTemplateItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = 4) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp),
        ) {
            Text(
                item.label,
                color = RevBrown,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    null,
                    tint = RevOrange.copy(alpha = .8f),
                    modifier = Modifier.size(18.dp),
                )
            }
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

// ============================================================
// Add / Edit sheet for template items
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateItemSheet(
    categories: List<PackingCategory>,
    initial: PackingTemplateItem?,
    onDismiss: () -> Unit,
    onSave: (label: String, categoryId: Int?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var selectedCategoryId by remember { mutableStateOf(initial?.category_id) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RevBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = RevTextSecondary.copy(alpha = 0.4f)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (initial == null) "Ajouter à la liste de base" else "Modifier l'article",
                color = RevBrown,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )

            PackingFieldLabel("Article")
            IOSTextField(
                value = label,
                onValueChange = { label = it },
                placeholder = "Ex: Chargeur, brosse à dents…",
                icon = Icons.Default.Label,
            )

            if (categories.isNotEmpty()) {
                PackingFieldLabel("Catégorie (optionnel)")
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories, key = { it.id }) { cat ->
                        val isSel = cat.id == selectedCategoryId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSel) Brush.horizontalGradient(listOf(RevOrange, RevRed))
                                    else Brush.horizontalGradient(listOf(RevCardBackground, RevCardBackground))
                                )
                                .clickable { selectedCategoryId = if (isSel) null else cat.id }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    iconForPackingCategory(cat.icon_key),
                                    null,
                                    tint = if (isSel) Color.White else RevOrange,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    cat.name,
                                    color = if (isSel) Color.White else RevBrown,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            IOSButton(
                text = if (initial == null) "Ajouter" else "Enregistrer",
                onClick = { onSave(label.trim(), selectedCategoryId) },
                style = IOSButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
                enabled = label.isNotBlank(),
            )
        }
    }
}
