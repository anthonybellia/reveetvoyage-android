package be.reveetvoyage.app.data.repo

import be.reveetvoyage.app.data.api.ApiService
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

    suspend fun logout() {
        runCatching { api.logout() }
        tokenStore.clearToken()
    }

    fun isAuthenticated(): Boolean = tokenStore.isAuthenticated()
}
