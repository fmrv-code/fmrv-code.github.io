package fmrv.widgetstudio.widget

import android.content.Context
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
import fmrv.widgetstudio.kinds.Kinds
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Actualise les sources distantes des widgets posés (notes, actualités, e-mails…) puis les redessine.
 * Périodique (≈ 30 min, réseau requis, batterie non faible) ou à la demande.
 */
class NotesRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entry = applicationContext.widgetEntryPoint()
        val periodic = inputData.getBoolean(KEY_PERIODIC, false)

        // Une source par type de widget posé, sans doublon.
        val sources = entry.widgetRepository().observeWidgets().first()
            .mapNotNull { Kinds.byId(it.kindId)?.dataSource(entry) }
            .distinct()
        if (sources.isEmpty()) {
            // Aucun widget n'a besoin du réseau : inutile de continuer à se réveiller. Replanifié au prochain lancement.
            if (periodic) WorkManager.getInstance(applicationContext).cancelUniqueWork(PERIODIC_NAME)
            return Result.success()
        }

        var retry = false
        for (source in sources) {
            val result = source.refresh()
            // Seule une panne réseau mérite une nouvelle tentative ; le reste exige une action de l'utilisateur.
            if (result is RefreshResult.Failure && result.reason == FailureReason.NETWORK) retry = true
        }
        UniversalWidget().updateAll(applicationContext)
        return if (retry) Result.retry() else Result.success()
    }

    companion object {
        private const val PERIODIC_NAME = "data_refresh_periodic"
        private const val ONE_SHOT_NAME = "data_refresh_now"
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
