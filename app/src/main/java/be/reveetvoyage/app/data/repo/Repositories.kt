package be.reveetvoyage.app.data.repo

import android.content.Context
import be.reveetvoyage.app.data.api.ApiService
import be.reveetvoyage.app.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

// Résultat d'une invitation, pour différencier les messages côté UI.
sealed class InviteResult {
    object Success : InviteResult()           // 2xx : membre lié ou invitation envoyée
    object Forbidden : InviteResult()         // 403 : pas propriétaire/admin
    data class Conflict(val message: String) : InviteResult() // 422 : déjà propriétaire, etc.
    data class Error(val message: String) : InviteResult()    // autre échec réseau/HTTP
}

@Singleton
class VoyageRepository @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context,
) {
    suspend fun list(page: Int = 1) = api.voyages(page).data
    suspend fun detail(id: Int) = api.voyageDetail(id).data
    suspend fun toggleEtape(voyageId: Int, etapeId: Int) = api.toggleEtape(voyageId, etapeId).data

    // ===== Étape : couverture & billets (admin) =====

    /** Upload / remplace l'image de couverture de l'étape. Renvoie l'étape à jour. */
    suspend fun uploadEtapeCover(
        voyageId: Int, etapeId: Int,
        bytes: ByteArray, fileName: String, mime: String,
    ): VoyageEtape? {
        val part = filePart("cover", bytes, fileName, mime)
        return runCatchingTemp(part) { api.uploadEtapeCover(voyageId, etapeId, it).data }
    }

    /** Ajoute un billet (PDF ou image) à l'étape. Renvoie l'étape à jour. */
    suspend fun uploadEtapeTicket(
        voyageId: Int, etapeId: Int,
        bytes: ByteArray, fileName: String, mime: String,
    ): VoyageEtape? {
        val part = filePart("ticket", bytes, fileName, mime)
        return runCatchingTemp(part) { api.uploadEtapeTicket(voyageId, etapeId, it).data }
    }

    /** Supprime un billet de l'étape (identifié par son url). Renvoie l'étape à jour. */
    suspend fun deleteEtapeTicket(voyageId: Int, etapeId: Int, ticketUrl: String): VoyageEtape? =
        api.deleteEtapeTicket(voyageId, etapeId, DeleteTicketRequest(ticketUrl)).data

    // Construit une MultipartBody.Part depuis des octets, via un fichier temporaire
    // en cacheDir (même approche que MessageRepository.sendWithAttachment).
    private fun filePart(name: String, bytes: ByteArray, fileName: String, mime: String): Pair<MultipartBody.Part, File> {
        val tmpFile = File.createTempFile("etape_", "_${System.currentTimeMillis()}", context.cacheDir).apply {
            FileOutputStream(this).use { it.write(bytes) }
        }
        val fileBody = tmpFile.asRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(name, fileName, fileBody) to tmpFile
    }

    // Exécute l'appel puis nettoie le fichier temporaire dans tous les cas.
    private suspend fun <T> runCatchingTemp(
        part: Pair<MultipartBody.Part, File>,
        block: suspend (MultipartBody.Part) -> T,
    ): T {
        try {
            return block(part.first)
        } finally {
            part.second.delete()
        }
    }

    // ===== Membres / collaboration =====

    suspend fun members(voyageId: Int) = api.getMembers(voyageId)

    suspend fun inviteMember(voyageId: Int, email: String, role: String? = null): InviteResult {
        return runCatching {
            val req = if (role.isNullOrBlank()) InviteRequest(email = email)
                      else InviteRequest(email = email, role = role)
            api.inviteMember(voyageId, req)
        }.fold(
            onSuccess = { resp ->
                when {
                    resp.isSuccessful -> InviteResult.Success
                    resp.code() == 403 -> InviteResult.Forbidden
                    resp.code() == 422 -> InviteResult.Conflict(
                        extractMessage(resp.errorBody()?.string()) ?: "Cette personne est déjà propriétaire."
                    )
                    else -> InviteResult.Error("Erreur ${resp.code()}")
                }
            },
            onFailure = { InviteResult.Error(it.message ?: "Erreur réseau") },
        )
    }

    suspend fun removeMember(voyageId: Int, userId: Int) = api.removeMember(voyageId, userId)

    // Extrait le champ "message" d'un corps d'erreur JSON sans dépendre du modèle.
    private fun extractMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.getOrNull(1)
    }
}

@Singleton
class ExpenseRepository @Inject constructor(private val api: ApiService) {
    suspend fun participants(voyageId: Int) = api.voyageParticipants(voyageId).data
    suspend fun addGuest(voyageId: Int, displayName: String) =
        api.addVoyageParticipant(voyageId, CreateParticipantRequest(display_name = displayName)).data
    suspend fun inviteByEmail(voyageId: Int, email: String) =
        api.addVoyageParticipant(voyageId, CreateParticipantRequest(email = email)).data
    suspend fun removeParticipant(voyageId: Int, participantId: Int) =
        api.deleteVoyageParticipant(voyageId, participantId)

    suspend fun expenses(voyageId: Int, since: String? = null) =
        api.voyageExpenses(voyageId, since).data
    suspend fun createExpense(voyageId: Int, req: CreateExpenseRequest) =
        api.createVoyageExpense(voyageId, req).data
    suspend fun updateExpense(voyageId: Int, expenseId: Int, req: CreateExpenseRequest) =
        api.updateVoyageExpense(voyageId, expenseId, req).data
    suspend fun deleteExpense(voyageId: Int, expenseId: Int) =
        api.deleteVoyageExpense(voyageId, expenseId)

    suspend fun settlement(voyageId: Int) = api.voyageSettlement(voyageId)

    suspend fun searchPlaces(q: String, lat: Double? = null, lng: Double? = null) =
        api.searchPlaces(q, lat, lng).data
}

@Singleton
class PackingRepository @Inject constructor(private val api: ApiService) {
    suspend fun categories() = api.packingCategories().data
    suspend fun template() = api.packingTemplate().data
    suspend fun addTemplate(req: PackingTemplateItemRequest) = api.createPackingTemplateItem(req).data
    suspend fun updateTemplate(id: Int, req: PackingTemplateItemRequest) = api.updatePackingTemplateItem(id, req).data
    suspend fun deleteTemplate(id: Int) = api.deletePackingTemplateItem(id)

    suspend fun voyagePacking(voyageId: Int, since: String? = null) = api.voyagePacking(voyageId, since).data
    suspend fun generateVoyagePacking(voyageId: Int) = api.generateVoyagePacking(voyageId).data
    suspend fun addVoyageItem(voyageId: Int, req: VoyagePackingCreateRequest) = api.createVoyagePackingItem(voyageId, req).data
    suspend fun updateVoyageItem(voyageId: Int, itemId: Int, req: VoyagePackingUpdateRequest) =
        api.updateVoyagePackingItem(voyageId, itemId, req).data
    suspend fun deleteVoyageItem(voyageId: Int, itemId: Int) = api.deleteVoyagePackingItem(voyageId, itemId)
}

@Singleton
class DevisRepository @Inject constructor(private val api: ApiService) {
    suspend fun list(page: Int = 1) = api.devis(page).data
}

@Singleton
class PassengerRepository @Inject constructor(private val api: ApiService) {
    suspend fun list() = api.passengers().data
    suspend fun create(req: PassengerRequest) = api.createPassenger(req).data
    suspend fun update(id: Int, req: PassengerRequest) = api.updatePassenger(id, req).data
    suspend fun delete(id: Int) = api.deletePassenger(id)
}

@Singleton
class MessageRepository @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context,
) {
    suspend fun list(since: String? = null) = api.messages(since).data
    suspend fun send(body: String) = api.sendMessage(SendMessageRequest(body)).data
    suspend fun unreadCount() = api.unreadCount().count
    suspend fun files() = api.files().data

    suspend fun sendWithAttachment(body: String, bytes: ByteArray, fileName: String, mime: String): Message {
        val tmpFile = File.createTempFile("attach_", "_${System.currentTimeMillis()}", context.cacheDir).apply {
            FileOutputStream(this).use { it.write(bytes) }
        }
        val mimeType = mime.toMediaTypeOrNull()
        val fileBody = tmpFile.asRequestBody(mimeType)
        val part = MultipartBody.Part.createFormData("attachment", fileName, fileBody)
        val bodyRb = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), body)
        val response = api.sendMessageWithAttachment(bodyRb, part).data
        tmpFile.delete()
        return response
    }
}

@Singleton
class UserRepository @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context,
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // ===== Aperçu "vue utilisateur" =====
    // Drapeau session-scoped : un vrai admin peut basculer en mode aperçu
    // pour voir l'app comme un client (sans les contrôles admin). Ne change
    // QUE l'affichage côté UI, jamais les permissions de données côté API.
    private val _previewAsUser = MutableStateFlow(false)
    val previewAsUser: StateFlow<Boolean> = _previewAsUser.asStateFlow()

    // Scope dédié au repository (singleton) pour matérialiser les StateFlow dérivés.
    private val repoScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
    )

    // Vrai si le compte connecté a réellement le rôle admin (ignore l'aperçu).
    val isRealAdmin: StateFlow<Boolean> =
        _currentUser
            .map { it?.role == "admin" }
            .stateIn(repoScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)

    // Admin "effectif" pour le gating UI : vrai admin ET pas en mode aperçu.
    val isAdmin: StateFlow<Boolean> =
        kotlinx.coroutines.flow.combine(_currentUser, _previewAsUser) { user, preview ->
            user?.role == "admin" && !preview
        }.stateIn(repoScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)

    fun setPreviewAsUser(enabled: Boolean) { _previewAsUser.value = enabled }
    fun togglePreviewAsUser() { _previewAsUser.value = !_previewAsUser.value }

    suspend fun refresh(): User? {
        return runCatching { api.me().user }.getOrNull()?.also { _currentUser.value = it }
    }

    fun setCachedUser(user: User?) {
        _currentUser.value = user
        // À la déconnexion, on réinitialise l'aperçu pour ne pas le conserver
        // d'une session à l'autre.
        if (user == null) _previewAsUser.value = false
    }

    suspend fun updateProfile(req: UpdateProfileRequest): Boolean = runCatching {
        _currentUser.value = api.updateMe(req).user; true
    }.getOrDefault(false)

    suspend fun updatePassword(req: UpdatePasswordRequest): Result<Unit> = runCatching {
        api.updatePassword(req); Unit
    }

    suspend fun updatePreferences(req: UpdatePreferencesRequest): Boolean = runCatching {
        _currentUser.value = api.updatePreferences(req).user; true
    }.getOrDefault(false)

    suspend fun uploadAvatar(imageBytes: ByteArray, mime: String = "image/jpeg"): Boolean = runCatching {
        val tmpFile = File.createTempFile("avatar", ".jpg", context.cacheDir).apply {
            FileOutputStream(this).use { it.write(imageBytes) }
        }
        val body = tmpFile.asRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("avatar", tmpFile.name, body)
        _currentUser.value = api.uploadAvatar(part).user
        tmpFile.delete()
        true
    }.getOrDefault(false)
}
