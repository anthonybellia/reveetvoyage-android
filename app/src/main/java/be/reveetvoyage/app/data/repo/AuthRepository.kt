package be.reveetvoyage.app.data.repo

import be.reveetvoyage.app.data.api.ApiService
import be.reveetvoyage.app.data.api.OfflineCacheInterceptor
import be.reveetvoyage.app.data.api.OfflineWriteOutbox
import be.reveetvoyage.app.data.api.TokenStore
import be.reveetvoyage.app.data.model.LoginRequest
import be.reveetvoyage.app.data.model.RegisterRequest
import be.reveetvoyage.app.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore,
    private val offlineCache: OfflineCacheInterceptor,
    private val offlineOutbox: OfflineWriteOutbox,
) {
    suspend fun login(email: String, password: String): User {
        val resp = api.login(LoginRequest(email = email, password = password))
        tokenStore.saveToken(resp.token)
        return resp.user
    }

    suspend fun register(prenom: String, nom: String, email: String, password: String, confirm: String): User {
        val resp = api.register(RegisterRequest(prenom, nom, email, password, confirm))
        tokenStore.saveToken(resp.token)
        return resp.user
    }

    suspend fun loadCurrentUser(): User? = runCatching { api.me().user }.getOrNull()

    /**
     * Résultat de la validation de session au démarrage.
     * On distingue volontairement le token rejeté (déconnexion) d'une simple
     * absence de réseau (on garde la session pour le mode hors-ligne).
     */
    sealed class SessionCheck {
        data class Valid(val user: User) : SessionCheck()
        object Invalid : SessionCheck()   // 401/403 : token rejeté par le serveur
        object Offline : SessionCheck()   // réseau injoignable ou 5xx : on garde la session
    }

    suspend fun checkSession(): SessionCheck = try {
        SessionCheck.Valid(api.me().user)
    } catch (e: retrofit2.HttpException) {
        if (e.code() == 401 || e.code() == 403) {
            tokenStore.clearToken()
            SessionCheck.Invalid
        } else {
            // 5xx ou autre : ne pas déconnecter, le serveur peut être temporairement KO
            SessionCheck.Offline
        }
    } catch (e: Exception) {
        // IOException (pas de réseau, timeout...) : mode hors-ligne, on garde la session
        SessionCheck.Offline
    }

    suspend fun logout() {
        runCatching { api.logout() }
        tokenStore.clearToken()
        // Appareil partagé : le compte suivant ne doit pas voir les données cachées
        // ni voir rejouer les écritures hors-ligne en attente.
        offlineCache.clearAll()
        offlineOutbox.clearAll()
    }

    fun isAuthenticated(): Boolean = tokenStore.isAuthenticated()
}
