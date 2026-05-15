package be.reveetvoyage.app.data.api

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
            .header("Accept", "application/json")
            .apply {
                tokenStore.getToken()?.let { header("Authorization", "Bearer $it") }
            }
            .build()
        return chain.proceed(req)
    }
}
