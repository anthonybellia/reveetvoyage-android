package be.reveetvoyage.app.ui.screens.packing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.reveetvoyage.app.data.model.PackingCategory
import be.reveetvoyage.app.ui.components.IOSButton
import be.reveetvoyage.app.ui.components.IOSButtonStyle
import be.reveetvoyage.app.ui.components.IOSTextField
import be.reveetvoyage.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPackingItemSheet(
    categories: List<PackingCategory>,
    onDismiss: () -> Unit,
    onSave: (label: String, categoryId: Int?, keepForNext: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var label by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var keepForNext by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RevBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = RevTextSecondary.copy(alpha = 0.4f)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Nouvel article",
                color = RevBrown,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )

            PackingFieldLabel("Article")
            IOSTextField(
                value = label,
                onValueChange = { label = it },
                placeholder = "Ex: Passeport, adaptateur USB…",
                icon = Icons.Default.Label,
            )

            if (categories.isNotEmpty()) {
                PackingFieldLabel("Catégorie (optionnel)")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        val isSel = cat.id == selectedCategoryId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSel) Brush.horizontalGradient(listOf(RevOrange, RevRed))
                                    else Brush.horizontalGradient(listOf(RevCardBackground, RevCardBackground))
                                )
                                .border(
                                    width = if (isSel) 0.dp else 0.5.dp,
                                    color = if (isSel) Color.Transparent else Color(0x14000000),
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable {
                                    selectedCategoryId = if (isSel) null else cat.id
                                }
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

            // "Garder pour mes prochains voyages" switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RevCardBackground)
                    .border(0.5.dp, Color(0x14000000), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Garder pour mes prochains voyages",
                        color = RevBrown,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Text(
                        "Ajouter à ma liste de base réutilisable",
                        color = RevTextSecondary,
                        fontSize = 11.sp,
                    )
                }
                Switch(
                    checked = keepForNext,
                    onCheckedChange = { keepForNext = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RevOrange,
                    ),
                )
            }

            Spacer(Modifier.height(4.dp))

            IOSButton(
                text = "Ajouter",
                onClick = { onSave(label.trim(), selectedCategoryId, keepForNext) },
                style = IOSButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
                enabled = label.isNotBlank(),
            )
        }
    }
}

@Composable
internal fun PackingFieldLabel(text: String) {
    Text(
        text.uppercase(),
        color = RevOrange,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
}
