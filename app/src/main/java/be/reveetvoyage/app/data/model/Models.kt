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
data class ApiOwner(
    val id: Int,
    val prenom: String? = null,
    val nom: String? = null,
    val email: String? = null,
    val phone: String? = null,
) {
    val fullName: String get() = listOfNotNull(prenom, nom).joinToString(" ").trim()
}

@Serializable
data class Voyage(
    val id: Int,
    val reference: String,
    val titre: String,
    val destination: String,
    val cover_image: String? = null,
    val cover_thumb: String? = null,
    val cover_micro: String? = null,
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
    val owner: ApiOwner? = null,
)

@Serializable
data class VoyageEtape(
    val id: Int,
    val ordre: Int,
    val type: String,
    val titre: String,
    val description: String? = null,
    // Description détaillée (HTML riche provenant de l'éditeur), peut être nulle
    val contenu_html: String? = null,
    val date: String? = null,
    val heure: String? = null,
    val heure_retour: String? = null,
    val lieu: String? = null,
    val lieu_retour: String? = null,
    val adresse: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val compagnie: String? = null,
    val numero_ref: String? = null,
    val cout: Double? = null,
    val image: String? = null,
    // Image de couverture de l'étape (même URL que `image` côté API, exposée séparément)
    val cover: String? = null,
    val fichier: String? = null,
    val images: List<String> = emptyList(),
    // Billets / tickets attachés à l'étape (peut être vide)
    val tickets: List<EtapeTicket> = emptyList(),
    val codes: List<EtapeCode> = emptyList(),
    val linked_app: LinkedApp? = null,
    val is_completed: Boolean = false,
    val completed_at: String? = null,
    // Mode de transport inter-étape ("car","train","plane","bus","navette","taxi","walk")
    val connector_mode: String? = null,
    val connector_duration: String? = null, // ex: "1h25"
    val connector_distance: String? = null, // ex: "1061 km · "
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
    val hasAttachments: Boolean get() = image != null || fichier != null || images.isNotEmpty()
    val hasTickets: Boolean get() = tickets.isNotEmpty()
    val hasConnector: Boolean get() = !connector_mode.isNullOrBlank() ||
        !connector_duration.isNullOrBlank() || !connector_distance.isNullOrBlank()

    /** Aperçu texte de la note : description sinon contenu_html sans balises HTML.
     *  Évite que les étapes dont le contenu n'existe que dans contenu_html
     *  (éditeur riche web) n'affichent rien dans la liste. */
    val notePreview: String get() {
        description?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val html = contenu_html ?: return ""
        if (html.isEmpty()) return ""
        return html
            .replace(Regex("(?i)<br\\s*/?>|<hr[^>]*>|</p>|</div>|</li>"), " ")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
            .replace("&gt;", ">").replace("&#39;", "'").replace("&apos;", "'")
            .replace("&quot;", "\"")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

// Billet / ticket attaché à une étape (PDF ou image)
@Serializable
data class EtapeTicket(
    val url: String,
    val name: String = "",
    val ext: String = "",
    val is_pdf: Boolean = false,
    val is_image: Boolean = false,
    // Passager auquel le billet est attribué (VoyageParticipant), si défini.
    val participant_id: Int? = null,
    val participant_name: String? = null,
)

@Serializable
data class EtapeCode(
    val label: String = "",
    val value: String = "",
)

@Serializable
data class LinkedApp(
    val name: String = "",
    val icon_url: String? = null,
    val app_store_url: String? = null,
    val play_store_url: String? = null,
    val website_url: String? = null,
    val notes: String? = null,
    val credentials: List<LinkedAppCredential> = emptyList(),
) {
    val hasCredentials: Boolean get() = credentials.isNotEmpty()
}

@Serializable
data class LinkedAppCredential(
    val label: String = "",
    val value: String = "",
)

@Serializable
data class Devis(
    val id: Int,
    val nom: String,
    val prenom: String,
    val email: String,
    val statut: String,
    val telephone: String? = null,
    val titre_voyage: String? = null,
    val destination: String? = null,
    val destination_souhaitee: String? = null,
    val type_voyage: String? = null,
    val nb_personnes: Int? = null,
    val participants: String? = null,
    val budget: String? = null,
    val duree: String? = null,
    val dates_souhaitees: String? = null,
    val flexible_dates: String? = null,
    val lieu_depart: String? = null,
    val message: String? = null,
    val cadre: String? = null,
    val hebergement: String? = null,
    val besoins_specifiques: String? = null,
    val activites: String? = null,
    val activites_eviter: String? = null,
    val imperatifs: String? = null,
    val evenement: String? = null,
    val montant_estime: Double? = null,
    val date_depart_prevue: String? = null,
    val date_retour_prevue: String? = null,
    // Présent → un voyage est déjà lié (on masque alors « Convertir en voyage »).
    val voyage_id: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val owner: ApiOwner? = null,
)

/** Corps de PUT /devis/{id} : tous les champs sont optionnels (envoie ce qu'on édite). */
@Serializable
data class DevisUpdateRequest(
    val statut: String? = null,
    val notes_admin: String? = null,
    val destination: String? = null,
    val destination_souhaitee: String? = null,
    val dates_souhaitees: String? = null,
    val duree: String? = null,
    val nb_personnes: Int? = null,
    val participants: String? = null,
    val budget: String? = null,
    val type_voyage: String? = null,
    val message: String? = null,
    val titre_voyage: String? = null,
    val montant_estime: Double? = null,
    val date_depart_prevue: String? = null,
    val date_retour_prevue: String? = null,
)

/** Note interne threadée d'un devis : { id, contenu, author, created_at }. */
@Serializable
data class DevisNote(
    val id: Int,
    val contenu: String,
    val author: String? = null,
    val created_at: String? = null,
)

/** Corps de POST /devis/{id}/notes. */
@Serializable
data class DevisNoteRequest(val contenu: String)

/** Enveloppe { data: { devis_id, voyage_id } } renvoyée par convert-to-voyage. */
@Serializable
data class DevisConvertResult(val devis_id: Int, val voyage_id: Int)

@Serializable
data class Passenger(
    val id: Int,
    val nom: String,
    val prenom: String,
    val full_name: String,
    val email: String? = null,
    val date_naissance: String? = null,
    val type_doc: String? = null,
    val num_doc: String? = null,
    val nationalite: String? = null,
    val notes: String? = null,
    val expiration_doc: String? = null,
    val is_default: Boolean = false,
    val account_user_id: Int? = null,
    val is_me: Boolean? = null,
) {
    val hasAccount: Boolean get() = account_user_id != null
    val isMe: Boolean get() = is_me == true
}

@Serializable
data class ConvertPassengerRequest(
    val email: String,
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

/** Corps de requête pour supprimer un billet d'étape (identifié par son url). */
@Serializable
data class DeleteTicketRequest(val url: String)

/**
 * Corps de la requête idempotente « set » de l'état terminé d'une étape.
 * On envoie l'ÉTAT CIBLE (pas un toggle relatif) : rejouable sans risque par
 * l'outbox hors-ligne (last-write-wins). Miroir d'iOS `setEtapeCompletion`.
 */
@Serializable
data class EtapeCompletionRequest(val is_completed: Boolean)

/** Type d'écriture différée supportée hors-ligne (actions booléennes idempotentes). */
@Serializable
enum class OfflineWriteKind { etapeCompletion }

/**
 * Une écriture faite hors-ligne, en attente de synchronisation. On mémorise
 * l'ÉTAT CIBLE désiré (pas une action relative), ce qui rend le rejeu idempotent.
 * Miroir de `OfflineWrite` côté iOS.
 */
@Serializable
data class OfflineWrite(
    val kind: OfflineWriteKind,
    val voyageId: Int,
    val entityId: Int,        // etapeId
    val value: Boolean,       // état désiré (true = fait)
    val updatedAt: Long,      // epoch millis, pour l'ordre de rejeu
) {
    /** Clé stable par entité : plusieurs basculements se réduisent au dernier état (LWW). */
    val id: String get() = "${kind.name}:$voyageId:$entityId"
}

@Serializable
data class UnreadCountResponse(val count: Int)

@Serializable
data class PaginatedResponse<T>(val data: List<T> = emptyList())

@Serializable
data class WrappedResponse<T>(val data: T)

// Enveloppe { data: ... | null } : utilisée par GET /users/search où `data`
// vaut null si aucun compte ne correspond exactement à l'email.
@Serializable
data class NullableWrappedResponse<T>(val data: T? = null)

@Serializable
data class PageResponse(
    val slug: String,
    val title: String,
    val content_html: String,
    val meta_description: String? = null,
    val updated_at: String? = null,
)

// ===== Weather =====
@Serializable
data class WeatherResponse(
    val timezone: String? = null,
    val current: WeatherCurrent? = null,
    val forecast: List<WeatherDay> = emptyList(),
)

@Serializable
data class WeatherCurrent(
    val temp: Double? = null,
    val feels_like: Double? = null,
    val code: Int? = null,
    val wind: Double? = null,
    val humidity: Double? = null,
    val is_day: Boolean = true,
)

@Serializable
data class WeatherDay(
    val date: String,
    val temp_max: Double? = null,
    val temp_min: Double? = null,
    val code: Int? = null,
    val precip_prob: Int? = null,
)

// ===== Voyage Expenses (Tricount-like) =====

@Serializable
data class VoyageParticipant(
    val id: Int,
    val voyage_id: Int,
    val user_id: Int? = null,
    val display_name: String,
    val is_guest: Boolean = false,
    val avatar_url: String? = null,
)

@Serializable
data class VoyageExpenseSplit(
    val participant_id: Int,
    val share_weight: Double = 1.0,
)

@Serializable
data class ExpensePaidBy(
    val id: Int,
    val display_name: String,
    val avatar_url: String? = null,
)

@Serializable
data class VoyageExpense(
    val id: Int,
    val voyage_id: Int,
    val paid_by_participant_id: Int,
    val title: String,
    val amount_cents: Long,
    val currency: String = "EUR",
    val category: String = "autre",
    val spent_at: String? = null,
    val description: String? = null,
    val splits: List<VoyageExpenseSplit> = emptyList(),
    val paid_by: ExpensePaidBy? = null,
    val location_name: String? = null,
    val location_latitude: Double? = null,
    val location_longitude: Double? = null,
) {
    val amount: Double get() = amount_cents / 100.0
}

@Serializable
data class Place(
    val name: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val type: String? = null,
)

@Serializable
data class SettlementBalance(
    val participant_id: Int,
    val name: String,
    val balance_cents: Long,
) {
    val balance: Double get() = balance_cents / 100.0
}

@Serializable
data class SettlementTransaction(
    val from_participant_id: Int,
    val from_name: String,
    val to_participant_id: Int,
    val to_name: String,
    val amount_cents: Long,
) {
    val amount: Double get() = amount_cents / 100.0
}

@Serializable
data class SettlementResponse(
    val balances: List<SettlementBalance> = emptyList(),
    val transactions: List<SettlementTransaction> = emptyList(),
    val total_cents: Long = 0,
    val by_category: Map<String, Long> = emptyMap(),
) {
    val total: Double get() = total_cents / 100.0
}

@Serializable
data class CreateParticipantRequest(
    val display_name: String? = null,
    val email: String? = null,
)

@Serializable
data class UpdateParticipantRequest(
    val display_name: String,
)

@Serializable
data class CreateExpenseRequest(
    val title: String,
    val amount: Double,
    val currency: String? = null,
    val paid_by_participant_id: Int,
    val spent_at: String? = null,
    val description: String? = null,
    val category: String? = null,
    val splits: List<VoyageExpenseSplit>? = null,
    val location_name: String? = null,
    val location_latitude: Double? = null,
    val location_longitude: Double? = null,
)

// ===== Voyage Members / Collaboration =====

// Un membre actif (lié à un compte utilisateur) du voyage.
@Serializable
data class VoyageMember(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val is_owner: Boolean = false,
    val avatar_url: String? = null,
)

// Une invitation en attente : la personne n'a pas encore de compte / pas encore accepté.
@Serializable
data class PendingInvite(
    val email: String,
    val role: String = "collaborator",
)

// Réponse de GET /voyages/{id}/members
@Serializable
data class MembersResponse(
    val members: List<VoyageMember> = emptyList(),
    val pending: List<PendingInvite> = emptyList(),
)

// Corps de POST /voyages/{id}/members
@Serializable
data class InviteRequest(
    val email: String,
    val role: String = "collaborator",
)

// ===== Invitations & autocomplete invitation =====

// Résultat d'une recherche d'utilisateur par email exact.
// Renvoyé par GET /users/search?email= dans { data: ... } ; data == null si
// aucun compte exact ne correspond (→ on proposera l'invitation par email).
@Serializable
data class UserSearchResult(
    val id: Int,
    val prenom: String? = null,
    val name: String? = null,
    val email: String,
    val avatar_url: String? = null,
) {
    // Libellé d'affichage : privilégie le nom complet, retombe sur prénom puis email.
    val displayName: String get() {
        name?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        prenom?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return email
    }
}

// Voyage référencé par une invitation (sous-ensemble léger de Voyage).
@Serializable
data class InvitedVoyage(
    val id: Int,
    val titre: String? = null,
    val destination: String? = null,
    val date_depart: String? = null,
    val date_retour: String? = null,
    val image: String? = null,
)

// Invitation en attente reçue par l'utilisateur courant.
// Renvoyée par GET /invitations dans le tableau `data`.
@Serializable
data class VoyageInvitation(
    val voyage: InvitedVoyage,
    val role: String = "collaborator",
    val inviter_name: String? = null,
    val created_at: String? = null,
)

// Réponse des endpoints accept / decline ({ ok, linked? }).
@Serializable
data class InvitationActionResponse(
    val ok: Boolean = false,
    val linked: Boolean? = null,
)

// ===== Notifications =====

// Notification persistée côté serveur, listée par GET /notifications.
@Serializable
data class AppNotification(
    val id: Int,
    val type: String? = null,
    val titre: String? = null,
    val message: String? = null,
    val url: String? = null,
    val lu: Boolean = false,
    val created_at: String? = null,
) {
    // Vrai pour une invitation à un voyage → router vers « Mes invitations ».
    val isVoyageInvite: Boolean get() = type == "voyage_invite"
}

// Enveloppe de GET /notifications ({ data: [...], unread_count }).
@Serializable
data class NotificationsResponse(
    val data: List<AppNotification> = emptyList(),
    val unread_count: Int = 0,
)

// ===== Packing List =====

@Serializable
data class PackingCategory(
    val id: Int,
    val key: String,
    val name: String,
    val icon_key: String,
    val color: String? = null,
)

@Serializable
data class PackingTemplateItem(
    val id: Int,
    val category_id: Int? = null,
    val label: String,
    val sort_order: Int,
)

@Serializable
data class VoyagePackingItem(
    val id: Int,
    val voyage_participant_id: Int,
    val category_id: Int? = null,
    val label: String,
    val is_checked: Boolean,
    val sort_order: Int,
    val source_template_item_id: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
)

@Serializable
data class PackingTemplateItemRequest(
    val category_id: Int? = null,
    val label: String,
    val sort_order: Int? = null,
)

@Serializable
data class VoyagePackingCreateRequest(
    val category_id: Int? = null,
    val label: String,
    val sort_order: Int? = null,
    val keep_for_next: Boolean = false,
)

@Serializable
data class VoyagePackingUpdateRequest(
    val is_checked: Boolean? = null,
    val label: String? = null,
    val category_id: Int? = null,
    val sort_order: Int? = null,
)
