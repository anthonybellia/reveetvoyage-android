package be.reveetvoyage.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import be.reveetvoyage.app.MainActivity
import be.reveetvoyage.app.R
import be.reveetvoyage.app.data.model.Voyage
import be.reveetvoyage.app.data.model.VoyageEtape
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_ID = "voyage_reminders"
private const val WORK_PREFIX = "voyage-reminder-"

/// Schedules J-15 / J-7 / J-2 / J-1 voyage reminders via WorkManager.
/// Equivalent of iOS NotificationManager. No Firebase needed.
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    init { ensureChannel() }

    fun scheduleVoyage(voyage: Voyage) {
        cancelVoyage(voyage.id)

        val depart = voyage.date_depart?.let { parseIso(it) } ?: return
        if (depart.before(Date())) return

        val cal = Calendar.getInstance()
        val arrival2hBefore = SimpleDateFormat("HH'h'mm", Locale("fr", "BE")).format(
            Date(depart.time - 2 * 60 * 60 * 1000)
        )

        val specs = listOf(
            ReminderSpec(15, 10, "j15", "✈️ Plus que 15 jours !",
                "Ton voyage à ${voyage.destination} se rapproche. Pense aux passeports et à la valise."),
            ReminderSpec(7, 10, "j7", "🎒 J-7 avant le départ",
                "Une semaine avant ${voyage.destination}. Vérifie tes documents et boucle ton planning."),
            ReminderSpec(2, 10, "j2", "📋 J-2, on se prépare",
                "Plus que 48h avant ${voyage.destination}. Pense à l'enregistrement en ligne."),
            ReminderSpec(1, 10, "j1morning", "🛫 Demain c'est le départ !",
                "Direction ${voyage.destination} demain. Vérifie les horaires de vol."),
            ReminderSpec(1, 18, "j1airport", "🛂 Heure idéale à l'aéroport",
                "Sois à l'aéroport demain à $arrival2hBefore (2h avant le décollage de ${voyage.destination})."),
        )

        for (spec in specs) {
            cal.time = depart
            cal.add(Calendar.DAY_OF_YEAR, -spec.daysBefore)
            cal.set(Calendar.HOUR_OF_DAY, spec.hour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val fireTime = cal.timeInMillis
            val now = System.currentTimeMillis()
            if (fireTime <= now) continue

            val data = workDataOf(
                "voyage_id" to voyage.id,
                "title" to spec.title,
                "body" to spec.body,
            )
            val tag = "voyage-${voyage.id}"
            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(fireTime - now, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(tag)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "$WORK_PREFIX${voyage.id}-${spec.key}",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        // 30-min-before reminders for each étape with date + heure
        for (etape in voyage.etapes.orEmpty()) {
            val etapeFireTime = etapeFireTimeMillis(etape) ?: continue
            val now = System.currentTimeMillis()
            if (etapeFireTime <= now) continue

            val body = buildString {
                etape.heure?.let { append("Prévu à $it") }
                val loc = etape.lieu
                if (!loc.isNullOrBlank()) {
                    if (isNotEmpty()) append(" — ")
                    append(loc)
                }
                if (isEmpty()) append("Prépare-toi pour la prochaine étape.")
            }

            val data = workDataOf(
                "voyage_id" to voyage.id,
                "title" to "Dans 30 min : ${etape.titre}",
                "body" to body,
            )
            val tag = "voyage-${voyage.id}"
            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(etapeFireTime - now, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(tag)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_PREFIX}${voyage.id}-etape-${etape.id}-30min",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    fun cancelVoyage(voyageId: Int) {
        WorkManager.getInstance(context).cancelAllWorkByTag("voyage-$voyageId")
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Rappels voyage", NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Rappels avant le départ (15j, 7j, 48h, 24h)"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun etapeFireTimeMillis(etape: VoyageEtape): Long? {
        val dateStr = etape.date ?: return null
        val heure = etape.heure ?: return null
        val baseDate = parseIso(dateStr) ?: return null
        val parts = heure.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.size < 2) return null

        val cal = Calendar.getInstance()
        cal.time = baseDate
        cal.set(Calendar.HOUR_OF_DAY, parts[0])
        cal.set(Calendar.MINUTE, parts[1])
        cal.set(Calendar.SECOND, 0)
        cal.add(Calendar.MINUTE, -30)
        return cal.timeInMillis
    }

    private fun parseIso(s: String): Date? {
        // Try a few common formats from Laravel
        for (fmt in listOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd")) {
            runCatching {
                return SimpleDateFormat(fmt, Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(s)
            }
        }
        return null
    }

    private data class ReminderSpec(
        val daysBefore: Int,
        val hour: Int,
        val key: String,
        val title: String,
        val body: String,
    )
}

class NotificationWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: return Result.success()
        val body = inputData.getString("body") ?: ""
        val voyageId = inputData.getInt("voyage_id", 0)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("voyage_id", voyageId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(applicationContext, voyageId, intent, pendingFlags)

        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            val notifId = voyageId * 100 + ((System.currentTimeMillis() % 100).toInt())
            NotificationManagerCompat.from(applicationContext).notify(notifId, notif)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted (Android 13+)
        }
        return Result.success()
    }
}
