package be.reveetvoyage.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import be.reveetvoyage.app.data.api.ApiConfig
import be.reveetvoyage.app.ui.theme.*
import coil.compose.AsyncImage

// ============================================================
// GlassCard — iOS-style flat card with hairline border
// ============================================================
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: Int = 16,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = RevCardBackground,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x14000000)),
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.padding(padding.dp)) { content() }
    }
}

// ============================================================
// IOSButton — three styles (primary, secondary, ghost)
// ============================================================
enum class IOSButtonStyle { Primary, Secondary, Ghost, Destructive }

@Composable
fun IOSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IOSButtonStyle = IOSButtonStyle.Primary,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "buttonScale",
    )

    val (background, contentColor, hasBorder) = when (style) {
        IOSButtonStyle.Primary -> Triple(
            Brush.horizontalGradient(listOf(RevOrange, RevRed)),
            Color.White,
            false,
        )
        IOSButtonStyle.Secondary -> Triple(
            Brush.horizontalGradient(listOf(RevCardBackground, RevCardBackground)),
            RevBrown,
            true,
        )
        IOSButtonStyle.Ghost -> Triple(
            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
            RevTextSecondary,
            false,
        )
        IOSButtonStyle.Destructive -> Triple(
            Brush.horizontalGradient(listOf(RevCardBackground, RevCardBackground)),
            RevRed,
            true,
        )
    }

    val shadowColor = if (style == IOSButtonStyle.Primary) RevOrange.copy(alpha = if (isPressed) 0.15f else 0.30f) else Color.Transparent
    val shadowRadius = if (style == IOSButtonStyle.Primary) (if (isPressed) 6.dp else 12.dp) else 0.dp

    Surface(
        modifier = modifier
            .height(52.dp)
            .scale(scale)
            .then(if (style == IOSButtonStyle.Primary) Modifier.shadow(shadowRadius, RoundedCornerShape(14.dp), spotColor = shadowColor) else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = if (hasBorder) androidx.compose.foundation.BorderStroke(1.dp, Color(0x1A000000)) else null,
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    icon?.let {
                        Icon(it, null, tint = contentColor, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = text,
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

// ============================================================
// IOSTextField — iOS-style input with leading icon
// ============================================================
@Composable
fun IOSTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsPressedAsState()
    var hasFocus by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        color = RevCardBackground,
        border = androidx.compose.foundation.BorderStroke(
            if (hasFocus) 1.5.dp else 1.dp,
            if (hasFocus) RevOrange.copy(alpha = 0.6f) else Color(0x14000000),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            icon?.let {
                Icon(
                    it, null,
                    tint = if (hasFocus) RevOrange else RevTextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }

            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { hasFocus = it.isFocused },
                singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(RevOrange),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = RevBrown,
                    fontSize = 16.sp,
                ),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = keyboardOptions,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(placeholder, color = RevTextSecondary, fontSize = 16.sp)
                        }
                        inner()
                    }
                },
            )
        }
    }
}

// ============================================================
// IOSTopBar — flat top bar, bold centered title, back chevron
// ============================================================
@Composable
fun IOSTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = RevBackground,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Back",
                        tint = RevOrange,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = title,
                color = RevBrown,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.align(Alignment.Center),
            )
            trailing?.let {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) { it() }
            }
        }
    }
}

// ============================================================
// IOSAlertDialog — iOS-style alert (stacked buttons, centered, blurred backdrop feel)
// ============================================================
@Composable
fun IOSAlertDialog(
    title: String,
    message: String? = null,
    confirmText: String,
    cancelText: String? = "Annuler",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = RevCardBackground.copy(alpha = 0.98f),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            title,
                            color = RevBrown,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        if (message != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                message,
                                color = RevTextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(color = Color(0x14000000), thickness = 0.5.dp)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = onConfirm,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(0.dp),
                        ) {
                            Text(
                                confirmText,
                                color = if (isDestructive) RevRed else RevOrange,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                        }

                        if (cancelText != null) {
                            androidx.compose.material3.HorizontalDivider(color = Color(0x14000000), thickness = 0.5.dp)
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(0.dp),
                            ) {
                                Text(
                                    cancelText,
                                    color = RevTextSecondary,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// IOSActionSheet — iOS bottom action sheet alternative
// ============================================================
data class IOSAction(val label: String, val isDestructive: Boolean = false, val onClick: () -> Unit)

@Composable
fun IOSActionSheet(
    title: String? = null,
    actions: List<IOSAction>,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = RevCardBackground,
                ) {
                    Column {
                        if (title != null) {
                            Text(
                                title,
                                color = RevTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            androidx.compose.material3.HorizontalDivider(color = Color(0x14000000), thickness = 0.5.dp)
                        }
                        actions.forEachIndexed { idx, a ->
                            if (idx > 0) {
                                androidx.compose.material3.HorizontalDivider(color = Color(0x14000000), thickness = 0.5.dp)
                            }
                            TextButton(
                                onClick = { a.onClick(); onDismiss() },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(0.dp),
                            ) {
                                Text(
                                    a.label,
                                    color = if (a.isDestructive) RevRed else RevOrange,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = RevCardBackground,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(0.dp),
                    ) {
                        Text(
                            "Annuler",
                            color = RevOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// AvatarView — gradient circle with initials, optional URL
// ============================================================
@Composable
fun AvatarView(
    firstName: String,
    lastName: String,
    avatarPath: String? = null,
    size: Int = 44,
) {
    val initials = "${firstName.firstOrNull() ?: ' '}${lastName.firstOrNull() ?: ' '}".uppercase().trim()
    val avatarUrl: String? = avatarPath?.takeIf { it.isNotBlank() }?.let {
        if (it.startsWith("http")) it else ApiConfig.SITE_BASE + it
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(RevYellow, RevOrange, RevRed))
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.4f).sp,
            )
        }
    }
}

// ============================================================
// StatusBadge — colored pill
// ============================================================
enum class BadgeKind { Neutral, Info, Success, Warning, Danger, Brand }

@Composable
fun StatusBadge(label: String, kind: BadgeKind = BadgeKind.Neutral) {
    val (bg, fg) = when (kind) {
        BadgeKind.Neutral -> Color.Gray.copy(alpha = 0.12f) to Color(0xFF6B6B6B)
        BadgeKind.Info    -> Color(0xFF2196F3).copy(alpha = 0.12f) to Color(0xFF1976D2)
        BadgeKind.Success -> Color(0xFF4CAF50).copy(alpha = 0.15f) to Color(0xFF2E7D32)
        BadgeKind.Warning -> RevYellow.copy(alpha = 0.25f) to Color(0xFF806600)
        BadgeKind.Danger  -> RevRed.copy(alpha = 0.15f) to RevRed
        BadgeKind.Brand   -> RevOrange.copy(alpha = 0.18f) to Color(0xFFC76426)
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

fun voyageStatutKind(statut: String): BadgeKind = when (statut) {
    "en_preparation" -> BadgeKind.Warning
    "confirme"       -> BadgeKind.Info
    "en_cours"       -> BadgeKind.Brand
    "termine"        -> BadgeKind.Success
    "annule"         -> BadgeKind.Danger
    else             -> BadgeKind.Neutral
}

fun devisStatutLabel(statut: String): Pair<String, BadgeKind> = when (statut) {
    "nouveau"  -> "Nouveau"       to BadgeKind.Info
    "en_cours" -> "En traitement" to BadgeKind.Brand
    "valide"   -> "Validé"        to BadgeKind.Success
    "refuse"   -> "Refusé"        to BadgeKind.Danger
    "archive"  -> "Archivé"       to BadgeKind.Neutral
    else       -> statut.replaceFirstChar { it.uppercase() } to BadgeKind.Neutral
}

// ============================================================
// SectionTitle
// ============================================================
@Composable
fun SectionTitle(title: String, icon: ImageVector? = null, trailing: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (icon != null) {
            Icon(icon, null, tint = RevOrange, modifier = Modifier.size(18.dp))
        }
        Text(title, color = RevBrown, fontWeight = FontWeight.Bold, fontSize = 18.sp,
             modifier = Modifier.weight(1f))
        trailing?.let {
            Text(it, color = RevTextSecondary, fontSize = 12.sp)
        }
    }
}

// ============================================================
// LoadingFull / EmptyState
// ============================================================
@Composable
fun LoadingFull() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = RevOrange)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = RevOrange.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        subtitle?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = RevTextSecondary, fontSize = 13.sp)
        }
    }
}
