package be.reveetvoyage.app.data.api

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache disque minimal des réponses GET pour le mode hors-ligne (lecture seule).
 *
 * On mémorise le corps brut de chaque GET réussi (indexé par l'URL complète) et
 * on le ressert quand le réseau est indisponible. Indépendant des en-têtes
 * Cache-Control du serveur. Symétrique de `OfflineCache` côté iOS.
 *
 * Aucune mutation n'est mise en file ici : c'est strictement de la lecture.
 */
@Singleton
class OfflineCacheInterceptor @Inject constructor(
    @ApplicationContext context: Context,
) : Interceptor {

    private val dir = File(context.cacheDir, "api-offline-cache").apply { mkdirs() }

    private fun fileFor(url: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
        val name = digest.joinToString("") { "%02x".format(it) }
        return File(dir, name)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") return chain.proceed(request)

        val file = fileFor(request.url.toString())
        return try {
            val response = chain.proceed(request)
            if (response.isSuccessful) {
                // peekBody ne consomme pas le corps original transmis à l'appelant.
                runCatching { file.writeBytes(response.peekBody(Long.MAX_VALUE).bytes()) }
            }
            response
        } catch (e: IOException) {
            // Réseau indisponible : on ressert le dernier corps connu s'il existe.
            val cached = file.takeIf { it.exists() }?.readBytes() ?: throw e
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK (cache hors-ligne)")
                .addHeader("X-Offline-Cache", "true")
                .body(cached.toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        }
    }

    /** Purge du cache, à appeler à la déconnexion (appareil partagé). */
    fun clearAll() {
        runCatching {
            dir.deleteRecursively()
            dir.mkdirs()
        }
    }
}
