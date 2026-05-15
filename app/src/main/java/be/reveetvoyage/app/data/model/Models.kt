package be.reveetvoyage.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val prenom: String,
    val nom: String,
    val email: String,
    val phone: String? = null,
    val avatar: String? = null,
    val role: String = "customer",
    val date_naissance: String? = null,
    val nationalite: String? = null,
    val adresse: String? = null,
    val code_postal: String? = null,
    val ville: String? = null,
    val pays: String? = null,
    val language: String? = null,
    val notif_emails: Boolean? = null,
    val notif_promo: Boolean? = null,
    val notif_voyages: Boolean? = null,
    val last_login_at: String? = null,
    val created_at: String? = null,
) {
    val fullName: String get() = "$prenom $nom"
}

@Serializable
data class AuthResponse(val user: User, val token: String)

@Serializable
data class MeResponse(val user: User)

@Serializable
data class LoginRequest(val email: String, val password: String, val device_name: String = "Android App")

@Serializable
data class RegisterRequest(
    val prenom: String,
    val nom: String,
    val email: String,
    val password: String,
    val password_confirmation: String,
    val device_name: String = "Android App",
)

@Serializable
data class UpdateProfileRequest(
    val prenom: String? = null,
    val nom: String? = null,
    val phone: String? = null,
    val date_naissance: String? = null,
    val nationalite: String? = null,
    val adresse: String? = null,
    val code_postal: String? = null,
    val ville: String? = null,
    val pays: String? = null,
)

@Serializable
data class UpdatePasswordRequest(
    val current_password: String,
    val password: String,
    val password_confirmation: String,
)

@Serializable
data class UpdatePreferencesRequest(
    val language: String? = null,
    val notif_emails: Boolean? = null,
    val notif_promo: Boolean? = null,
    val notif_voyages: Boolean? = null,
)

@Serializable
data class Voyage(
    val id: Int,
    val reference: String,
    val titre: String,
    val destination: String,
    val date_depart: String? = null,
    val date_retour: String? = null,
    val montant_total: Double = 0.0,
    val montant_acompte: Double = 0.0,
    val montant_paye: Double = 0.0,
    val statut: String,
    val statut_label: String,
    val description: String? = null,
    val participants: List<String>? = null,
    val token: String? = null,
    val etapes: List<VoyageEtape>? = null,
)

@Serializable
data class VoyageEtape(
    val id: Int,
    val ordre: Int,
    val type: String,
    val titre: String,
    val description: String? = null,
    val date: String? = null,
    val heure: String? = null,
    val lieu: String? = null,
    val adresse: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val cout: Double? = null,
    val is_completed: Boolean = false,
    val completed_at: String? = null,
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
}

@Serializable
data class Devis(
    val id: Int,
    val nom: String,
    val prenom: String,
    val email: String,
    val statut: String,
    val titre_voyage: String? = null,
    val destination: String? = null,
    val destination_souhaitee: String? = null,
    val type_voyage: String? = null,
    val nb_personnes: Int? = null,
    val budget: String? = null,
    val duree: String? = null,
    val message: String? = null,
    val cadre: String? = null,
    val hebergement: String? = null,
    val activites: String? = null,
)

@Serializable
data class Passenger(
    val id: Int,
    val nom: String,
    val prenom: String,
    val full_name: String,
    val date_naissance: String? = null,
    val type_doc: String? = null,
    val num_doc: String? = null,
    val nationalite: String? = null,
    val notes: String? = null,
    val expiration_doc: String? = null,
    val is_default: Boolean = false,
)

@Serializable
data class PassengerRequest(
    val nom: String,
    val prenom: String,
    val date_naissance: String? = null,
    val type_doc: String? = null,
    val num_doc: String? = null,
    val nationalite: String? = null,
    val notes: String? = null,
    val expiration_doc: String? = null,
    val is_default: Boolean = false,
    val langues: List<String>? = null,
)

@Serializable
data class Message(
    val id: Int,
    val sender: String,
    val body: String,
    val attachment_url: String? = null,
    val attachment_type: String? = null,   // "image" | "pdf" | "other"
    val attachment_name: String? = null,
    val attachment_size: Int? = null,
    val attachment_mime: String? = null,
    val read_at: String? = null,
    val is_read: Boolean = false,
    val created_at: String,
) {
    val isFromUser: Boolean get() = sender == "user"
    val isFromAdmin: Boolean get() = sender == "admin"
    val hasAttachment: Boolean get() = !attachment_url.isNullOrBlank()
}

@Serializable
data class SendMessageRequest(val body: String)

@Serializable
data class UnreadCountResponse(val count: Int)

@Serializable
data class PaginatedResponse<T>(val data: List<T> = emptyList())

@Serializable
data class WrappedResponse<T>(val data: T)

@Serializable
data class PageResponse(
    val slug: String,
    val title: String,
    val content_html: String,
    val meta_description: String? = null,
    val updated_at: String? = null,
)
