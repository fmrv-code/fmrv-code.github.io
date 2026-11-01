package fmrv.widgetstudio.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import fmrv.widgetstudio.domain.repository.WidgetRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Supprime les configurations orphelines (widgets retirés pendant que l'application était arrêtée). */
@Singleton
class WidgetReconciler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: WidgetRepository,
) {
    suspend fun run() {
        val manager = AppWidgetManager.getInstance(context)
        val placed = listOf(
            SmallWidgetReceiver::class.java,
            MediumWidgetReceiver::class.java,
            WideWidgetReceiver::class.java,
            BarWidgetReceiver::class.java,
        ).flatMap { manager.getAppWidgetIds(ComponentName(context, it)).toList() }.toSet()
        repository.retainOnly(placed)
    }
}
