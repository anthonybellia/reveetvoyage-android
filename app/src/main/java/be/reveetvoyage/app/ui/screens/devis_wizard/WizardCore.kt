package be.reveetvoyage.app.ui.screens.devis_wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.api.ApiConfig
import be.reveetvoyage.app.data.model.Passenger
import be.reveetvoyage.app.data.model.PassengerRequest
import be.reveetvoyage.app.data.model.User
import be.reveetvoyage.app.data.model.WrappedResponse
import be.reveetvoyage.app.data.repo.PassengerRepository
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
 *
 * - `selectedPassengers` : voyageurs liés (issus de /api/passengers), source de vérité pour
 *   le nombre de personnes envoyé au backend (`nb_personnes` est dérivé de cette liste).
 * - `participants` : notes complémentaires (allergies, accompagnants non-enregistrés…).
 * - `lieuxDepart` / `lieuxRetour` : multi-aéroports (max 10 chacun côté backend).
 */
data class DevisDraft(
    val phone: String = "",
    val selectedPassengers: List<Passenger> = emptyList(),
    val participants: String = "",
    val datesSouhaitees: String = "",
    val flexibleDates: Boolean = false,
    val duree: String = "",
    val lieuxDepart: List<Airport> = emptyList(),
    val lieuxRetour: List<Airport> = emptyList(),
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
 * `type_voyage` est une ENUM stricte côté DB (couple|famille|amis|solo|lune_de_miel) ;
 * on envoie "couple" comme défaut sûr puisque le quiz natif ne demande pas
 * explicitement la composition du groupe (les chips Voyageurs capturent juste le nombre).
 */
@Serializable
data class CreateDevisRequest(
    val destination: String? = null,
    val destination_souhaitee: String? = null,
    val dates_souhaitees: String? = null,
    val flexible_dates: String? = null,
    val duree: String? = null,
    val nb_personnes: Int? = null,
    val participants: String? = null,
    val lieu_depart: String? = null,
    val lieux_depart: List<String>? = null,
    val lieux_retour: List<String>? = null,
    val passenger_ids: List<Int>? = null,
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
    val type_voyage: String = "couple",
    val message: String? = null,
)

private fun Airport.toRequestString(): String = "$c — $n ($v)"

fun DevisDraft.toRequest(): CreateDevisRequest {
    val depart = lieuxDepart.map { it.toRequestString() }
    val retour = lieuxRetour.map { it.toRequestString() }
    return CreateDevisRequest(
        destination = destination.trim().ifBlank { null },
        destination_souhaitee = destination.trim().ifBlank { null },
        dates_souhaitees = datesSouhaitees.trim().ifBlank { null },
        flexible_dates = if (flexibleDates) "1" else null,
        duree = duree.trim().ifBlank { null },
        nb_personnes = selectedPassengers.size.takeIf { it > 0 },
        participants = participants.trim().ifBlank { null },
        lieu_depart = depart.firstOrNull(),
        lieux_depart = depart.takeIf { it.isNotEmpty() },
        lieux_retour = retour.takeIf { it.isNotEmpty() },
        passenger_ids = selectedPassengers.map { it.id }.takeIf { it.isNotEmpty() },
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
        type_voyage = "couple",
        message = message.trim().ifBlank { null },
    )
}

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
    private val passengerRepo: PassengerRepository,
) : ViewModel() {

    private val _draft = MutableStateFlow(DevisDraft())
    val draft: StateFlow<DevisDraft> = _draft.asStateFlow()

    private val _stepIndex = MutableStateFlow(0)
    val stepIndex: StateFlow<Int> = _stepIndex.asStateFlow()

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    val currentUser: StateFlow<User?> = userRepo.currentUser

    // Voyageurs (Mes passagers) — chargé à la demande quand on ouvre la BottomSheet picker
    private val _myPassengers = MutableStateFlow<List<Passenger>>(emptyList())
    val myPassengers: StateFlow<List<Passenger>> = _myPassengers.asStateFlow()
    private val _passengersLoading = MutableStateFlow(false)
    val passengersLoading: StateFlow<Boolean> = _passengersLoading.asStateFlow()
    private val _passengerCreateError = MutableStateFlow<String?>(null)
    val passengerCreateError: StateFlow<String?> = _passengerCreateError.asStateFlow()

    // Airport autocomplete — debounced 300ms (utilisé uniquement par le legacy single-field si encore branché)
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
        _myPassengers.value = emptyList()
        _passengerCreateError.value = null
    }

    // ---------- Draft updates ----------
    fun update(transform: (DevisDraft) -> DevisDraft) {
        _draft.value = transform(_draft.value)
    }

    // ---------- Passengers ----------
    fun addPassenger(p: Passenger) {
        _draft.value = _draft.value.copy(
            selectedPassengers = if (_draft.value.selectedPassengers.any { it.id == p.id }) {
                _draft.value.selectedPassengers
            } else {
                _draft.value.selectedPassengers + p
            }
        )
    }

    fun removePassenger(p: Passenger) {
        _draft.value = _draft.value.copy(
            selectedPassengers = _draft.value.selectedPassengers.filterNot { it.id == p.id }
        )
    }

    fun togglePassenger(p: Passenger) {
        if (_draft.value.selectedPassengers.any { it.id == p.id }) removePassenger(p) else addPassenger(p)
    }

    /** Charge la liste des passagers du user (appelée à l'ouverture de la picker). */
    fun loadMyPassengers() {
        if (_passengersLoading.value) return
        viewModelScope.launch {
            _passengersLoading.value = true
            _myPassengers.value = runCatching { passengerRepo.list() }.getOrDefault(emptyList())
            _passengersLoading.value = false
        }
    }

    /**
     * Crée un nouveau passager via POST /api/passengers et l'ajoute automatiquement
     * à la sélection du draft. Retourne true si succès.
     */
    suspend fun createPassenger(req: PassengerRequest): Boolean {
        _passengerCreateError.value = null
        return runCatching { passengerRepo.create(req) }
            .onSuccess { created ->
                created?.let { addPassenger(it) }
                // rafraîchir la liste pour que la BottomSheet reflète le nouvel ajout
                _myPassengers.value = runCatching { passengerRepo.list() }.getOrDefault(_myPassengers.value)
            }
            .onFailure { e ->
                _passengerCreateError.value = (e.message ?: "Création impossible").take(160)
            }
            .isSuccess
    }

    fun consumePassengerError() { _passengerCreateError.value = null }

    // ---------- Airports ----------
    /** Appel direct API (pas de debounce VM) — chaque AirportMultiSelector gère son propre debounce local. */
    suspend fun searchAirportsImmediate(q: String): List<Airport> =
        runCatching { api.searchAirports(q) }.getOrDefault(emptyList())

    // ---------- Airport autocomplete (legacy single-field) ----------
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
