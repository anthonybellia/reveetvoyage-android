package be.reveetvoyage.app.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import be.reveetvoyage.app.data.model.WeatherDay
import be.reveetvoyage.app.data.model.WeatherResponse
import be.reveetvoyage.app.ui.theme.RevBrown
import be.reveetvoyage.app.ui.theme.RevCardBackground
import be.reveetvoyage.app.ui.theme.RevOrange
import be.reveetvoyage.app.ui.theme.RevRed
import be.reveetvoyage.app.ui.theme.RevTextSecondary
import be.reveetvoyage.app.ui.theme.RevYellow
import kotlin.math.roundToInt

// ============================================================
// WeatherCard — current temp + 5 day forecast with animated icon
// ============================================================
@Composable
fun WeatherCard(
    weather: WeatherResponse?,
    locationLabel: String?,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(weatherGradient(weather?.current?.code, weather?.current?.is_day ?: true))
                .padding(16.dp),
        ) {
            when {
                isLoading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp))
                    Text("Météo en cours…", color = Color.White, fontSize = 14.sp)
                }

                weather?.current == null -> Text(
                    "Météo indisponible",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                )

                else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (!locationLabel.isNullOrBlank()) {
                                Text(
                                    locationLabel,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Text(
                                "${weather.current.temp?.roundToInt() ?: "-"}°",
                                color = Color.White,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                weatherLabel(weather.current.code),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            weather.current.feels_like?.let { fl ->
                                Text(
                                    "Ressenti ${fl.roundToInt()}°",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp,
                                )
                            }
                        }
                        AnimatedWeatherIcon(
                            code = weather.current.code,
                            isDay = weather.current.is_day,
                            size = 72,
                        )
                    }

                    if (weather.forecast.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            weather.forecast.take(5).forEach { day ->
                                ForecastDay(day = day)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastDay(day: WeatherDay) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        Text(
            shortDow(day.date),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        AnimatedWeatherIcon(code = day.code, isDay = true, size = 24)
        Text(
            "${day.temp_max?.roundToInt() ?: "-"}°",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "${day.temp_min?.roundToInt() ?: "-"}°",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 11.sp,
        )
    }
}

// ============================================================
// Animated weather icon — pure Compose animations
// ============================================================
@Composable
fun AnimatedWeatherIcon(code: Int?, isDay: Boolean, size: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather")

    val type = weatherType(code, isDay)
    Box(modifier = Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        when (type) {
            WeatherType.SUN -> {
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
                    label = "sunRotate",
                )
                val pulse by infiniteTransition.animateFloat(
                    initialValue = 0.95f, targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "sunPulse",
                )
                Icon(
                    Icons.Default.WbSunny, null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(size.dp).rotate(rotation).scale(pulse),
                )
            }
            WeatherType.MOON -> {
                val pulse by infiniteTransition.animateFloat(
                    initialValue = 0.96f, targetValue = 1.04f,
                    animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "moonPulse",
                )
                Icon(Icons.Default.NightsStay, null, tint = Color(0xFFF5F0E8),
                    modifier = Modifier.size(size.dp).scale(pulse))
            }
            WeatherType.CLOUD -> {
                val drift by infiniteTransition.animateFloat(
                    initialValue = -3f, targetValue = 3f,
                    animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "cloudDrift",
                )
                Icon(Icons.Default.Cloud, null, tint = Color(0xFFE0E7EE),
                    modifier = Modifier.size(size.dp).offset { IntOffset(drift.dp.value.toInt(), 0) })
            }
            WeatherType.RAIN -> {
                Box(modifier = Modifier.size(size.dp)) {
                    Icon(Icons.Default.Cloud, null, tint = Color(0xFFB0BEC5),
                        modifier = Modifier.size(size.dp).align(Alignment.TopCenter))
                    val drop1 by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
                        label = "drop1",
                    )
                    val drop2 by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1100, delayMillis = 200, easing = LinearEasing)),
                        label = "drop2",
                    )
                    val drop3 by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(950, delayMillis = 450, easing = LinearEasing)),
                        label = "drop3",
                    )
                    listOf(drop1 to 0.30f, drop2 to 0.50f, drop3 to 0.70f).forEach { (t, x) ->
                        Icon(
                            Icons.Default.WaterDrop, null,
                            tint = Color(0xFF64B5F6).copy(alpha = 1f - t),
                            modifier = Modifier
                                .size((size * 0.30).dp)
                                .align(Alignment.TopStart)
                                .offset {
                                    IntOffset(
                                        (size * x).dp.value.toInt(),
                                        ((size * 0.55) + (size * 0.40 * t)).dp.value.toInt()
                                    )
                                },
                        )
                    }
                }
            }
            WeatherType.SNOW -> {
                val rotate by infiniteTransition.animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
                    label = "snowRotate",
                )
                Icon(Icons.Default.AcUnit, null, tint = Color(0xFFE0F7FA),
                    modifier = Modifier.size(size.dp).rotate(rotate))
            }
            WeatherType.STORM -> {
                val flash by infiniteTransition.animateFloat(
                    initialValue = 0.5f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "stormFlash",
                )
                Icon(Icons.Default.Thunderstorm, null, tint = Color(0xFFFFEB3B).copy(alpha = flash),
                    modifier = Modifier.size(size.dp))
            }
            WeatherType.FOG -> {
                val drift by infiniteTransition.animateFloat(
                    initialValue = -4f, targetValue = 4f,
                    animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "fogDrift",
                )
                Icon(Icons.Default.Air, null, tint = Color(0xFFCFD8DC),
                    modifier = Modifier.size(size.dp).offset { IntOffset(drift.dp.value.toInt(), 0) })
            }
        }
    }
}

private enum class WeatherType { SUN, MOON, CLOUD, RAIN, SNOW, STORM, FOG }

private fun weatherType(code: Int?, isDay: Boolean): WeatherType = when (code) {
    null, 0 -> if (isDay) WeatherType.SUN else WeatherType.MOON
    1, 2 -> if (isDay) WeatherType.SUN else WeatherType.MOON
    3 -> WeatherType.CLOUD
    45, 48 -> WeatherType.FOG
    in 51..67, in 80..82 -> WeatherType.RAIN
    in 71..77, in 85..86 -> WeatherType.SNOW
    in 95..99 -> WeatherType.STORM
    else -> WeatherType.CLOUD
}

fun weatherLabel(code: Int?): String = when (code) {
    null -> "—"
    0 -> "Ciel clair"
    1 -> "Plutôt clair"
    2 -> "Partiellement nuageux"
    3 -> "Couvert"
    45, 48 -> "Brouillard"
    51, 53, 55 -> "Bruine"
    61, 63, 65 -> "Pluie"
    71, 73, 75 -> "Neige"
    77 -> "Neige fine"
    80, 81, 82 -> "Averses"
    85, 86 -> "Averses de neige"
    95 -> "Orage"
    96, 99 -> "Orage avec grêle"
    else -> "Variable"
}

private fun weatherGradient(code: Int?, isDay: Boolean): Brush = when (weatherType(code, isDay)) {
    WeatherType.SUN  -> Brush.linearGradient(listOf(Color(0xFFFFB347), Color(0xFFFF7043)))
    WeatherType.MOON -> Brush.linearGradient(listOf(Color(0xFF3F4E78), Color(0xFF1A2238)))
    WeatherType.CLOUD-> Brush.linearGradient(listOf(Color(0xFF78909C), Color(0xFF455A64)))
    WeatherType.RAIN -> Brush.linearGradient(listOf(Color(0xFF546E7A), Color(0xFF263238)))
    WeatherType.SNOW -> Brush.linearGradient(listOf(Color(0xFF90CAF9), Color(0xFF5C8FCB)))
    WeatherType.STORM-> Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF1B262C)))
    WeatherType.FOG  -> Brush.linearGradient(listOf(Color(0xFFB0BEC5), Color(0xFF78909C)))
}

private fun shortDow(isoDate: String): String {
    return try {
        val dow = java.time.LocalDate.parse(isoDate).dayOfWeek
        when (dow) {
            java.time.DayOfWeek.MONDAY    -> "Lun"
            java.time.DayOfWeek.TUESDAY   -> "Mar"
            java.time.DayOfWeek.WEDNESDAY -> "Mer"
            java.time.DayOfWeek.THURSDAY  -> "Jeu"
            java.time.DayOfWeek.FRIDAY    -> "Ven"
            java.time.DayOfWeek.SATURDAY  -> "Sam"
            java.time.DayOfWeek.SUNDAY    -> "Dim"
        }
    } catch (e: Exception) {
        isoDate.take(3)
    }
}

// ============================================================
// Geocoding helper for reverse lookup of a city name (used by HomeScreen)
// ============================================================
@SuppressLint("MissingPermission")
suspend fun reverseGeocodeCity(context: Context, lat: Double, lng: Double): String? {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val gc = Geocoder(context, java.util.Locale.getDefault())
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                    gc.getFromLocation(lat, lng, 1) { addrs ->
                        val a = addrs.firstOrNull()
                        cont.resume(a?.locality ?: a?.subAdminArea ?: a?.adminArea) {}
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val list = gc.getFromLocation(lat, lng, 1)
                list?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
            }
        } catch (t: Throwable) { null }
    }
}
