package be.reveetvoyage.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.model.User
import be.reveetvoyage.app.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow(repo.isAuthenticated())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun bootstrap() {
        if (!repo.isAuthenticated()) {
            _isAuthenticated.value = false
            return
        }
        // Token présent : on considère l'utilisateur connecté immédiatement.
        // Indispensable pour ouvrir l'app hors-ligne (avion) sans renvoyer au login.
        _isAuthenticated.value = true
        viewModelScope.launch {
            when (val res = repo.checkSession()) {
                is AuthRepository.SessionCheck.Valid -> {
                    _currentUser.value = res.user
                    _isAuthenticated.value = true
                }
                AuthRepository.SessionCheck.Invalid -> {
                    // Token réellement rejeté par le serveur : on déconnecte.
                    _isAuthenticated.value = false
                }
                AuthRepository.SessionCheck.Offline -> {
                    // Pas de réseau : on garde la session, la couche data sert le cache.
                }
            }
        }
    }

    suspend fun login(email: String, password: String): Boolean {
        _isLoading.value = true
        _errorMessage.value = null
        return try {
            _currentUser.value = repo.login(email.trim(), password)
            _isAuthenticated.value = true
            true
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Erreur de connexion"
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun register(prenom: String, nom: String, email: String, password: String, confirm: String): Boolean {
        _isLoading.value = true
        _errorMessage.value = null
        return try {
            _currentUser.value = repo.register(prenom.trim(), nom.trim(), email.trim(), password, confirm)
            _isAuthenticated.value = true
            true
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Erreur d'inscription"
            false
        } finally {
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _currentUser.value = null
            _isAuthenticated.value = false
        }
    }
}
