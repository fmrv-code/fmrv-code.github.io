package fmrv.widgetstudio.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
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
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
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
import fmrv.widgetstudio.domain.model.BackgroundStyle
import fmrv.widgetstudio.domain.model.NoteSummary
import fmrv.widgetstudio.domain.model.NotesSnapshot
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetLayout
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
        val density = context.resources.displayMetrics.density
        val iconDp = if (config?.contentType == WidgetContentType.APP_GRID) GRID_ICON_BITMAP_DP else ICON_BITMAP_DP
        val iconPx = (iconDp * density).roundToInt()
        val packages = when (config?.contentType) {
            WidgetContentType.APP_SHORTCUT, WidgetContentType.SEARCH_BAR -> listOfNotNull(config.targetPackage)
            WidgetContentType.APP_GRID -> config.packages.take(WidgetConfig.MAX_GRID_APPS)
            else -> emptyList()
        }
        // Les icônes sont réduites pour rester sous la limite mémoire des RemoteViews (≈ 1 Mo).
        val icons = packages.distinct()
            .mapNotNull { pkg -> entryPoint.iconProvider().icon(pkg, iconPx)?.let { pkg to it } }
            .toMap()
        val device = if (config?.contentType == WidgetContentType.DEVICE_STATUS) DeviceStatsReader.read(context) else null

        val notes = config?.contentType
            ?.takeIf { it.isList }
            ?.let { type -> entryPoint.listSources().forType(type)?.let { loadNotes(context, it) } }

        provideContent { UniversalWidgetContent(appWidgetId, config, icons, notes, device) }
    }

    /** Lit le cache (sans réseau). Si le cache est vide et ancien, déclenche une actualisation en arrière-plan. */
    private suspend fun loadNotes(context: Context, repository: fmrv.widgetstudio.domain.repository.ItemsRepository): NotesSnapshot {
        val snapshot = repository.snapshot()
        val stale = System.currentTimeMillis() - snapshot.refreshedAtMillis > EMPTY_CACHE_RETRY_MS
        if (snapshot.connected && snapshot.notes.isEmpty() && stale) NotesRefreshWorker.enqueueNow(context)
        return snapshot
    }

    private companion object {
        const val ICON_BITMAP_DP = 72
        const val GRID_ICON_BITMAP_DP = 44
        const val EMPTY_CACHE_RETRY_MS = 5 * 60 * 1000L
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
private fun UniversalWidgetContent(
    appWidgetId: Int,
    config: WidgetConfig?,
    icons: Map<String, Bitmap>,
    notes: NotesSnapshot?,
    device: DeviceSnapshot?,
) {
    val context = LocalContext.current
    if (config == null) {
        UnconfiguredContent(context, appWidgetId)
        return
    }

    val size = LocalSize.current
    val widthDp = size.width.value
    val heightDp = size.height.value
    val icon = config.targetPackage?.let { icons[it] }

    val launchIntent = launchIntentFor(context, config.targetPackage)
    // Un raccourci dont l'application a disparu ouvre la configuration plutôt que de ne rien faire.
    val clickIntent = when (config.contentType) {
        WidgetContentType.APP_SHORTCUT, WidgetContentType.SEARCH_BAR -> launchIntent ?: configureIntent(context, appWidgetId)
        WidgetContentType.CLOCK_DATE -> launchIntent
        // Ces widgets ont leurs propres zones cliquables (icônes, jauges, éléments de liste).
        WidgetContentType.APP_GRID,
        WidgetContentType.DEVICE_STATUS,
        WidgetContentType.WEATHER_CLOCK,
        WidgetContentType.NOTES_LIST,
        WidgetContentType.NEWS_FEED,
        WidgetContentType.MAIL_INBOX,
        -> null
    }
    val clickModifier = if (clickIntent != null) {
        GlanceModifier.clickable(actionStartActivity(clickIntent))
    } else {
        GlanceModifier
    }

    ShapedBox(
        config = config,
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground().then(clickModifier),
    ) {
        when (config.contentType) {
            WidgetContentType.APP_SHORTCUT -> if (config.layout == WidgetLayout.PILL) {
                PillContent(context, config, icon, heightDp, search = false)
            } else {
                AppShortcutContent(config, icon, widthDp, heightDp)
            }
            WidgetContentType.SEARCH_BAR -> PillContent(context, config, icon, heightDp, search = true)
            WidgetContentType.APP_GRID -> GridContent(context, config, icons, widthDp, heightDp)
            WidgetContentType.DEVICE_STATUS -> DeviceContent(context, config, device, widthDp, heightDp)
            WidgetContentType.WEATHER_CLOCK -> Unit
            WidgetContentType.CLOCK_DATE -> ClockContent(context, config, widthDp, heightDp)
            WidgetContentType.NOTES_LIST, WidgetContentType.NEWS_FEED, WidgetContentType.MAIL_INBOX ->
                NotesContent(context, config, notes, widthDp, heightDp)
        }
    }
}

@Composable
private fun ShapedBox(
    config: WidgetConfig,
    modifier: GlanceModifier,
    content: @Composable () -> Unit,
) {
    val (start, _) = WidgetBackground.colors(config)
    val radius = WidgetStyle.cornerRadiusDp(config.shape).dp
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val fill = if (config.background == BackgroundStyle.SOLID) {
            GlanceModifier.background(ColorProvider(Color(start)))
        } else {
            // Dégradé / verre : petit bitmap étiré ; les coins sont découpés par cornerRadius.
            GlanceModifier.background(ImageProvider(WidgetBackground.gradientBitmap(config)))
        }
        Box(
            modifier = modifier.then(fill).cornerRadius(radius),
            contentAlignment = Alignment.Center,
            content = content,
        )
    } else {
        // Android 11 : cornerRadius n'est pas supporté, on teinte une forme vectorielle étirée (couleur de départ).
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(fallbackShapeRes(config.shape)),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(ColorProvider(Color(start))),
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

internal fun glanceFont(font: WidgetFont): FontFamily = when (font) {
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

@Composable
private fun NotesContent(
    context: Context,
    config: WidgetConfig,
    snapshot: NotesSnapshot?,
    widthDp: Float,
    heightDp: Float,
) {
    val textColor = Color(config.textColor)
    val titleSp = WidgetStyle.notesTitleSizeSp(widthDp, heightDp)
    val font = glanceFont(config.font)
    val type = config.contentType
    val headerIntent = ListLinks.headerIntent(context, type)

    Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (WidgetStyle.showNotesHeader(heightDp)) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val titleModifier = GlanceModifier.defaultWeight()
                Text(
                    text = config.name.ifBlank { context.getString(ListLinks.defaultTitle(type)) },
                    maxLines = 1,
                    modifier = if (headerIntent != null) {
                        titleModifier.clickable(actionStartActivity(headerIntent))
                    } else {
                        titleModifier
                    },
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = (titleSp + 2f).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = font,
                    ),
                )
                Image(
                    provider = ImageProvider(R.drawable.ic_refresh),
                    contentDescription = context.getString(R.string.notes_refresh),
                    modifier = GlanceModifier.size(36.dp).padding(8.dp).clickable(actionRunCallback<RefreshNotesAction>()),
                    colorFilter = ColorFilter.tint(ColorProvider(textColor)),
                )
            }
        }

        val notes = snapshot?.notes.orEmpty()
        when {
            snapshot == null || !snapshot.connected ->
                NotesHint(context.getString(ListLinks.notConnectedHint(type)), textColor, font, titleSp)
            notes.isEmpty() -> NotesHint(context.getString(R.string.notes_empty), textColor, font, titleSp)
            else -> {
                val deepLink = type == WidgetContentType.NOTES_LIST && JoplinLinks.supportsOpenNote(context)
                val showSnippet = WidgetStyle.showNoteSnippet(heightDp)
                val snippetSp = WidgetStyle.notesSnippetSizeSp(titleSp)
                val now = System.currentTimeMillis()
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(notes, itemId = { it.id.hashCode().toLong() }) { note ->
                        NoteRow(context, type, note, deepLink, showSnippet, now, textColor, font, titleSp, snippetSp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(
    context: Context,
    type: WidgetContentType,
    note: NoteSummary,
    deepLink: Boolean,
    showSnippet: Boolean,
    nowMillis: Long,
    textColor: Color,
    font: FontFamily,
    titleSp: Float,
    snippetSp: Float,
) {
    val intent = ListLinks.itemIntent(context, type, note, deepLink)
    val base = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)
    Column(modifier = if (intent != null) base.clickable(actionStartActivity(intent)) else base) {
        Text(
            text = note.title.ifBlank { context.getString(R.string.notes_untitled) },
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(textColor),
                fontSize = titleSp.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = font,
            ),
        )
        if (showSnippet) {
            val whenText = DateUtils.getRelativeTimeSpanString(
                note.updatedMillis, nowMillis, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE,
            ).toString()
            Text(
                text = if (note.snippet.isBlank()) whenText else "$whenText · ${note.snippet}",
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(textColor.copy(alpha = 0.72f)),
                    fontSize = snippetSp.sp,
                    fontFamily = font,
                ),
            )
        }
    }
}

@Composable
private fun NotesHint(text: String, textColor: Color, font: FontFamily, sizeSp: Float) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(textColor.copy(alpha = 0.85f)),
                fontSize = sizeSp.sp,
                fontFamily = font,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
