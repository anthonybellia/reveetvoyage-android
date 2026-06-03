package be.reveetvoyage.app.data.api

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * État partagé du mode "aperçu en tant que voyageur".
 *
 * Volontairement léger et SANS dépendance réseau : c'est ce qui permet à
 * [AuthInterceptor] de le consulter sans créer de cycle Hilt
 * (AuthInterceptor -> OkHttp -> Retrofit -> ApiService -> UserRepository).
 *
 * Le [UserRepository] est l'unique propriétaire qui écrit dedans ; l'interceptor
 * ne fait que lire. Point unique de vérité pour l'en-tête HTTP de prévisualisation.
 */
@Singleton
class PreviewState @Inject constructor() {
    // Vrai si le compte connecté a réellement le rôle admin.
    private val realAdmin = AtomicBoolean(false)
    // Vrai si l'admin a activé le mode "aperçu voyageur".
    private val previewAsUser = AtomicBoolean(false)

    fun setRealAdmin(value: Boolean) { realAdmin.set(value) }
    fun setPreviewAsUser(value: Boolean) { previewAsUser.set(value) }

    /**
     * L'en-tête X-Preview-As-User ne doit partir QUE si l'utilisateur est
     * réellement admin ET qu'il a basculé en mode aperçu.
     */
    fun shouldSendPreviewHeader(): Boolean = realAdmin.get() && previewAsUser.get()
}
