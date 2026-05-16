package be.reveetvoyage.app.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.api.ApiService
import be.reveetvoyage.app.data.model.WeatherResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val api: ApiService,
) : ViewModel() {

    private val _weather = MutableStateFlow<WeatherResponse?>(null)
    val weather: StateFlow<WeatherResponse?> = _weather.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _locationLabel = MutableStateFlow<String?>(null)
    val locationLabel: StateFlow<String?> = _locationLabel.asStateFlow()

    private var lastKey: String? = null

    fun load(lat: Double, lng: Double, label: String? = null) {
        val key = "%.3f:%.3f".format(lat, lng)
        if (key == lastKey && _weather.value != null) return
        lastKey = key
        if (label != null) _locationLabel.value = label
        viewModelScope.launch {
            _loading.value = true
            runCatching { api.weather(lat, lng) }
                .onSuccess { _weather.value = it }
                .onFailure { /* silently ignored — show "Météo indisponible" */ }
            _loading.value = false
        }
    }

    fun setLocationLabel(label: String?) { _locationLabel.value = label }
}

// ============================================================
// LocationHelper — one-shot last known position without prompt
// ============================================================
object LocationHelper {
    @SuppressLint("MissingPermission")
    fun lastKnownLocation(context: Context): Pair<Double, Double>? {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p) ?: continue
                return Pair(loc.latitude, loc.longitude)
            } catch (t: Throwable) { /* ignored */ }
        }
        return null
    }
}
