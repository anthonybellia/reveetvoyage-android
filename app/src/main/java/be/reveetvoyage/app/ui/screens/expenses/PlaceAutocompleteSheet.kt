package be.reveetvoyage.app.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.reveetvoyage.app.data.model.Place
import be.reveetvoyage.app.ui.components.IOSTextField
import be.reveetvoyage.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceAutocompleteSheet(
    vm: ExpensesViewModel,
    onDismiss: () -> Unit,
    onSelect: (Place) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.length < 2) {
            results = emptyList()
            isSearching = false
            hasSearched = false
            return@LaunchedEffect
        }
        delay(350)
        isSearching = true
        results = runCatching { vm.searchPlaces(query) }.getOrDefault(emptyList())
        isSearching = false
        hasSearched = true
    }

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Rechercher un lieu",
                color = RevBrown,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )

            IOSTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Restaurant, hôtel, adresse…",
                icon = Icons.Default.Search,
            )

            when {
                query.length < 2 -> {
                    HintBlock(
                        icon = Icons.Default.LocationOn,
                        title = "Tape au moins 2 caractères",
                        subtitle = "Ex: Pizza chez Mario, Tour Eiffel, Hôtel Belair…",
                    )
                }
                isSearching && results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = RevOrange, strokeWidth = 2.5.dp)
                    }
                }
                hasSearched && results.isEmpty() -> {
                    HintBlock(
                        icon = Icons.Default.LocationOn,
                        title = "Aucun résultat",
                        subtitle = "Essaie un autre mot-clé.",
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(results, key = { it.name + "|" + it.address }) { place ->
                            PlaceRow(place = place, onClick = { onSelect(place) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(place: Place, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = RevCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconColorForType(place.type).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    iconForType(place.type),
                    null,
                    tint = iconColorForType(place.type),
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    place.name,
                    color = RevBrown,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    place.address,
                    color = RevTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun HintBlock(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = RevOrange.copy(alpha = 0.6f), modifier = Modifier.size(36.dp))
        Text(title, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(subtitle, color = RevTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

private fun iconForType(type: String?): ImageVector = when (type?.lowercase()) {
    "amenity", "restaurant", "cafe", "bar", "fast_food" -> Icons.Default.Restaurant
    "tourism", "hotel", "lodging" -> Icons.Default.Hotel
    "shop", "shopping" -> Icons.Default.ShoppingBag
    else -> Icons.Default.Place
}

private fun iconColorForType(type: String?): Color = when (type?.lowercase()) {
    "amenity", "restaurant", "cafe", "bar", "fast_food" -> RevOrange
    "tourism", "hotel", "lodging" -> Color(0xFF6366F1)
    "shop", "shopping" -> Color(0xFFEC4899)
    else -> RevBrown
}
