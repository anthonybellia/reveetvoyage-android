package be.reveetvoyage.app.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import be.reveetvoyage.app.data.model.VoyageExpense
import be.reveetvoyage.app.data.model.VoyageExpenseSplit
import be.reveetvoyage.app.data.model.VoyageParticipant
import be.reveetvoyage.app.ui.components.IOSButton
import be.reveetvoyage.app.ui.components.IOSButtonStyle
import be.reveetvoyage.app.ui.components.IOSTextField
import be.reveetvoyage.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    participants: List<VoyageParticipant>,
    initial: VoyageExpense?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        paidByParticipantId: Int,
        splits: List<VoyageExpenseSplit>,
        category: String,
        spentAt: String?,
        description: String?,
        locationName: String?,
        locationLatitude: Double?,
        locationLongitude: Double?,
    ) -> Unit,
    vm: ExpensesViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amountStr by remember {
        mutableStateOf(initial?.let { String.format(Locale.US, "%.2f", it.amount) } ?: "")
    }
    var paidBy by remember(participants) {
        mutableStateOf(initial?.paid_by_participant_id ?: participants.firstOrNull()?.id)
    }
    var splitSelection by remember(participants) {
        mutableStateOf(
            initial?.splits?.map { it.participant_id }?.toSet()
                ?: participants.map { it.id }.toSet()
        )
    }
    var category by remember { mutableStateOf(initial?.category ?: "autre") }
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var spentAt by remember {
        mutableStateOf(initial?.spent_at?.take(10) ?: dateFmt.format(Date()))
    }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    var locationName by remember { mutableStateOf(initial?.location_name) }
    var locationLat by remember { mutableStateOf(initial?.location_latitude) }
    var locationLng by remember { mutableStateOf(initial?.location_longitude) }
    var showPlaceSheet by remember { mutableStateOf(false) }

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
                if (initial == null) "Nouvelle dépense" else "Modifier la dépense",
                color = RevBrown,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )

            FieldLabel("Titre")
            IOSTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Ex: Restaurant chez Mario",
                icon = Icons.Default.Title,
            )

            FieldLabel("Montant")
            IOSTextField(
                value = amountStr,
                onValueChange = { v -> amountStr = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                placeholder = "0,00",
                icon = Icons.Default.Euro,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            FieldLabel("Catégorie")
            CategoryRow(selected = category, onSelect = { category = it })

            FieldLabel("Payé par")
            ParticipantRow(
                participants = participants,
                isSelected = { it == paidBy },
                onSelect = { paidBy = it },
            )

            FieldLabel("Partagé entre")
            ParticipantRow(
                participants = participants,
                isSelected = { it in splitSelection },
                onSelect = { id ->
                    splitSelection = if (id in splitSelection) {
                        (splitSelection - id).ifEmpty { setOf(id) }
                    } else splitSelection + id
                },
            )

            FieldLabel("Date")
            DateField(
                value = spentAt,
                onClick = { showDatePicker = true },
            )

            FieldLabel("Lieu (optionnel)")
            LocationField(
                name = locationName,
                hasCoords = locationLat != null && locationLng != null,
                onClick = { showPlaceSheet = true },
                onClear = {
                    locationName = null
                    locationLat = null
                    locationLng = null
                },
            )

            FieldLabel("Description (optionnel)")
            MultilineField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Ajoute une note…",
            )

            Spacer(Modifier.height(8.dp))

            val parsedAmount = amountStr.replace(',', '.').toDoubleOrNull() ?: 0.0
            val canSave = title.isNotBlank() && parsedAmount > 0 && paidBy != null && splitSelection.isNotEmpty()

            IOSButton(
                text = "Enregistrer",
                onClick = {
                    val splits = splitSelection.map { VoyageExpenseSplit(it, 1.0) }
                    onSave(
                        title.trim(),
                        parsedAmount,
                        paidBy!!,
                        splits,
                        category,
                        spentAt,
                        description.takeIf { it.isNotBlank() },
                        locationName?.takeIf { it.isNotBlank() },
                        locationLat,
                        locationLng,
                    )
                },
                style = IOSButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
            )
        }
    }

    if (showPlaceSheet) {
        PlaceAutocompleteSheet(
            vm = vm,
            onDismiss = { showPlaceSheet = false },
            onSelect = { place ->
                locationName = place.name
                locationLat = place.latitude
                locationLng = place.longitude
                showPlaceSheet = false
            },
        )
    }

    if (showDatePicker) {
        val initMillis = runCatching { dateFmt.parse(spentAt)?.time }.getOrNull() ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        spentAt = dateFmt.format(cal.time)
                    }
                    showDatePicker = false
                }) { Text("OK", color = RevOrange, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler", color = RevTextSecondary)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        color = RevOrange,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
}

@Composable
private fun CategoryRow(selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ExpenseCategories, key = { it.key }) { cat ->
            val isSel = cat.key == selected
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSel) Brush.horizontalGradient(listOf(cat.color, cat.color))
                        else Brush.horizontalGradient(listOf(RevCardBackground, RevCardBackground))
                    )
                    .border(
                        width = if (isSel) 0.dp else 0.5.dp,
                        color = if (isSel) Color.Transparent else Color(0x14000000),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(cat.key) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    cat.icon,
                    null,
                    tint = if (isSel) Color.White else cat.color,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    cat.label,
                    color = if (isSel) Color.White else RevBrown,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ParticipantRow(
    participants: List<VoyageParticipant>,
    isSelected: (Int) -> Boolean,
    onSelect: (Int) -> Unit,
) {
    if (participants.isEmpty()) {
        Text(
            "Aucun participant — ajoute-en dans l'onglet Participants.",
            color = RevTextSecondary,
            fontSize = 12.sp,
        )
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(participants, key = { it.id }) { p ->
            val sel = isSelected(p.id)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (sel) Brush.horizontalGradient(listOf(RevOrange, RevRed))
                        else Brush.horizontalGradient(listOf(RevCardBackground, RevCardBackground))
                    )
                    .border(
                        width = if (sel) 0.dp else 0.5.dp,
                        color = if (sel) Color.Transparent else Color(0x14000000),
                        shape = CircleShape,
                    )
                    .clickable { onSelect(p.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    p.display_name,
                    color = if (sel) Color.White else RevBrown,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun LocationField(
    name: String?,
    hasCoords: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    val isSet = !name.isNullOrBlank()
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = RevCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.LocationOn,
                null,
                tint = if (isSet) RevOrange else RevTextSecondary,
                modifier = Modifier.size(18.dp),
            )
            if (isSet) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        name!!,
                        color = RevBrown,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    if (hasCoords) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Position enregistrée",
                            color = RevTextSecondary,
                            fontSize = 11.sp,
                        )
                    }
                }
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = RevTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                Text(
                    "Ajouter un lieu",
                    color = RevTextSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DateField(value: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(52.dp).clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = RevCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = RevTextSecondary, modifier = Modifier.size(18.dp))
            Text(value, color = RevBrown, fontSize = 16.sp, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MultilineField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
        shape = RoundedCornerShape(14.dp),
        color = RevCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Notes, null, tint = RevTextSecondary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                cursorBrush = SolidColor(RevOrange),
                textStyle = TextStyle(color = RevBrown, fontSize = 15.sp),
                minLines = 3,
                maxLines = 6,
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, color = RevTextSecondary, fontSize = 15.sp)
                        }
                        inner()
                    }
                },
            )
        }
    }
}
