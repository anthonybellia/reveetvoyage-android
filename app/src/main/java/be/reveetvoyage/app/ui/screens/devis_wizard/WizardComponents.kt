package be.reveetvoyage.app.ui.screens.devis_wizard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.reveetvoyage.app.ui.theme.*

// ============================================================
// SelectableChip — chip selectable réutilisé (single & multi)
// ============================================================
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) RevOrange else RevCardBackground,
        label = "chipBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Color.White else RevBrown,
        label = "chipFg",
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) RevOrange else Color(0x22000000),
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
            }
            Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ============================================================
// WizardTextArea — OutlinedTextField multi-ligne, look cohérent
// ============================================================
@Composable
fun WizardTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
    maxLines: Int = 6,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = RevTextSecondary, fontSize = 14.sp) },
        textStyle = TextStyle(color = RevBrown, fontSize = 15.sp),
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RevOrange.copy(alpha = 0.7f),
            unfocusedBorderColor = Color(0x22000000),
            cursorColor = RevOrange,
            focusedContainerColor = RevCardBackground,
            unfocusedContainerColor = RevCardBackground,
        ),
    )
}

// ============================================================
// WizardSingleLineField — OutlinedTextField single line
// ============================================================
@Composable
fun WizardSingleLineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = RevTextSecondary, fontSize = 14.sp) },
        textStyle = TextStyle(color = RevBrown, fontSize = 15.sp),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        leadingIcon = leadingIcon?.let {
            { Icon(it, null, tint = RevTextSecondary, modifier = Modifier.size(18.dp)) }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RevOrange.copy(alpha = 0.7f),
            unfocusedBorderColor = Color(0x22000000),
            cursorColor = RevOrange,
            focusedContainerColor = RevCardBackground,
            unfocusedContainerColor = RevCardBackground,
        ),
    )
}

// ============================================================
// WizardSectionLabel
// ============================================================
@Composable
fun WizardSectionLabel(text: String, sublabel: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text, color = RevBrown, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        sublabel?.let {
            Text(it, color = RevTextSecondary, fontSize = 12.sp)
        }
    }
}

// ============================================================
// SelectableCard — gros bouton cliquable type "card" (radio/option)
// ============================================================
@Composable
fun SelectableCard(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = RevOrange,
) {
    val borderColor by animateColorAsState(
        if (selected) RevOrange else Color(0x14000000),
        label = "cardBorder",
    )
    val bg by animateColorAsState(
        if (selected) RevOrange.copy(alpha = 0.06f) else RevCardBackground,
        label = "cardBg",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                subtitle?.let {
                    Text(it, color = RevTextSecondary, fontSize = 12.sp)
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(RevOrange),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0x22000000), CircleShape),
                )
            }
        }
    }
}

// ============================================================
// ReadonlyRow — pour afficher infos identité non éditables (Étape 1)
// ============================================================
@Composable
fun ReadonlyRow(label: String, value: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF6F1EA))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, tint = RevOrange, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = RevTextSecondary, fontSize = 11.sp)
            Text(
                value.ifBlank { "—" },
                color = RevBrown,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
