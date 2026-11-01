package fmrv.widgetstudio.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Base commune des fournisseurs de widget. Les sous-classes ne diffèrent que par la taille
 * par défaut déclarée dans leur fichier `appwidget-provider`.
 */
abstract class BaseWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = UniversalWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val repository = context.widgetEntryPoint().widgetRepository()
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { repository.delete(it) }
            } finally {
                pending?.finish()
            }
        }
    }
}

class SmallWidgetReceiver : BaseWidgetReceiver()

class MediumWidgetReceiver : BaseWidgetReceiver()

class WideWidgetReceiver : BaseWidgetReceiver()

class BarWidgetReceiver : BaseWidgetReceiver()

class LargeWidgetReceiver : BaseWidgetReceiver()
