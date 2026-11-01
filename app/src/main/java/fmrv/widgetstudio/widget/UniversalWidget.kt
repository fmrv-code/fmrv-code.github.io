package fmrv.widgetstudio.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
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
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetShape
import kotlin.math.roundToInt

/**
 * Widget unique et générique : chaque instance posée sur l'écran d'accueil lit sa propre
 * [WidgetConfig] (clé : identifiant système du widget) et se rend en conséquence.
 */
class UniversalWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val entryPoint = context.widgetEntryPoint()
        val config = entryPoint.widgetRepository().get(appWidgetId)
        val iconPx = (ICON_BITMAP_DP * context.resources.displayMetrics.density).roundToInt()
        val icon = config
            ?.takeIf { it.contentType == WidgetContentType.APP_SHORTCUT }
            ?.targetPackage
            ?.let { entryPoint.iconProvider().icon(it, iconPx) }

        provideContent { UniversalWidgetContent(appWidgetId, config, icon) }
    }

    private companion object {
        const val ICON_BITMAP_DP = 72
    }
}

fun configureIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, WidgetConfigActivity::class.java)
        .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        // Les extras ne distinguent pas les PendingIntent : on rend l'intent unique par widget.
        .setData(Uri.parse("widgetstudio://configure/$appWidgetId"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@Composable
private fun UniversalWidgetContent(appWidgetId: Int, config: WidgetConfig?, icon: Bitmap?) {
    val context = LocalContext.current
    if (config == null) {
        UnconfiguredContent(context, appWidgetId)
        return
    }

    val size = LocalSize.current
    val widthDp = size.width.value
    val heightDp = size.height.value
    val background = Color(config.backgroundColor).copy(alpha = config.opacity)

    val launchIntent = config.targetPackage
        ?.let { context.packageManager.getLaunchIntentForPackage(it) }
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // Un raccourci dont l'application a disparu ouvre la configuration plutôt que de ne rien faire.
    val clickIntent = when (config.contentType) {
        WidgetContentType.APP_SHORTCUT -> launchIntent ?: configureIntent(context, appWidgetId)
        WidgetContentType.CLOCK_DATE -> launchIntent
    }
    val clickModifier = if (clickIntent != null) {
        GlanceModifier.clickable(actionStartActivity(clickIntent))
    } else {
        GlanceModifier
    }

    ShapedBox(
        shape = config.shape,
        color = background,
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground().then(clickModifier),
    ) {
        when (config.contentType) {
            WidgetContentType.APP_SHORTCUT -> AppShortcutContent(config, icon, widthDp, heightDp)
            WidgetContentType.CLOCK_DATE -> ClockContent(context, config, widthDp, heightDp)
        }
    }
}

@Composable
private fun ShapedBox(
    shape: WidgetShape,
    color: Color,
    modifier: GlanceModifier,
    content: @Composable () -> Unit,
) {
    val colorProvider = ColorProvider(color)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Box(
            modifier = modifier
                .background(colorProvider)
                .cornerRadius(WidgetStyle.cornerRadiusDp(shape).dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
    } else {
        // Android 11 : cornerRadius n'est pas supporté, on teinte une forme vectorielle étirée.
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(fallbackShapeRes(shape)),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(colorProvider),
            )
            content()
        }
    }
}

private fun fallbackShapeRes(shape: WidgetShape): Int = when (shape) {
    WidgetShape.SQUARE -> R.drawable.widget_shape_square
    WidgetShape.ROUNDED -> R.drawable.widget_shape_rounded
    WidgetShape.PILL -> R.drawable.widget_shape_pill
}

private fun glanceFont(font: WidgetFont): FontFamily = when (font) {
    WidgetFont.SANS -> FontFamily.SansSerif
    WidgetFont.SERIF -> FontFamily.Serif
    WidgetFont.MONO -> FontFamily.Monospace
    WidgetFont.CURSIVE -> FontFamily.Cursive
}

@Composable
private fun AppShortcutContent(config: WidgetConfig, icon: Bitmap?, widthDp: Float, heightDp: Float) {
    val labelVisible = WidgetStyle.isLabelVisible(config.showLabel, heightDp)
    val iconSize = WidgetStyle.iconSizeDp(widthDp, heightDp, labelVisible).dp
    val label = config.targetLabel ?: config.name

    Column(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = label,
                modifier = GlanceModifier.size(iconSize),
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_placeholder),
                contentDescription = label,
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(ColorProvider(Color(config.textColor))),
            )
        }
        if (labelVisible) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(Color(config.textColor)),
                    fontSize = WidgetStyle.labelSizeSp(widthDp, heightDp).sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = glanceFont(config.font),
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun ClockContent(context: Context, config: WidgetConfig, widthDp: Float, heightDp: Float) {
    // Glance n'a pas de TextClock : on embarque un RemoteViews, mis à jour par le système sans réveiller l'app.
    val timeSp = WidgetStyle.timeSizeSp(widthDp, heightDp, config.showDate)
    val timePattern = WidgetStyle.timePattern(config.use24Hour)
    val datePattern = WidgetStyle.datePattern(widthDp)

    val views = RemoteViews(context.packageName, clockLayout(config.font)).apply {
        setTextColor(R.id.clock_time, config.textColor)
        setTextViewTextSize(R.id.clock_time, TypedValue.COMPLEX_UNIT_SP, timeSp)
        setCharSequence(R.id.clock_time, "setFormat12Hour", timePattern)
        setCharSequence(R.id.clock_time, "setFormat24Hour", timePattern)

        setViewVisibility(R.id.clock_date, if (config.showDate) View.VISIBLE else View.GONE)
        setTextColor(R.id.clock_date, config.textColor)
        setTextViewTextSize(R.id.clock_date, TypedValue.COMPLEX_UNIT_SP, WidgetStyle.dateSizeSp(timeSp))
        setCharSequence(R.id.clock_date, "setFormat12Hour", datePattern)
        setCharSequence(R.id.clock_date, "setFormat24Hour", datePattern)
    }
    AndroidRemoteViews(remoteViews = views, modifier = GlanceModifier.fillMaxSize())
}

private fun clockLayout(font: WidgetFont): Int = when (font) {
    WidgetFont.SANS -> R.layout.widget_clock_sans
    WidgetFont.SERIF -> R.layout.widget_clock_serif
    WidgetFont.MONO -> R.layout.widget_clock_mono
    WidgetFont.CURSIVE -> R.layout.widget_clock_cursive
}

@Composable
private fun UnconfiguredContent(context: Context, appWidgetId: Int) {
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
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
