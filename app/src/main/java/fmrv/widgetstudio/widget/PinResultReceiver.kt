package fmrv.widgetstudio.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Appelé par le lanceur quand un widget épinglé depuis l'application a été posé.
 * Filet de sécurité : si l'activité de configuration n'a pas déjà adopté la configuration en attente, on le fait ici.
 */
class PinResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val entryPoint = context.widgetEntryPoint()
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (entryPoint.widgetRepository().adoptPending(appWidgetId) != null) {
                    entryPoint.widgetUpdater().refresh(appWidgetId)
                }
            } finally {
                pending?.finish()
            }
        }
    }
}
