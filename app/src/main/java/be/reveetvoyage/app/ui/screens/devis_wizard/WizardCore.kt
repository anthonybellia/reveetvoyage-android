package be.reveetvoyage.app.ui.screens.devis_wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.api.ApiConfig
import be.reveetvoyage.app.data.model.User
import be.reveetvoyage.app.data.model.WrappedResponse
import be.reveetvoyage.app.data.repo.UserRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import javax.inject.Inject

// =============================================================
// Models — self-contained quiz module (Write is restricted to
// ui/screens/, so models live here rather than in data/model)
// =============================================================

/**
 * Accumulateur d'état du quiz multi-étapes "Demander un voyage".
 */
data class DevisDraft(
    val phone: String = "",
    val nbPersonnes: String = "",
    val participants: String = "",
    val datesSouhaitees: String = "",
    val flexibleDates: Boolean = false,
    val duree: String = "",
    val lieuDepart: String = "",
    val destination: String = "",
    val ouvertSuggestions: Boolean = false,
    val cadre: Set<String> = emptySet(),
    val hebergement: String = "",
    val besoinsSpecifiques: String = "",
    val activites: Set<String> = emptySet(),
    val activitesEviter: String = "",
    val budget: String = "",
    val imperatifs: String = "",
    val evenement: String = "",
    val message: String = "",
)

/**
 * Payload envoyé à POST /api/devis. Tous nullable côté backend ; le serveur
 * autofille nom/email/téléphone à partir de l'utilisateur authentifié.
 *
 * NB : on omet volontairement `type_voyage` (enum strict couple/famille/...)
 * car le quiz natif ne le demande pas explicitement et l'envoyer vide
 * déclencherait une erreur de validation.
 */
@Serializable
data class CreateDevisRequest(
    val destination: String? = null,
    val destination_souhaitee: String? = null,
    val dates_souhaitees: String? = null,
    val flexible_dates: String? = null,
    val duree: String? = null,
    val nb_personnes: String? = null,
    val participants: String? = null,
    val lieu_depart: String? = null,
    val preferences_horaires: String? = null,
    val ouvert_suggestions: String? = null,
    val cadre: String? = null,
    val hebergement: String? = null,
    val besoins_specifiques: String? = null,
    val activites: String? = null,
    val activites_eviter: String? = null,
    val imperatifs: String? = null,
    val evenement: String? = null,
    val budget: String? = null,
    val message: String? = null,
)

fun DevisDraft.toRequest(): CreateDevisRequest = CreateDevisRequest(
    destination = destination.trim().ifBlank { null },
    destination_souhaitee = destination.trim().ifBlank { null },
    dates_souhaitees = datesSouhaitees.trim().ifBlank { null },
    flexible_dates = if (flexibleDates) "1" else null,
    duree = duree.trim().ifBlank { null },
    nb_personnes = nbPersonnes.trim().ifBlank { null },
    participants = participants.trim().ifBlank { null },
    lieu_depart = lieuDepart.trim().ifBlank { null },
    preferences_horaires = null,
    ouvert_suggestions = if (ouvertSuggestions) "1" else null,
    cadre = cadre.joinToString(", ").ifBlank { null },
    hebergement = hebergement.trim().ifBlank { null },
    besoins_specifiques = besoinsSpecifiques.trim().ifBlank { null },
    activites = activites.joinToString(", ").ifBlank { null },
    activites_eviter = activitesEviter.trim().ifBlank { null },
    imperatifs = imperatifs.trim().ifBlank { null },
    evenement = evenement.trim().ifBlank { null },
    budget = budget.trim().ifBlank { null },
    message = message.trim().ifBlank { null },
)

/** Aéroport renvoyé par GET /api/airports (champs compacts). */
@Serializable
data class Airport(
    val c: String,
    val n: String,
    val v: String,
    val p: String,
    val a: Double? = null,
    val o: Double? = null,
) {
    val label: String get() = "$c — $v"
    val sublabel: String get() = "$n · $p"
}

// =============================================================
// Retrofit API local au module quiz (réutilise OkHttp + Json Hilt)
// =============================================================

interface DevisWizardApi {
    @POST("devis")
    suspend fun createDevis(@Body req: CreateDevisRequest): WrappedResponse<be.reveetvoyage.app.data.model.Devis>

    @GET("airports")
    suspend fun searchAirports(@Query("q") q: String): List<Airport>
}

// =============================================================
// Hilt module — fournit le DevisWizardApi en réutilisant l'OkHttp
// global (avec AuthInterceptor déjà câblé pour POST /devis)
// =============================================================
@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
object DevisWizardModule {
    @dagger.Provides
    @javax.inject.Singleton
    fun provideDevisWizardApi(okHttpClient: OkHttpClient, json: Json): DevisWizardApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(DevisWizardApi::class.java)
    }
}

// =============================================================
// État UI
// =============================================================

enum class WizardStep(val index: Int, val title: String) {
    Identite(0, "Vos infos"),
    Voyageurs(1, "Voyageurs"),
    Dates(2, "Dates"),
    Destination(3, "Destination"),
    Sejour(4, "Séjour"),
    Activites(5, "Activités"),
    Details(6, "Détails");

    companion object {
        val TOTAL = values().size
        fun fromIndex(i: Int): WizardStep = values().getOrNull(i) ?: Identite
    }
}

sealed class SubmitState {
    object Idle : SubmitState()
    object Submitting : SubmitState()
    object Success : SubmitState()
    data class Error(val message: String) : SubmitState()
}

// =============================================================
// ViewModel
// =============================================================

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DevisWizardViewModel @Inject constructor(
    private val api: DevisWizardApi,
    private val userRepo: UserRepository,
) : ViewModel() {

    private val _draft = MutableStateFlow(DevisDraft())
    val draft: StateFlow<DevisDraft> = _draft.asStateFlow()

    private val _stepIndex = MutableStateFlow(0)
    val stepIndex: StateFlow<Int> = _stepIndex.asStateFlow()

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    val currentUser: StateFlow<User?> = userRepo.currentUser

    // Airport autocomplete — debounced 300ms
    private val _airportQuery = MutableStateFlow("")
    private val _airportResults = MutableStateFlow<List<Airport>>(emptyList())
    val airportResults: StateFlow<List<Airport>> = _airportResults.asStateFlow()
    private val _airportLoading = MutableStateFlow(false)
    val airportLoading: StateFlow<Boolean> = _airportLoading.asStateFlow()

    init {
        // Refresh user once at wizard open
        viewModelScope.launch { userRepo.refresh() }

        // Preload phone from current user — seed une fois quand user devient non-null
        viewModelScope.launch {
            var seeded = false
            userRepo.currentUser.collect { u ->
                if (!seeded && u != null) {
                    seeded = true
                    if (_draft.value.phone.isBlank()) {
                        _draft.value = _draft.value.copy(phone = u.phone.orEmpty())
                    }
                }
            }
        }

        // Airport search pipeline
        _airportQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { q ->
                if (q.length < 2) {
                    _airportResults.value = emptyList()
                    _airportLoading.value = false
                }
            }
            .filter { it.length >= 2 }
            .flatMapLatest { q ->
                flow {
                    _airportLoading.value = true
                    val res = runCatching { api.searchAirports(q) }.getOrDefault(emptyList())
                    emit(res)
                }
            }
            .onEach {
                _airportResults.value = it
                _airportLoading.value = false
            }
            .launchIn(viewModelScope)
    }

    // ---------- Step navigation ----------
    fun next() {
        if (_stepIndex.value < WizardStep.TOTAL - 1) _stepIndex.value += 1
    }

    fun previous() {
        if (_stepIndex.value > 0) _stepIndex.value -= 1
    }

    fun goToStep(index: Int) {
        if (index in 0 until WizardStep.TOTAL) _stepIndex.value = index
    }

    fun reset() {
        _draft.value = DevisDraft(phone = currentUser.value?.phone.orEmpty())
        _stepIndex.value = 0
        _submitState.value = SubmitState.Idle
    }

    // ---------- Draft updates ----------
    fun update(transform: (DevisDraft) -> DevisDraft) {
        _draft.value = transform(_draft.value)
    }

    // ---------- Airport autocomplete ----------
    fun queryAirports(q: String) {
        _airportQuery.value = q
    }

    fun clearAirportResults() {
        _airportQuery.value = ""
        _airportResults.value = emptyList()
    }

    // ---------- Submit ----------
    fun submit() {
        if (_submitState.value is SubmitState.Submitting) return
        _submitState.value = SubmitState.Submitting
        viewModelScope.launch {
            val result = runCatching { api.createDevis(_draft.value.toRequest()) }
            _submitState.value = result.fold(
                onSuccess = { SubmitState.Success },
                onFailure = { e ->
                    val msg = (e.message ?: "Erreur réseau").take(160)
                    SubmitState.Error(msg)
                },
            )
        }
    }

    fun consumeError() {
        if (_submitState.value is SubmitState.Error) _submitState.value = SubmitState.Idle
    }

    // ---------- Validation locale minimale ----------
    fun canAdvance(step: WizardStep, user: User?): Boolean = when (step) {
        WizardStep.Identite -> {
            if (user == null) false
            else {
                val phoneOk = !user.phone.isNullOrBlank() || _draft.value.phone.isNotBlank()
                val nameOk = user.prenom.isNotBlank() && user.nom.isNotBlank()
                val emailOk = user.email.isNotBlank()
                phoneOk && nameOk && emailOk
            }
        }
        WizardStep.Details -> _draft.value.budget.isNotBlank()
        else -> true
    }
}
