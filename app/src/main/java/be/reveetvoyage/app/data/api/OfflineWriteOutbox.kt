package be.reveetvoyage.app.data.api

import android.content.Context
import be.reveetvoyage.app.data.model.OfflineWrite
import be.reveetvoyage.app.data.model.OfflineWriteKind
import be.reveetvoyage.app.data.repo.VoyageRepository
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File d'attente persistante des écritures faites hors-ligne (iOS : `OfflineOutbox`).
 *
 * Volontairement limitée aux actions booléennes idempotentes et sans conflit
 * (last-write-wins) : cocher une étape comme faite. On mémorise l'ÉTAT CIBLE
 * désiré (pas une action relative), ce qui rend le rejeu idempotent et sûr.
 *
 * Stockée dans `filesDir` (pas le cache) pour survivre à l'éviction système :
 * une écriture en attente ne doit jamais être perdue silencieusement.
 *
 * `repo` est injecté en `Lazy` pour casser le cycle Dagger
 * (AuthRepository → outbox, et outbox → VoyageRepository).
 */
@Singleton
class OfflineWriteOutbox @Inject constructor(
    @ApplicationContext context: Context,
    private val repo: Lazy<VoyageRepository>,
    private val json: Json,
) {
    private val file = File(context.filesDir, "offline-outbox.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    // État en mémoire, indexé par OfflineWrite.id (LWW par entité).
    private val pending = MutableStateFlow<Map<String, OfflineWrite>>(loadFromDisk())

    /** Nombre d'écritures en attente — observable pour un éventuel badge UI. */
    val pendingCount: StateFlow<Int> = MutableStateFlow(pending.value.size).also { count ->
        scope.launch { pending.collect { count.value = it.size } }
    }.asStateFlow()

    /** Enfile (ou écrase) l'écriture pour cette entité, puis tente un envoi immédiat. */
    fun enqueue(write: OfflineWrite) {
        pending.value = pending.value + (write.id to write)
        persist()
        flushAsync()
    }

    /** Déclenche un flush en arrière-plan (retour réseau, premier plan, démarrage). */
    fun flushAsync() {
        scope.launch { flush() }
    }

    /**
     * Tente d'envoyer toutes les écritures en attente, des plus anciennes aux plus
     * récentes. Sûr à rappeler. S'arrête au premier échec réseau (retry plus tard)
     * ou sur 401/403 (session invalide : on conserve la file).
     */
    suspend fun flush() {
        mutex.withLock {
            if (pending.value.isEmpty()) return
            val ordered = pending.value.values.sortedBy { it.updatedAt }
            for (write in ordered) {
                try {
                    send(write)
                    pending.value = pending.value - write.id
                    persist()
                } catch (e: retrofit2.HttpException) {
                    if (e.code() == 401 || e.code() == 403) break // session invalide
                    // 5xx/422 : on conserve et on réessaiera au prochain flush.
                    break
                } catch (e: Exception) {
                    break // réseau indisponible : retry plus tard
                }
            }
        }
    }

    /** Vide la file (déconnexion : ne pas rejouer les écritures sur un autre compte). */
    fun clearAll() {
        pending.value = emptyMap()
        persist()
    }

    private suspend fun send(w: OfflineWrite) {
        when (w.kind) {
            OfflineWriteKind.etapeCompletion ->
                repo.get().setEtapeCompletion(w.voyageId, w.entityId, w.value)
        }
    }

    private fun persist() {
        runCatching {
            file.writeText(json.encodeToString(pending.value.values.toList()))
        }
    }

    private fun loadFromDisk(): Map<String, OfflineWrite> = runCatching {
        if (!file.exists()) return emptyMap()
        json.decodeFromString<List<OfflineWrite>>(file.readText())
            .associateBy { it.id }
    }.getOrDefault(emptyMap())
}
