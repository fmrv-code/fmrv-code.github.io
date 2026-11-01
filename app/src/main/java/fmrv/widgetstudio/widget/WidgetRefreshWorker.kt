package fmrv.widgetstudio.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Redessine les widgets : le jour courant d'un calendrier, un compte à rebours ou les valeurs de l'appareil
 * changent sans que l'application soit ouverte. Toutes les 30 minutes, et juste après minuit.
 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        UniversalWidget().updateAll(applicationContext)
        if (inputData.getBoolean(KEY_MIDNIGHT, false)) scheduleMidnight(applicationContext, replace = true)
        return Result.success()
    }

    companion object {
        private const val PERIODIC_NAME = "widget_refresh"
        private const val MIDNIGHT_NAME = "widget_refresh_midnight"
        private const val KEY_MIDNIGHT = "midnight"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
            scheduleMidnight(context, replace = false)
        }

        /** Planifie le prochain redessin juste après minuit (heure locale) ; le travail se replanifie lui-même. */
        fun scheduleMidnight(context: Context, replace: Boolean) {
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay().plusSeconds(5)
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setInitialDelay(Duration.between(now, nextMidnight))
                .setInputData(workDataOf(KEY_MIDNIGHT to true))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                MIDNIGHT_NAME,
                if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
