package be.reveetvoyage.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import be.reveetvoyage.app.R
import be.reveetvoyage.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen() {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch {
                scale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
            }
            launch {
                alpha.animateTo(1f, tween(durationMillis = 600))
            }
            launch {
                rotation.animateTo(
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        tween(durationMillis = 12000, easing = LinearEasing),
                        RepeatMode.Restart
                    )
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        RevYellow.copy(alpha = 0.35f),
                        RevOrange.copy(alpha = 0.45f),
                        RevRed.copy(alpha = 0.30f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        OrbitingDots(rotation = rotation.value, alpha = alpha.value)

        Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.icon_brand),
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .scale(scale.value)
                .alpha(alpha.value),
        )
    }
}

@Composable
private fun OrbitingDots(rotation: Float, alpha: Float) {
    val palette = listOf(RevYellow, RevOrange, RevRed)
    Box(modifier = Modifier.size(360.dp), contentAlignment = Alignment.Center) {
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45.0 + rotation))
            val radius = 130f
            val x = (cos(angle) * radius).toFloat()
            val y = (sin(angle) * radius).toFloat()
            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .size(if (i % 2 == 0) 10.dp else 6.dp)
                    .alpha(alpha * 0.8f)
                    .clip(CircleShape)
                    .background(palette[i % palette.size])
            )
        }
    }
}

private fun Modifier.alpha(value: Float): Modifier = this.then(
    androidx.compose.ui.draw.alpha(value)
)
