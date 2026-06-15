package be.reveetvoyage.app.data.api

import be.reveetvoyage.app.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    // Auth (public)
    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body req: RegisterRequest): AuthResponse

    // Auth (authenticated)
    @GET("auth/me")
    suspend fun me(): MeResponse

    @PUT("auth/me")
    suspend fun updateMe(@Body req: UpdateProfileRequest): MeResponse

    @POST("auth/me/password")
    suspend fun updatePassword(@Body req: UpdatePasswordRequest): Map<String, String>

    @PUT("auth/me/preferences")
    suspend fun updatePreferences(@Body req: UpdatePreferencesRequest): MeResponse

    @Multipart
    @POST("auth/me/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): MeResponse

    @POST("auth/logout")
    suspend fun logout(): Map<String, Boolean>

    // Voyages
    @GET("voyages")
    suspend fun voyages(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): PaginatedResponse<Voyage>

    @GET("voyages/{id}")
    suspend fun voyageDetail(@Path("id") id: Int): WrappedResponse<Voyage>

    @POST("voyages/{voyageId}/etapes/{etapeId}/toggle")
    suspend fun toggleEtape(
        @Path("voyageId") voyageId: Int,
        @Path("etapeId") etapeId: Int,
    ): WrappedResponse<VoyageEtape>

    /**
     * Idempotent : fixe l'état terminé/en attente d'une étape.
     * Utilisé par l'outbox hors-ligne (rejouable sans risque, last-write-wins).
     */
    @PUT("voyages/{voyageId}/etapes/{etapeId}/completion")
    suspend fun setEtapeCompletion(
        @Path("voyageId") voyageId: Int,
        @Path("etapeId") etapeId: Int,
        @Body req: EtapeCompletionRequest,
    ): WrappedResponse<VoyageEtape>

    // ===== Étape : image de couverture & billets (admin) =====
    // Renvoient l'étape mise à jour (EtapeResource côté backend).

    @Multipart
    @POST("voyages/{voyageId}/etapes/{etapeId}/cover")
    suspend fun uploadEtapeCover(
        @Path("voyageId") voyageId: Int,
        @Path("etapeId") etapeId: Int,
        @Part cover: MultipartBody.Part,
    ): WrappedResponse<VoyageEtape>

    @Multipart
    @POST("voyages/{voyageId}/etapes/{etapeId}/tickets")
    suspend fun uploadEtapeTicket(
        @Path("voyageId") voyageId: Int,
        @Path("etapeId") etapeId: Int,
        @Part ticket: MultipartBody.Part,
    ): WrappedResponse<VoyageEtape>

    // Suppression d'un billet identifié par son `url` (corps JSON sur un DELETE).
    @HTTP(method = "DELETE", path = "voyages/{voyageId}/etapes/{etapeId}/tickets", hasBody = true)
    suspend fun deleteEtapeTicket(
        @Path("voyageId") voyageId: Int,
        @Path("etapeId") etapeId: Int,
        @Body req: DeleteTicketRequest,
    ): WrappedResponse<VoyageEtape>

    // Devis
    @GET("devis")
    suspend fun devis(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): PaginatedResponse<Devis>

    @GET("devis/{id}")
    suspend fun devisDetail(@Path("id") id: Int): WrappedResponse<Devis>

    // Admin : met à jour le devis (statut, notes_admin, champs éditables).
    @PUT("devis/{id}")
    suspend fun updateDevis(@Path("id") id: Int, @Body body: DevisUpdateRequest): WrappedResponse<Devis>

    // Admin : convertit le devis en voyage. Renvoie { data: { devis_id, voyage_id } }.
    @POST("devis/{id}/convert-to-voyage")
    suspend fun convertDevisToVoyage(@Path("id") id: Int): WrappedResponse<DevisConvertResult>

    // Admin : supprime le devis. Renvoie { ok: true }.
    @DELETE("devis/{id}")
    suspend fun deleteDevis(@Path("id") id: Int): Map<String, Boolean>

    // Admin : notes internes threadées. { data: [ {id,contenu,author,created_at} ] }.
    @GET("devis/{id}/notes")
    suspend fun devisNotes(@Path("id") id: Int): PaginatedResponse<DevisNote>

    @POST("devis/{id}/notes")
    suspend fun addDevisNote(@Path("id") id: Int, @Body body: DevisNoteRequest): WrappedResponse<DevisNote>

    @DELETE("devis/{id}/notes/{noteId}")
    suspend fun deleteDevisNote(
        @Path("id") id: Int,
        @Path("noteId") noteId: Int,
    ): Map<String, Boolean>

    // Passengers
    @GET("passengers")
    suspend fun passengers(): PaginatedResponse<Passenger>

    @POST("passengers")
    suspend fun createPassenger(@Body req: PassengerRequest): WrappedResponse<Passenger>

    @PUT("passengers/{id}")
    suspend fun updatePassenger(@Path("id") id: Int, @Body req: PassengerRequest): WrappedResponse<Passenger>

    @DELETE("passengers/{id}")
    suspend fun deletePassenger(@Path("id") id: Int): Map<String, String>

    @POST("passengers/{id}/convert")
    suspend fun convertPassenger(
        @Path("id") id: Int,
        @Body req: ConvertPassengerRequest,
    ): WrappedResponse<Passenger>

    // Messages
    @GET("messages")
    suspend fun messages(@Query("since") since: String? = null): PaginatedResponse<Message>

    @POST("messages")
    suspend fun sendMessage(@Body req: SendMessageRequest): WrappedResponse<Message>

    @Multipart
    @POST("messages")
    suspend fun sendMessageWithAttachment(
        @Part("body") body: okhttp3.RequestBody,
        @Part attachment: MultipartBody.Part,
    ): WrappedResponse<Message>

    @GET("messages/unread-count")
    suspend fun unreadCount(): UnreadCountResponse

    @GET("files")
    suspend fun files(): PaginatedResponse<Message>

    // Pages dynamiques (CGU, confidentialité, etc.) — public
    @GET("pages/{slug}")
    suspend fun page(@Path("slug") slug: String): PageResponse

    // Weather (Open-Meteo proxy)
    @GET("weather")
    suspend fun weather(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
    ): WeatherResponse

    // Places autocomplete
    @GET("places/search")
    suspend fun searchPlaces(
        @Query("q") q: String,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
    ): PaginatedResponse<Place>

    // ===== Voyage Members / Collaboration =====
    // L'auth (token Bearer) est ajoutée automatiquement par AuthInterceptor : pas de header manuel.

    @GET("voyages/{id}/members")
    suspend fun getMembers(@Path("id") id: Int): MembersResponse

    // On renvoie un Response<ResponseBody> brut pour pouvoir distinguer les codes HTTP
    // (403 = pas propriétaire/admin, 422 = déjà propriétaire) côté repository.
    @POST("voyages/{id}/members")
    suspend fun inviteMember(
        @Path("id") id: Int,
        @Body body: InviteRequest,
    ): retrofit2.Response<okhttp3.ResponseBody>

    @DELETE("voyages/{id}/members/{userId}")
    suspend fun removeMember(
        @Path("id") id: Int,
        @Path("userId") userId: Int,
    ): Map<String, String>

    // ===== Invitations (autocomplete + réception) =====

    // Recherche d'un utilisateur par email exact. `data` peut être null.
    @GET("users/search")
    suspend fun searchUser(@Query("email") email: String): NullableWrappedResponse<UserSearchResult>

    // Invitations en attente reçues par l'utilisateur courant.
    @GET("invitations")
    suspend fun invitations(): PaginatedResponse<VoyageInvitation>

    @POST("voyages/{id}/invitations/accept")
    suspend fun acceptInvitation(@Path("id") voyageId: Int): InvitationActionResponse

    @POST("voyages/{id}/invitations/decline")
    suspend fun declineInvitation(@Path("id") voyageId: Int): InvitationActionResponse

    // ===== Notifications =====

    @GET("notifications")
    suspend fun notifications(): NotificationsResponse

    @POST("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): Map<String, Boolean>

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(): Map<String, Boolean>

    // ===== Voyage Expenses (Tricount-like) =====

    @GET("voyages/{id}/participants")
    suspend fun voyageParticipants(@Path("id") voyageId: Int): PaginatedResponse<VoyageParticipant>

    @POST("voyages/{id}/participants")
    suspend fun addVoyageParticipant(
        @Path("id") voyageId: Int,
        @Body req: CreateParticipantRequest,
    ): WrappedResponse<VoyageParticipant>

    @PUT("voyages/{id}/participants/{pid}")
    suspend fun updateVoyageParticipant(
        @Path("id") voyageId: Int,
        @Path("pid") participantId: Int,
        @Body req: UpdateParticipantRequest,
    ): WrappedResponse<VoyageParticipant>

    @DELETE("voyages/{id}/participants/{pid}")
    suspend fun deleteVoyageParticipant(
        @Path("id") voyageId: Int,
        @Path("pid") participantId: Int,
    ): Map<String, String>

    @GET("voyages/{id}/expenses")
    suspend fun voyageExpenses(
        @Path("id") voyageId: Int,
        @Query("since") since: String? = null,
    ): PaginatedResponse<VoyageExpense>

    @POST("voyages/{id}/expenses")
    suspend fun createVoyageExpense(
        @Path("id") voyageId: Int,
        @Body req: CreateExpenseRequest,
    ): WrappedResponse<VoyageExpense>

    @PUT("voyages/{id}/expenses/{eid}")
    suspend fun updateVoyageExpense(
        @Path("id") voyageId: Int,
        @Path("eid") expenseId: Int,
        @Body req: CreateExpenseRequest,
    ): WrappedResponse<VoyageExpense>

    @DELETE("voyages/{id}/expenses/{eid}")
    suspend fun deleteVoyageExpense(
        @Path("id") voyageId: Int,
        @Path("eid") expenseId: Int,
    ): Map<String, String>

    @GET("voyages/{id}/settlement")
    suspend fun voyageSettlement(@Path("id") voyageId: Int): SettlementResponse

    // ===== Packing List =====

    @GET("packing-categories")
    suspend fun packingCategories(): PaginatedResponse<PackingCategory>

    @GET("packing-template")
    suspend fun packingTemplate(): PaginatedResponse<PackingTemplateItem>

    @POST("packing-template")
    suspend fun createPackingTemplateItem(@Body req: PackingTemplateItemRequest): WrappedResponse<PackingTemplateItem>

    @PUT("packing-template/{itemId}")
    suspend fun updatePackingTemplateItem(
        @Path("itemId") itemId: Int,
        @Body req: PackingTemplateItemRequest,
    ): WrappedResponse<PackingTemplateItem>

    @DELETE("packing-template/{itemId}")
    suspend fun deletePackingTemplateItem(@Path("itemId") itemId: Int): Map<String, String>

    @GET("voyages/{id}/packing")
    suspend fun voyagePacking(
        @Path("id") voyageId: Int,
        @Query("since") since: String? = null,
    ): PaginatedResponse<VoyagePackingItem>

    @POST("voyages/{id}/packing/generate")
    suspend fun generateVoyagePacking(
        @Path("id") voyageId: Int,
    ): PaginatedResponse<VoyagePackingItem>

    @POST("voyages/{id}/packing")
    suspend fun createVoyagePackingItem(
        @Path("id") voyageId: Int,
        @Body req: VoyagePackingCreateRequest,
    ): WrappedResponse<VoyagePackingItem>

    @PUT("voyages/{id}/packing/{itemId}")
    suspend fun updateVoyagePackingItem(
        @Path("id") voyageId: Int,
        @Path("itemId") itemId: Int,
        @Body req: VoyagePackingUpdateRequest,
    ): WrappedResponse<VoyagePackingItem>

    @DELETE("voyages/{id}/packing/{itemId}")
    suspend fun deleteVoyagePackingItem(
        @Path("id") voyageId: Int,
        @Path("itemId") itemId: Int,
    ): Map<String, String>
}
