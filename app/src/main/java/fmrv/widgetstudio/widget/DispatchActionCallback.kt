package fmrv.widgetstudio.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll

/** Point d'entrée de toutes les actions internes d'un widget (actualiser, cocher une tâche…). */
class DispatchActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        when (parameters[ClickIdKey]) {
            "refresh" -> {
                NotesRefreshWorker.enqueueNow(context)
                UniversalWidget().updateAll(context)
            }
        }
    }
}
