package be.reveetvoyage.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.model.Passenger
import be.reveetvoyage.app.data.model.PassengerRequest
import be.reveetvoyage.app.data.repo.PassengerRepository
import be.reveetvoyage.app.ui.components.*
import be.reveetvoyage.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PassengersViewModel @Inject constructor(private val repo: PassengerRepository) : ViewModel() {
    private val _list = MutableStateFlow<List<Passenger>>(emptyList())
    val list: StateFlow<List<Passenger>> = _list.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _isLoading.value = true
            _list.value = runCatching { repo.list() }.getOrDefault(emptyList())
            _isLoading.value = false
        }
    }

    suspend fun create(req: PassengerRequest): Boolean = runCatching {
        val p = repo.create(req)
        _list.value = _list.value + p
        true
    }.getOrDefault(false)

    suspend fun update(id: Int, req: PassengerRequest): Boolean = runCatching {
        val p = repo.update(id, req)
        _list.value = _list.value.map { if (it.id == id) p else it }
        true
    }.getOrDefault(false)

    suspend fun delete(id: Int) {
        runCatching { repo.delete(id); _list.value = _list.value.filterNot { it.id == id } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengersScreen(vm: PassengersViewModel = hiltViewModel()) {
    val list by vm.list.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Passenger?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(RevYellow.copy(alpha = .08f), RevBackground))
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            when {
                isLoading && list.isEmpty() -> LoadingFull()
                list.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.PersonAddAlt, null, tint = RevOrange.copy(alpha = .4f),
                         modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Aucun passager enregistré", color = RevBrown, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Ajoute des passagers pour gagner du temps lors d'un devis",
                         color = RevTextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { editing = null; showSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RevOrange),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Ajouter un passager", color = Color.White)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                ) {
                    items(list, key = { it.id }) { p ->
                        PassengerCard(
                            p,
                            onTap = { editing = p; showSheet = true },
                            onDelete = { scope.launch { vm.delete(p.id) } }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { editing = null; showSheet = true },
                containerColor = RevOrange,
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            ) { Icon(Icons.Default.Add, null) }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                PassengerForm(
                    initial = editing,
                    onCancel = { showSheet = false },
                    onSave = { req ->
                        val ok = if (editing != null) vm.update(editing!!.id, req) else vm.create(req)
                        if (ok) showSheet = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PassengerCard(p: Passenger, onTap: () -> Unit, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onTap() }) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AvatarView(p.prenom, p.nom, size = 48)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p.full_name, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (p.is_default) {
                        Icon(Icons.Default.Star, null, tint = RevYellow, modifier = Modifier.size(14.dp))
                    }
                }
                p.nationalite?.let { Text(it, color = RevTextSecondary, fontSize = 12.sp) }
                if (p.type_doc != null && p.num_doc != null) {
                    Text("${p.type_doc.uppercase()} · ${p.num_doc}", color = RevOrange, fontSize = 11.sp)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = RevTextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassengerForm(
    initial: Passenger?,
    onCancel: () -> Unit,
    onSave: suspend (PassengerRequest) -> Unit,
) {
    var prenom by remember { mutableStateOf(initial?.prenom ?: "") }
    var nom by remember { mutableStateOf(initial?.nom ?: "") }
    var nationalite by remember { mutableStateOf(initial?.nationalite ?: "") }
    var typeDoc by remember { mutableStateOf(initial?.type_doc ?: "") }
    var numDoc by remember { mutableStateOf(initial?.num_doc ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var isDefault by remember { mutableStateOf(initial?.is_default ?: false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (initial == null) "Nouveau passager" else "Modifier",
             fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RevBrown)

        OutlinedTextField(prenom, { prenom = it }, label = { Text("Prénom") },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
        OutlinedTextField(nom, { nom = it }, label = { Text("Nom") },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
        OutlinedTextField(nationalite, { nationalite = it }, label = { Text("Nationalité") },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(typeDoc, { typeDoc = it },
            label = { Text("Type de document (passeport / carte_identite)") },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(numDoc, { numDoc = it }, label = { Text("Numéro document") },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = isDefault, onCheckedChange = { isDefault = it },
                colors = SwitchDefaults.colors(checkedThumbColor = RevOrange))
            Spacer(Modifier.width(8.dp))
            Text("Passager principal", color = RevBrown)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)) { Text("Annuler") }
            Button(
                onClick = {
                    saving = true
                    scope.launch {
                        onSave(PassengerRequest(
                            nom = nom, prenom = prenom,
                            nationalite = nationalite.ifBlank { null },
                            type_doc = typeDoc.ifBlank { null },
                            num_doc = numDoc.ifBlank { null },
                            notes = notes.ifBlank { null },
                            is_default = isDefault,
                        ))
                        saving = false
                    }
                },
                enabled = !saving && prenom.isNotBlank() && nom.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RevOrange),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f),
            ) {
                if (saving) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp))
                else Text("Enregistrer", color = Color.White)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
