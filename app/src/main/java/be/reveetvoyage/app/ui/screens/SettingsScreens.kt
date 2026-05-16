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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

private val RowDividerColor = Color(0x14000000)

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = RowDividerColor,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp),
    )
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
    onOpenPage: (slug: String, title: String) -> Unit,
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
                        "Nom, email, téléphone", onClick = onOpenEditProfile)
                    RowDivider()
                    SettingsRow(Icons.Default.Lock, "Mot de passe",
                        "Modifier mon mot de passe", onClick = onOpenChangePassword)
                    RowDivider()
                    SettingsRow(Icons.Default.Email, "Mes messages",
                        "Discussions avec l'équipe", onClick = onOpenMessages)
                }
            }

            // App
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0) {
                Column {
                    SettingsRow(Icons.Default.Notifications, "Notifications",
                        "Email + rappels voyage", onClick = onOpenNotifications)
                    RowDivider()
                    val langLabel = when (user?.language) {
                        "en" -> "English"; "nl" -> "Nederlands"; else -> "Français"
                    }
                    SettingsRow(Icons.Default.Language, "Langue", value = langLabel, onClick = onOpenLanguage)
                }
            }

            // Liens utiles
            val linksContext = androidx.compose.ui.platform.LocalContext.current
            Text("LIENS UTILES", color = RevTextSecondary, fontSize = 11.sp,
                 fontWeight = FontWeight.SemiBold,
                 modifier = Modifier.padding(start = 8.dp, top = 4.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0) {
                Column {
                    SettingsRow(Icons.Default.Public, "Site web",
                        "reveetvoyage.be",
                        onClick = { openUrl(linksContext, "https://www.reveetvoyage.be") })
                    RowDivider()
                    SettingsRow(Icons.Default.Phone, "Téléphone",
                        "+32 497 02 85 20",
                        onClick = { openUrl(linksContext, "tel:+32497028520") })
                    RowDivider()
                    SettingsRow(Icons.Default.MailOutline, "Email",
                        "contact@reveetvoyage.be",
                        onClick = { openUrl(linksContext, "mailto:contact@reveetvoyage.be") })
                    RowDivider()
                    SettingsRow(Icons.Default.PhotoCamera, "Instagram",
                        "@matilda_travelplanner",
                        onClick = { openUrl(linksContext, "https://www.instagram.com/matilda_travelplanner") })
                    RowDivider()
                    SettingsRow(Icons.Default.CreditCard, "Revolut",
                        "Carte voyage sans frais",
                        onClick = { openUrl(linksContext, "https://revolut.com/referral/?referral-code=mlarosa97!MAY2-26-AR-H1&geo-redirect") })
                    RowDivider()
                    SettingsRow(Icons.Default.Wifi, "Holafly",
                        "eSIM data internationale",
                        onClick = { openUrl(linksContext, "https://www.holafly.com/?ref=reveetvoyage") })
                    RowDivider()
                    SettingsRow(Icons.Default.Star, "Noter l'app",
                        "Sur le Play Store",
                        onClick = { openUrl(linksContext, "market://details?id=be.reveetvoyage.app") })
                    RowDivider()
                    SettingsRow(Icons.Default.Share, "Partager Rêve et Voyage",
                        "Envoie l'app à un ami",
                        onClick = { shareApp(linksContext) })
                }
            }

            // Légal
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0) {
                Column {
                    SettingsRow(Icons.Default.Description, "Conditions générales",
                        "Lire les CGU",
                        onClick = { onOpenPage("conditions-generales", "Conditions générales") })
                    RowDivider()
                    SettingsRow(Icons.Default.PrivacyTip, "Confidentialité",
                        "Protection des données",
                        onClick = { onOpenPage("politique-de-confidentialite", "Confidentialité") })
                    RowDivider()
                    SettingsRow(Icons.Default.Receipt, "Conditions de vente",
                        "CGV applicables",
                        onClick = { onOpenPage("conditions-de-vente", "Conditions de vente") })
                    RowDivider()
                    SettingsRow(Icons.Default.Help, "Aide",
                        "Contacte l'équipe Rêve et Voyage",
                        onClick = onOpenMessages)
                }
            }

            // Logout
            IOSButton(
                text = "Se déconnecter",
                onClick = { showLogoutDialog = true },
                style = IOSButtonStyle.Destructive,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text("Rêve et Voyage · v1.0", color = RevTextSecondary, fontSize = 11.sp,
                 modifier = Modifier.fillMaxWidth().wrapContentWidth())
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showLogoutDialog) {
        IOSAlertDialog(
            title = "Se déconnecter ?",
            message = "Tu devras te reconnecter pour accéder à ton compte.",
            confirmText = "Se déconnecter",
            cancelText = "Annuler",
            isDestructive = true,
            onConfirm = { showLogoutDialog = false; vm.logout(); onLogout() },
            onDismiss = { showLogoutDialog = false },
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = RevOrange, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = RevBrown, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            subtitle?.let { Text(it, color = RevTextSecondary, fontSize = 11.sp) }
        }
        value?.let {
            Text(it, color = RevTextSecondary, fontSize = 14.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = RevTextSecondary.copy(alpha = .5f))
    }
}

// ============================================================
// EditProfileScreen
// ============================================================
// Helpers — open url + share app
// ============================================================
private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (_: Throwable) {
        // Fallback for market:// when Play Store not installed → open web
        if (url.startsWith("market://")) {
            openUrl(context, "https://play.google.com/store/apps/details?id=be.reveetvoyage.app")
        }
    }
}

private fun shareApp(context: android.content.Context) {
    val text = "Découvre Rêve et Voyage — l'app pour organiser tes voyages 🌴 https://www.reveetvoyage.be"
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    val chooser = android.content.Intent.createChooser(intent, "Partager")
    chooser.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(chooser)
}

// ============================================================
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

    Column(
        modifier = Modifier.fillMaxSize().background(RevBackground),
    ) {
        IOSTopBar(
            title = "Mes informations",
            onBack = onBack,
            trailing = {
                TextButton(
                    onClick = {
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
                    },
                    enabled = !saving && prenom.isNotBlank() && nom.isNotBlank(),
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = RevOrange)
                    } else {
                        Text("OK", color = RevOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            },
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box {
                    AvatarView(prenom, nom, user?.avatar, size = 70)
                    if (avatarUploading) {
                        CircularProgressIndicator(
                            color = RevOrange,
                            modifier = Modifier.align(Alignment.Center),
                        )
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

            IOSTextField(prenom, { prenom = it }, placeholder = "Prénom")
            IOSTextField(nom, { nom = it }, placeholder = "Nom")
            IOSTextField(phone, { phone = it }, placeholder = "Téléphone")
            IOSTextField(nationalite, { nationalite = it }, placeholder = "Nationalité")

            HorizontalDivider(color = RowDividerColor, thickness = 0.5.dp)
            Text("Adresse", fontWeight = FontWeight.SemiBold, color = RevBrown)

            IOSTextField(adresse, { adresse = it }, placeholder = "Adresse")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IOSTextField(cp, { cp = it }, placeholder = "CP", modifier = Modifier.width(110.dp))
                IOSTextField(ville, { ville = it }, placeholder = "Ville", modifier = Modifier.weight(1f))
            }
            IOSTextField(pays, { pays = it }, placeholder = "Pays")
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ============================================================
// ChangePasswordScreen
// ============================================================
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

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        IOSTopBar(title = "Mot de passe", onBack = onBack)

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IOSTextField(current, { current = it }, placeholder = "Mot de passe actuel", isPassword = true)
            IOSTextField(newPwd, { newPwd = it }, placeholder = "Nouveau (min 8)", isPassword = true)
            IOSTextField(confirm, { confirm = it }, placeholder = "Confirmer", isPassword = true)

            msg?.let { Text(it, color = if (success) Color(0xFF2E7D32) else RevRed, fontSize = 12.sp) }

            IOSButton(
                text = "Mettre à jour",
                onClick = {
                    saving = true
                    scope.launch {
                        val (ok, message) = vm.savePassword(current, newPwd, confirm)
                        success = ok; msg = message
                        saving = false
                        if (ok) { kotlinx.coroutines.delay(1000); onBack() }
                    }
                },
                style = IOSButtonStyle.Primary,
                isLoading = saving,
                enabled = !saving && isValid,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ============================================================
// LanguageScreen
// ============================================================
@Composable
fun LanguageScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val user by vm.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val languages = listOf(
        Triple("fr", "Français", "🇫🇷"),
        Triple("en", "English", "🇬🇧"),
        Triple("nl", "Nederlands", "🇳🇱"),
    )

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        IOSTopBar(title = "Langue", onBack = onBack)

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        IOSTopBar(title = "Notifications", onBack = onBack)

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0) {
                Column {
                    NotifToggleRow(Icons.Default.Email, "Emails généraux", "Comptes, sécurité, factures",
                        notifEmails) { notifEmails = it; persist() }
                    HorizontalDivider(color = RowDividerColor, thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 52.dp))
                    NotifToggleRow(Icons.Default.Sell, "Emails promotionnels", "Bons plans, offres, newsletter",
                        notifPromo) { notifPromo = it; persist() }
                    HorizontalDivider(color = RowDividerColor, thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 52.dp))
                    NotifToggleRow(Icons.Default.Flight, "Notifications voyage", "Rappels J-15, J-7, J-2 et J-1",
                        notifVoyages) { notifVoyages = it; persist() }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Rappels Ryanair-style 15j / 7j / 48h / 24h avant le départ avec heure idéale d'arrivée à l'aéroport.",
                color = RevTextSecondary, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun NotifToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = RevOrange, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = RevTextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RevOrange,
            ),
        )
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

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenMessages: () -> Unit,
    vm: NotificationsScreenViewModel = hiltViewModel(),
) {
    val unread by vm.unread.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        IOSTopBar(title = "Notifications", onBack = onBack)

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(RevYellow.copy(alpha = .06f), RevBackground))
            )
        ) {
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
