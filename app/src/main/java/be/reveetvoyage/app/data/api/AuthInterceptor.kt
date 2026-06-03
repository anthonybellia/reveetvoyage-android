package be.reveetvoyage.app.data.api

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val previewState: PreviewState,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
            .header("Accept", "application/json")
            .apply {
                tokenStore.getToken()?.let { header("Authorization", "Bearer $it") }
                // Aperçu "en tant que voyageur" : quand un vrai admin a activé le
                // toggle, on demande au backend de le scoper comme un client normal.
                // Point unique d'ajout de l'en-tête (cf. PreviewState / UserRepository).
                if (previewState.shouldSendPreviewHeader()) {
                    header("X-Preview-As-User", "1")
                }
            }
            .build()
        return chain.proceed(req)
    }
}
