package be.reveetvoyage.app.ui.screens.admin

import kotlinx.serialization.Serializable

/**
 * Owner block returned by the backend on `/api/devis*` and `/api/voyages*`
 * when the authenticated user is an admin (role == "admin").
 *
 * For non-admin viewers the API omits this field, so on the client it stays
 * `null` and no customer info ever leaks to other customers.
 *
 * REFACTOR (out-of-sandbox follow-up):
 *   This class belongs in `be.reveetvoyage.app.data.model.Models.kt`
 *   alongside the other API DTOs. It lives here only because the sub-agent
 *   sandbox restricted writes to `ui/screens/`. Once moved:
 *     1. Delete this file.
 *     2. Add `val owner: ApiOwner? = null` to `data class Devis` and
 *        `data class Voyage` so kotlinx-serialization can populate it
 *        from the JSON response (the backend already sends it for admins).
 *     3. Update the `import` in `AdminBanner.kt`,
 *        `HomeScreen.kt`, `DevisScreens.kt` and `VoyagesScreens.kt`
 *        from `be.reveetvoyage.app.ui.screens.admin.ApiOwner` to
 *        `be.reveetvoyage.app.data.model.ApiOwner`.
 *
 * Until step 2 happens, the `OwnerRow` UI hooks below are wired in but
 * dormant — they always read `owner = null` because the model doesn't
 * expose the field yet. The admin banner itself works fine since it only
 * reads `user.role`.
 */
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
