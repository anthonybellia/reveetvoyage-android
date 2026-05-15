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

    // Devis
    @GET("devis")
    suspend fun devis(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): PaginatedResponse<Devis>

    // Passengers
    @GET("passengers")
    suspend fun passengers(): PaginatedResponse<Passenger>

    @POST("passengers")
    suspend fun createPassenger(@Body req: PassengerRequest): WrappedResponse<Passenger>

    @PUT("passengers/{id}")
    suspend fun updatePassenger(@Path("id") id: Int, @Body req: PassengerRequest): WrappedResponse<Passenger>

    @DELETE("passengers/{id}")
    suspend fun deletePassenger(@Path("id") id: Int): Map<String, String>

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
}
