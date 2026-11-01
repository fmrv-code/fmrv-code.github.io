package fmrv.widgetstudio.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.BackgroundStyle
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetShape
import fmrv.widgetstudio.domain.model.WidgetStyle
import fmrv.widgetstudio.render.AnalogClock
import fmrv.widgetstudio.render.AnalogFace
import fmrv.widgetstudio.render.Click
import fmrv.widgetstudio.render.Colors
import fmrv.widgetstudio.render.HAlign
import fmrv.widgetstudio.render.Img
import fmrv.widgetstudio.render.ImgScale
import fmrv.widgetstudio.render.ImgSource
import fmrv.widgetstudio.render.Mod
import fmrv.widgetstudio.render.Node
import fmrv.widgetstudio.render.ScrollList
import fmrv.widgetstudio.render.Space
import fmrv.widgetstudio.render.TextWeight
import fmrv.widgetstudio.render.VAlign
import fmrv.widgetstudio.render.Box as BoxNode
import fmrv.widgetstudio.render.Column as ColumnNode
import fmrv.widgetstudio.render.ClockText as ClockTextNode
import fmrv.widgetstudio.render.Row as RowNode
import fmrv.widgetstudio.render.Text as TextNode
import fmrv.widgetstudio.render.TextAlign as NodeTextAlign
import androidx.glance.text.Text as GlanceText
import androidx.glance.text.TextAlign as GlanceTextAlign

/** Ce dont le dessin Glance a besoin en plus de l'arbre. */
class GlanceEnv(
    val context: Context,
    val appWidgetId: Int,
    val assets: Map<ImgSource, Bitmap>,
)

val ClickIdKey = ActionParameters.Key<String>("click_id")
val ClickArgKey = ActionParameters.Key<String>("click_arg")

/** Dessine l'arbre d'éléments avec Glance. */
@Composable
fun RenderNode(node: Node, env: GlanceEnv, weigher: (((GlanceModifier) -> GlanceModifier)?) = null) {
    when (node) {
        is BoxNode -> Box(
            modifier = glanceModifier(node.mod, env, weigher),
            contentAlignment = Alignment(node.horizontal.toGlance(), node.vertical.toGlance()),
        ) {
            node.children.forEach { RenderNode(it, env) }
        }
        is ColumnNode -> Column(
            modifier = glanceModifier(node.mod, env, weigher),
            verticalAlignment = node.vertical.toGlance(),
            horizontalAlignment = node.horizontal.toGlance(),
        ) {
            val scope = this
            node.children.forEach { child -> RenderNode(child, env) { m -> with(scope) { m.defaultWeight() } } }
        }
        is RowNode -> Row(
            modifier = glanceModifier(node.mod, env, weigher),
            verticalAlignment = node.vertical.toGlance(),
            horizontalAlignment = node.horizontal.toGlance(),
        ) {
            val scope = this
            node.children.forEach { child -> RenderNode(child, env) { m -> with(scope) { m.defaultWeight() } } }
        }
        is TextNode -> GlanceText(
            text = node.text,
            modifier = glanceModifier(node.mod, env, weigher),
            maxLines = node.maxLines,
            style = TextStyle(
                color = ColorProvider(Color(node.color)),
                fontSize = node.sizeSp.sp,
                fontWeight = when (node.weight) {
                    TextWeight.NORMAL -> FontWeight.Normal
                    TextWeight.MEDIUM -> FontWeight.Medium
                    TextWeight.BOLD -> FontWeight.Bold
                },
                fontFamily = glanceFont(node.font),
                textAlign = when (node.align) {
                    NodeTextAlign.START -> GlanceTextAlign.Start
                    NodeTextAlign.CENTER -> GlanceTextAlign.Center
                    NodeTextAlign.END -> GlanceTextAlign.End
                },
            ),
        )
        is Img -> RenderImage(node, env, weigher)
        is Space -> Spacer(modifier = GlanceModifier.width(node.widthDp.dp).height(node.heightDp.dp))
        is ClockTextNode -> AndroidRemoteViews(
            remoteViews = clockTextViews(env.context, node),
            modifier = glanceModifier(node.mod, env, weigher),
        )
        is AnalogClock -> AndroidRemoteViews(
            remoteViews = RemoteViews(env.context.packageName, analogLayout(node.face)),
            modifier = glanceModifier(node.mod, env, weigher).width(node.sizeDp.dp).height(node.sizeDp.dp),
        )
        is ScrollList -> LazyColumn(modifier = glanceModifier(node.mod, env, weigher)) {
            items(node.items.size) { index -> RenderNode(node.items[index], env) }
        }
    }
}

@Composable
private fun RenderImage(node: Img, env: GlanceEnv, weigher: (((GlanceModifier) -> GlanceModifier)?)) {
    val modifier = glanceModifier(node.mod, env, weigher).width(node.widthDp.dp).height(node.heightDp.dp)
    val bitmap = env.assets[node.source]
    val provider = when (val source = node.source) {
        is ImgSource.Res -> ImageProvider(source.id)
        else -> bitmap?.let { ImageProvider(it) }
    }
    if (provider != null) {
        Image(
            provider = provider,
            contentDescription = node.description,
            modifier = modifier,
            contentScale = when (node.scale) {
                ImgScale.FIT -> ContentScale.Fit
                ImgScale.CROP -> ContentScale.Crop
                ImgScale.FILL -> ContentScale.FillBounds
            },
            colorFilter = node.tint?.let { ColorFilter.tint(ColorProvider(Color(it))) },
        )
    } else {
        // Image introuvable (application désinstallée, photo supprimée) : pictogramme neutre.
        Image(
            provider = ImageProvider(R.drawable.ic_widget_placeholder),
            contentDescription = node.description,
            modifier = modifier,
            colorFilter = ColorFilter.tint(ColorProvider(Color(0xFF9E9E9E))),
        )
    }
}

private fun glanceModifier(mod: Mod, env: GlanceEnv, weigher: (((GlanceModifier) -> GlanceModifier)?)): GlanceModifier {
    var m: GlanceModifier = GlanceModifier
    if (mod.fillWidth && mod.fillHeight) m = m.fillMaxSize()
    else if (mod.fillWidth) m = m.fillMaxWidth()
    else if (mod.fillHeight) m = m.fillMaxHeight()
    mod.width?.let { m = m.width(it.dp) }
    mod.height?.let { m = m.height(it.dp) }
    if (mod.weight > 0f && weigher != null) m = weigher(m)
    mod.background?.let { m = m.background(ColorProvider(Color(it))) }
    if (mod.corner > 0f) m = m.cornerRadius(mod.corner.dp)
    mod.click?.let { click -> clickAction(click, env)?.let { m = m.clickable(it) } }
    if (mod.pad != fmrv.widgetstudio.render.Pad.None) {
        m = m.padding(start = mod.pad.left.dp, top = mod.pad.top.dp, end = mod.pad.right.dp, bottom = mod.pad.bottom.dp)
    }
    return m
}

private fun HAlign.toGlance(): Alignment.Horizontal = when (this) {
    HAlign.START -> Alignment.Start
    HAlign.CENTER -> Alignment.CenterHorizontally
    HAlign.END -> Alignment.End
}

private fun VAlign.toGlance(): Alignment.Vertical = when (this) {
    VAlign.TOP -> Alignment.Top
    VAlign.CENTER -> Alignment.CenterVertically
    VAlign.BOTTOM -> Alignment.Bottom
}

internal fun glanceFont(font: WidgetFont): FontFamily = when (font) {
    WidgetFont.SANS -> FontFamily.SansSerif
    WidgetFont.SERIF -> FontFamily.Serif
    WidgetFont.MONO -> FontFamily.Monospace
    WidgetFont.CURSIVE -> FontFamily.Cursive
}

fun configureIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, WidgetConfigActivity::class.java)
        .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        // Les extras ne distinguent pas les PendingIntent : on rend l'intent unique par widget.
        .setData(Uri.parse("widgetstudio://configure/$appWidgetId"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun launchIntent(context: Context, packageName: String): Intent? =
    context.packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** Traduit une [Click] de l'arbre en action Glance. */
private fun clickAction(click: Click, env: GlanceEnv): Action? = when (click) {
    is Click.LaunchApp ->
        // Une application désinstallée ouvre la configuration plutôt que de ne rien faire.
        actionStartActivity(launchIntent(env.context, click.packageName) ?: configureIntent(env.context, env.appWidgetId))
    is Click.OpenUri -> actionStartActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(click.uri)).apply { click.packageName?.let { setPackage(it) } }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    is Click.SystemAction -> actionStartActivity(Intent(click.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    is Click.Callback -> actionRunCallback<DispatchActionCallback>(
        actionParametersOf(ClickIdKey to click.id, ClickArgKey to click.argument),
    )
    Click.Refresh -> actionRunCallback<DispatchActionCallback>(actionParametersOf(ClickIdKey to "refresh", ClickArgKey to ""))
    Click.Configure -> actionStartActivity(configureIntent(env.context, env.appWidgetId))
}

private fun clockTextViews(context: Context, node: ClockTextNode): RemoteViews =
    RemoteViews(context.packageName, clockTextLayout(node.font, node.bold)).apply {
        val id = R.id.clock_text
        setTextColor(id, node.color)
        setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, node.sizeSp)
        // Les deux formats identiques : l'affichage 12/24 h suit le réglage de l'utilisateur dans le motif.
        setCharSequence(id, "setFormat12Hour", node.pattern)
        setCharSequence(id, "setFormat24Hour", node.pattern)
        setInt(
            id, "setGravity",
            when (node.align) {
                NodeTextAlign.START -> Gravity.START
                NodeTextAlign.CENTER -> Gravity.CENTER_HORIZONTAL
                NodeTextAlign.END -> Gravity.END
            },
        )
        node.timeZone?.let { setString(id, "setTimeZone", it) }
    }

private fun clockTextLayout(font: WidgetFont, bold: Boolean): Int = when (font) {
    WidgetFont.SANS -> if (bold) R.layout.clock_text_sans_bold else R.layout.clock_text_sans
    WidgetFont.SERIF -> if (bold) R.layout.clock_text_serif_bold else R.layout.clock_text_serif
    WidgetFont.MONO -> if (bold) R.layout.clock_text_mono_bold else R.layout.clock_text_mono
    WidgetFont.CURSIVE -> if (bold) R.layout.clock_text_cursive_bold else R.layout.clock_text_cursive
}

private fun analogLayout(face: AnalogFace): Int = when (face) {
    AnalogFace.LIGHT -> R.layout.clock_analog_light
    AnalogFace.DARK -> R.layout.clock_analog_dark
    AnalogFace.CLASSIC -> R.layout.clock_analog_classic
    AnalogFace.BOLD -> R.layout.clock_analog_bold
}

private fun cornerFor(shape: WidgetShape): Float = when (shape) {
    WidgetShape.SQUARE -> 8f
    WidgetShape.ROUNDED -> 28f
    WidgetShape.PILL -> 100f
}

private fun fallbackShapeRes(shape: WidgetShape): Int = when (shape) {
    WidgetShape.SQUARE -> R.drawable.widget_shape_square
    WidgetShape.ROUNDED -> R.drawable.widget_shape_rounded
    WidgetShape.PILL -> R.drawable.widget_shape_pill
}

/** Fond du widget (uni, dégradé, verre) et découpe selon la forme choisie. */
@Composable
fun GlanceFrame(style: WidgetStyle, content: @Composable () -> Unit) {
    val (start, _) = Colors.background(style)
    val radius = cornerFor(style.shape).dp
    val base = GlanceModifier.fillMaxSize().appWidgetBackground()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val fill = if (style.background == BackgroundStyle.SOLID) {
            GlanceModifier.background(ColorProvider(Color(start)))
        } else {
            // Dégradé / verre : petit bitmap étiré ; les coins sont découpés par cornerRadius.
            GlanceModifier.background(ImageProvider(gradientBitmap(style)))
        }
        Box(modifier = base.then(fill).cornerRadius(radius), contentAlignment = Alignment.Center, content = content)
    } else {
        // Android 11 : cornerRadius n'est pas supporté, on teinte une forme vectorielle étirée.
        Box(modifier = base, contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(fallbackShapeRes(style.shape)),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(ColorProvider(Color(start))),
            )
            content()
        }
    }
}
