package fmrv.widgetstudio.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.WidgetInstance
import fmrv.widgetstudio.kinds.Kinds
import fmrv.widgetstudio.kinds.LoadEnv
import fmrv.widgetstudio.kinds.PreparedWidget
import fmrv.widgetstudio.kinds.RenderEnv
import fmrv.widgetstudio.kinds.WidgetKind
import fmrv.widgetstudio.kinds.prepare
import fmrv.widgetstudio.render.imageRequests
import fmrv.widgetstudio.render.loadAll
import java.time.ZonedDateTime
import java.util.Locale

/** Environnement de rendu d'une instance : paramètres complétés par les valeurs par défaut de son type. */
fun renderEnv(context: Context, instance: WidgetInstance, kind: WidgetKind<*>, widthDp: Float, heightDp: Float): RenderEnv =
    RenderEnv(
        widthDp = widthDp,
        heightDp = heightDp,
        style = instance.style,
        params = instance.params.withDefaults(kind.defaultParams),
        name = instance.name.ifBlank { context.getString(kind.title) },
        now = ZonedDateTime.now(),
        locale = Locale.getDefault(),
        strings = { context.getString(it) },
    )

/**
 * Widget unique et générique : chaque instance posée sur l'écran d'accueil lit sa propre configuration
 * (clé : identifiant système du widget), laisse son type produire un arbre d'éléments, puis le dessine.
 */
class UniversalWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val entry = context.widgetEntryPoint()
        val instance = entry.widgetRepository().get(appWidgetId)
        val kind = instance?.let { Kinds.byId(it.kindId) }
        if (instance == null || kind == null) {
            provideContent { UnconfiguredContent(appWidgetId) }
            return
        }

        val prepared = kind.prepare(LoadEnv(context, entry, instance))
        // Un premier dessin « à blanc » (grande taille) révèle les images nécessaires : icônes d'applications, photos.
        val probe = prepared.render(renderEnv(context, instance, kind, PROBE_DP, PROBE_DP))
        val assets = entry.assetProvider().loadAll(probe.imageRequests(), context.resources.displayMetrics.density)

        provideContent {
            val size = LocalSize.current
            val node = prepared.render(renderEnv(context, instance, kind, size.width.value, size.height.value))
            GlanceFrame(instance.style) { RenderNode(node, GlanceEnv(context, appWidgetId, assets)) }
        }
    }

    private companion object {
        const val PROBE_DP = 320f
    }
}

@Composable
private fun UnconfiguredContent(appWidgetId: Int) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(Color(0xFF3C3C3E)))
            .cornerRadius(28.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(configureIntent(context, appWidgetId))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = context.getString(R.string.widget_unconfigured),
            style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, textAlign = TextAlign.Center),
        )
    }
}
