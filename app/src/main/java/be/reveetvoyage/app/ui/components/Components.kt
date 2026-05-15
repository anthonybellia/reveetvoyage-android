package be.reveetvoyage.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.reveetvoyage.app.data.api.ApiConfig
import be.reveetvoyage.app.ui.theme.*
import coil.compose.AsyncImage

// ============================================================
// GlassCard — rounded card with subtle border + shadow
// ============================================================
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: Int = 16,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RevCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.padding(padding.dp)) { content() }
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
        BadgeKind.Neutral -> Color.Gray.copy(alpha = 0.15f) to Color.Gray
        BadgeKind.Info    -> Color(0xFF2196F3).copy(alpha = 0.15f) to Color(0xFF1976D2)
        BadgeKind.Success -> Color(0xFF4CAF50).copy(alpha = 0.18f) to Color(0xFF2E7D32)
        BadgeKind.Warning -> RevYellow.copy(alpha = 0.30f) to Color(0xFF806600)
        BadgeKind.Danger  -> RevRed.copy(alpha = 0.18f) to RevRed
        BadgeKind.Brand   -> RevOrange.copy(alpha = 0.20f) to Color(0xFFC76426)
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
