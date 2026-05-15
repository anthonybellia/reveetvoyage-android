package be.reveetvoyage.app.data.api

import be.reveetvoyage.app.data.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Auth (public)
    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body req: RegisterRequest): AuthResponse

    // Auth (authenticated)
    @GET("auth/me")
    suspend fun me(): MeResponse

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

    // Messages
    @GET("messages")
    suspend fun messages(@Query("since") since: String? = null): PaginatedResponse<Message>

    @POST("messages")
    suspend fun sendMessage(@Body req: SendMessageRequest): WrappedResponse<Message>

    @GET("messages/unread-count")
    suspend fun unreadCount(): UnreadCountResponse
}
