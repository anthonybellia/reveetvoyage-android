package be.reveetvoyage.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.model.UpdatePasswordRequest
import be.reveetvoyage.app.data.model.UpdatePreferencesRequest
import be.reveetvoyage.app.data.model.UpdateProfileRequest
import be.reveetvoyage.app.data.model.User
import be.reveetvoyage.app.data.repo.AuthRepository
import be.reveetvoyage.app.data.repo.UserRepository
import be.reveetvoyage.app.ui.components.*
import be.reveetvoyage.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    val userRepo: UserRepository,
) : ViewModel() {
    val currentUser: StateFlow<User?> = userRepo.currentUser

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast

    fun consumeToast() { _toast.value = null }

    init {
        viewModelScope.launch { userRepo.refresh() }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            userRepo.setCachedUser(null)
        }
    }

    suspend fun saveProfile(req: UpdateProfileRequest): Boolean = userRepo.updateProfile(req)

    suspend fun savePassword(current: String, new: String, confirm: String): Pair<Boolean, String?> {
        val res = userRepo.updatePassword(UpdatePasswordRequest(current, new, confirm))
        return if (res.isSuccess) true to "Mot de passe mis à jour"
               else false to (res.exceptionOrNull()?.message ?: "Erreur")
    }

    suspend fun savePreferences(req: UpdatePreferencesRequest): Boolean = userRepo.updatePreferences(req)

    suspend fun uploadAvatar(bytes: ByteArray): Boolean = userRepo.uploadAvatar(bytes)
}

// ============================================================
// SettingsScreen — main hub
// ============================================================
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMessages: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val user by vm.currentUser.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(RevYellow.copy(alpha = .10f), RevBackground))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Profile header
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 22) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                       modifier = Modifier.fillMaxWidth(),
                       verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (user == null) {
                        CircularProgressIndicator(color = RevOrange)
                    } else {
                        AvatarView(user!!.prenom, user!!.nom, avatarPath = user!!.avatar, size = 80)
                        Text(user!!.fullName, color = RevBrown, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(user!!.email, color = RevTextSecondary, fontSize = 13.sp)
                    }
                }
            }

            // Compte
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0) {
                Column {
                    SettingsRow(Icons.Default.Person, "Informations personnelles",
                        "Nom, email, téléphone", RevOrange, onClick = onOpenEditProfile)
                    Divider(modifier = Modifier.padding(start = 60.dp))
                    SettingsRow(Icons.Default.Lock, "Mot de passe",
                        "Modifier mon mot de passe", RevRed, onClick = onOpenChangePassword)
                    Divider(modifier = Modifier.padding(start = 60.dp))
                    SettingsRow(Icons.Default.Email, "Mes messages",
                        "Discussions avec l'équipe", RevYellow, onClick = onOpenMessages)
                }
            }

            // App
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0) {
                Column {
                    SettingsRow(Icons.Default.Notifications, "Notifications",
                        "Email + rappels voyage", RevOrange, onClick = onOpenNotifications)
                    Divider(modifier = Modifier.padding(start = 60.dp))
                    val langLabel = when (user?.language) {
                        "en" -> "English"; "nl" -> "Nederlands"; else -> "Français"
                    }
                    SettingsRow(Icons.Default.Language, "Langue", langLabel, RevOrange, onClick = onOpenLanguage)
                }
            }

            // Logout
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = RevRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Se déconnecter", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))
            Text("Rêve et Voyage · v1.0", color = RevTextSecondary, fontSize = 11.sp,
                 modifier = Modifier.fillMaxWidth().wrapContentWidth())
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Se déconnecter ?") },
            text = { Text("Tu devras te reconnecter pour accéder à ton compte.") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; vm.logout(); onLogout() }) {
                    Text("Se déconnecter", color = RevRed)
                }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(color.copy(alpha = .85f), color))),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = RevBrown, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = RevTextSecondary, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = RevTextSecondary.copy(alpha = .5f))
    }
}

// ============================================================
// EditProfileScreen
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val user by vm.currentUser.collectAsState()
    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nationalite by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var cp by remember { mutableStateOf("") }
    var ville by remember { mutableStateOf("") }
    var pays by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var avatarUploading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            avatarUploading = true
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) vm.uploadAvatar(bytes)
            avatarUploading = false
        }
    }

    LaunchedEffect(user) {
        user?.let {
            prenom = it.prenom; nom = it.nom; phone = it.phone.orEmpty()
            nationalite = it.nationalite.orEmpty(); adresse = it.adresse.orEmpty()
            cp = it.code_postal.orEmpty(); ville = it.ville.orEmpty(); pays = it.pays.orEmpty()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes informations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        saving = true
                        scope.launch {
                            vm.saveProfile(UpdateProfileRequest(
                                prenom = prenom, nom = nom,
                                phone = phone.ifBlank { null },
                                nationalite = nationalite.ifBlank { null },
                                adresse = adresse.ifBlank { null },
                                code_postal = cp.ifBlank { null },
                                ville = ville.ifBlank { null },
                                pays = pays.ifBlank { null },
                            ))
                            saving = false
                            onBack()
                        }
                    }, enabled = !saving && prenom.isNotBlank() && nom.isNotBlank()) {
                        if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = RevOrange)
                        else Text("Enregistrer", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
               verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Avatar
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box {
                    AvatarView(prenom, nom, user?.avatar, size = 70)
                    if (avatarUploading) {
                        CircularProgressIndicator(color = RevOrange,
                            modifier = Modifier.align(Alignment.Center))
                    }
                }
                Column {
                    TextButton(onClick = {
                        photoLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }) { Text("Changer ma photo", color = RevOrange, fontWeight = FontWeight.SemiBold) }
                    Text("Compression auto en WebP", color = RevTextSecondary, fontSize = 11.sp)
                }
            }

            OutlinedTextField(prenom, { prenom = it }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(nom, { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(nationalite, { nationalite = it }, label = { Text("Nationalité") }, modifier = Modifier.fillMaxWidth())
            Divider()
            Text("Adresse", fontWeight = FontWeight.SemiBold, color = RevBrown)
            OutlinedTextField(adresse, { adresse = it }, label = { Text("Adresse") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(cp, { cp = it }, label = { Text("CP") }, modifier = Modifier.width(110.dp))
                OutlinedTextField(ville, { ville = it }, label = { Text("Ville") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(pays, { pays = it }, label = { Text("Pays") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ============================================================
// ChangePasswordScreen
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    var current by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isValid = current.isNotEmpty() && newPwd.length >= 8 && newPwd == confirm

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mot de passe") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
               verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(current, { current = it }, label = { Text("Mot de passe actuel") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(newPwd, { newPwd = it }, label = { Text("Nouveau (min 8)") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(confirm, { confirm = it }, label = { Text("Confirmer") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

            msg?.let { Text(it, color = if (success) Color(0xFF2E7D32) else RevRed, fontSize = 12.sp) }

            Button(
                onClick = {
                    saving = true
                    scope.launch {
                        val (ok, message) = vm.savePassword(current, newPwd, confirm)
                        success = ok; msg = message
                        saving = false
                        if (ok) { kotlinx.coroutines.delay(1000); onBack() }
                    }
                },
                enabled = !saving && isValid,
                colors = ButtonDefaults.buttonColors(containerColor = RevOrange),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                if (saving) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Text("Mettre à jour", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ============================================================
// LanguageScreen
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val user by vm.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val languages = listOf(
        Triple("fr", "Français", "🇫🇷"),
        Triple("en", "English", "🇬🇧"),
        Triple("nl", "Nederlands", "🇳🇱"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Langue") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
               verticalArrangement = Arrangement.spacedBy(12.dp)) {
            languages.forEach { (code, label, flag) ->
                val selected = user?.language == code
                GlassCard(modifier = Modifier.fillMaxWidth().clickable {
                    scope.launch { vm.savePreferences(UpdatePreferencesRequest(language = code)) }
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(flag, fontSize = 32.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(code.uppercase(), color = RevTextSecondary, fontSize = 11.sp)
                        }
                        if (selected) Icon(Icons.Default.CheckCircle, null, tint = RevOrange,
                            modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

// ============================================================
// NotificationsSettingsScreen
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val user by vm.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    var notifEmails by remember { mutableStateOf(true) }
    var notifPromo by remember { mutableStateOf(true) }
    var notifVoyages by remember { mutableStateOf(true) }

    LaunchedEffect(user) {
        user?.let {
            notifEmails = it.notif_emails ?: true
            notifPromo = it.notif_promo ?: true
            notifVoyages = it.notif_voyages ?: true
        }
    }

    fun persist() {
        scope.launch {
            vm.savePreferences(UpdatePreferencesRequest(
                notif_emails = notifEmails, notif_promo = notifPromo, notif_voyages = notifVoyages
            ))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
               verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NotifToggle(Icons.Default.Email, "Emails généraux", "Comptes, sécurité, factures",
                notifEmails) { notifEmails = it; persist() }
            NotifToggle(Icons.Default.Sell, "Emails promotionnels", "Bons plans, offres, newsletter",
                notifPromo) { notifPromo = it; persist() }
            NotifToggle(Icons.Default.Flight, "Notifications voyage", "Rappels J-15, J-7, J-2 et J-1",
                notifVoyages) { notifVoyages = it; persist() }

            Spacer(Modifier.height(8.dp))
            Text("Rappels Ryanair-style 15j / 7j / 48h / 24h avant le départ avec heure idéale d'arrivée à l'aéroport.",
                 color = RevTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NotifToggle(icon: ImageVector, title: String, subtitle: String,
                        value: Boolean, onChange: (Boolean) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = RevOrange, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = RevTextSecondary, fontSize = 11.sp)
            }
            Switch(checked = value, onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedThumbColor = RevOrange,
                    checkedTrackColor = RevOrange.copy(alpha = .3f)))
        }
    }
}

// ============================================================
// NotificationsScreen — placeholder + unread messages count
// ============================================================
@HiltViewModel
class NotificationsScreenViewModel @Inject constructor(
    private val msgRepo: be.reveetvoyage.app.data.repo.MessageRepository,
) : ViewModel() {
    private val _unread = MutableStateFlow(0)
    val unread: StateFlow<Int> = _unread

    init { viewModelScope.launch { _unread.value = runCatching { msgRepo.unreadCount() }.getOrDefault(0) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit, onOpenMessages: () -> Unit,
                        vm: NotificationsScreenViewModel = hiltViewModel()) {
    val unread by vm.unread.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(
            Brush.verticalGradient(listOf(RevYellow.copy(alpha = .06f), RevBackground))
        )) {
            if (unread == 0) {
                EmptyState(Icons.Default.NotificationsOff, "Aucune notification",
                    "On te préviendra dès qu'il y aura du nouveau.")
            } else {
                Column(modifier = Modifier.padding(20.dp)) {
                    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenMessages)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(
                                Brush.linearGradient(listOf(RevOrange.copy(alpha = .85f), RevOrange))
                            ), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MarkEmailUnread, null, tint = Color.White)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Tu as $unread message${if (unread > 1) "s" else ""} non lu${if (unread > 1) "s" else ""}",
                                     color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("L'équipe Rêve et Voyage t'a répondu",
                                     color = RevTextSecondary, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = RevTextSecondary.copy(alpha = .5f))
                        }
                    }
                }
            }
        }
    }
}
