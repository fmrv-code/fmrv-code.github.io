package fmrv.widgetstudio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import fmrv.widgetstudio.domain.model.PinResult
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.domain.repository.WidgetPinner
import fmrv.widgetstudio.domain.repository.WidgetUpdater
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlanceWidgetPinner @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetPinner {

    override fun requestPin(size: WidgetSizeOption): PinResult {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return PinResult.UNSUPPORTED

        val receiver = when (size) {
            WidgetSizeOption.SMALL -> SmallWidgetReceiver::class.java
            WidgetSizeOption.MEDIUM -> MediumWidgetReceiver::class.java
            WidgetSizeOption.WIDE -> WideWidgetReceiver::class.java
            WidgetSizeOption.BAR -> BarWidgetReceiver::class.java
        }
        // Mutable : le système y ajoute l'identifiant du widget une fois posé.
        val callback = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, PinResultReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val requested = manager.requestPinAppWidget(ComponentName(context, receiver), null, callback)
        return if (requested) PinResult.REQUESTED else PinResult.FAILED
    }
}

@Singleton
class GlanceWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetUpdater {

    override suspend fun refresh(appWidgetId: Int) {
        val glanceId = try {
            GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
        } catch (e: IllegalArgumentException) {
            return // le widget n'existe plus
        }
        UniversalWidget().update(context, glanceId)
    }

    override suspend fun refreshAll() {
        UniversalWidget().updateAll(context)
    }
}
