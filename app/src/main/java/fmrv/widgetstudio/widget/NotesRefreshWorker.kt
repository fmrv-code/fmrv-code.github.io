package fmrv.widgetstudio.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import fmrv.widgetstudio.domain.model.FailureReason
import fmrv.widgetstudio.domain.model.RefreshResult
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Actualise le cache des widgets de type liste (notes, actualités, e-mails) puis les redessine.
 * Périodique (≈ 30 min, réseau requis, batterie non faible) ou à la demande.
 */
class NotesRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = applicationContext.widgetEntryPoint()
        val periodic = inputData.getBoolean(KEY_PERIODIC, false)

        val listTypes = entryPoint.widgetRepository().observeWidgets().first()
            .map { it.contentType }
            .filter { it.isList }
            .toSet()
        if (listTypes.isEmpty()) {
            // Plus aucun widget de liste : inutile de continuer à se réveiller. Le travail est replanifié au prochain lancement.
            if (periodic) WorkManager.getInstance(applicationContext).cancelUniqueWork(PERIODIC_NAME)
            return Result.success()
        }

        var retry = false
        for (type in listTypes) {
            val result = entryPoint.listSources().forType(type)?.refresh()
            // Seule une panne réseau mérite une nouvelle tentative ; le reste exige une action de l'utilisateur.
            if (result is RefreshResult.Failure && result.reason == FailureReason.NETWORK) retry = true
        }
        UniversalWidget().updateAll(applicationContext)
        return if (retry) Result.retry() else Result.success()
    }

    companion object {
        private const val PERIODIC_NAME = "notes_refresh_periodic"
        private const val ONE_SHOT_NAME = "notes_refresh_now"
        private const val KEY_PERIODIC = "periodic"

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotesRefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_PERIODIC to true))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Actualisation immédiate ; sans effet si une est déjà en attente ou en cours. */
        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<NotesRefreshWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONE_SHOT_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}

/** Bouton d'actualisation du widget. */
class RefreshNotesAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        NotesRefreshWorker.enqueueNow(context)
    }
}
