package be.reveetvoyage.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val RevBrown  = Color(0xFF1F1008)
val RevOrange = Color(0xFFF09D6B)
val RevYellow = Color(0xFFF2C61D)
val RevRed    = Color(0xFFE45F60)
val RevBackground = Color(0xFFFFFBF7)
val RevCardBackground = Color(0xFFFFFFFF)
val RevTextSecondary = Color(0xFF8A8A8A)

private val LightColors = lightColorScheme(
    primary = RevOrange,
    onPrimary = Color.White,
    secondary = RevYellow,
    onSecondary = RevBrown,
    tertiary = RevRed,
    background = RevBackground,
    surface = RevCardBackground,
    onSurface = RevBrown,
)

private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun ReveEtVoyageTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = Shapes,
        content = content
    )
}
