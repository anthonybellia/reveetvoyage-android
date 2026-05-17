package be.reveetvoyage.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.reveetvoyage.app.data.model.ApiOwner
import be.reveetvoyage.app.ui.theme.RevBrown
import be.reveetvoyage.app.ui.theme.RevYellow

/**
 * Discreet banner shown at the top of admin-facing screens to remind the
 * viewer they are seeing the global (cross-user) view instead of their
 * personal data. Read-only badge — does not add any admin actions.
 *
 * Render this conditionally: `if (user?.role == "admin") AdminBanner()`.
 *
 * REFACTOR: belongs in `ui/components/` — sandbox-only location.
 */
@Composable
fun AdminBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = RevYellow.copy(alpha = 0.18f),
        border = BorderStroke(0.5.dp, RevYellow.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = RevBrown,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Mode admin · vue globale",
                color = RevBrown,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Sub-row displayed inside Devis/Voyage cards to surface the owning
 * customer when the admin is viewing the global list.
 *
 * Pass `null` to render nothing — keeps the call site idiomatic:
 *     `OwnerRow(devis.owner)`
 *
 * Today `devis.owner` doesn't yet exist on the model (see refactor note in
 * AdminModels.kt). Once the field is added to `Devis`/`Voyage`, the call
 * sites in `HomeScreen.kt`, `DevisScreens.kt` and `VoyagesScreens.kt` will
 * start lighting up automatically for admin users.
 */
@Composable
fun OwnerRow(owner: ApiOwner?, modifier: Modifier = Modifier) {
    if (owner == null) return
    val name = owner.fullName.takeIf { it.isNotBlank() }
    val email = owner.email
    val parts = listOfNotNull(name, email)
    if (parts.isEmpty()) return
    Text(
        text = "👤 " + parts.joinToString(" · "),
        color = androidx.compose.ui.graphics.Color(0xFF8A8A8A),
        fontSize = 12.sp,
        modifier = modifier,
    )
}
