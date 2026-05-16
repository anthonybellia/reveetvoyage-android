package be.reveetvoyage.app.data.repo

import android.content.Context
import be.reveetvoyage.app.data.api.ApiService
import be.reveetvoyage.app.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoyageRepository @Inject constructor(private val api: ApiService) {
    suspend fun list(page: Int = 1) = api.voyages(page).data
    suspend fun detail(id: Int) = api.voyageDetail(id).data
    suspend fun toggleEtape(voyageId: Int, etapeId: Int) = api.toggleEtape(voyageId, etapeId).data
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

    suspend fun refresh(): User? {
        return runCatching { api.me().user }.getOrNull()?.also { _currentUser.value = it }
    }

    fun setCachedUser(user: User?) { _currentUser.value = user }

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
