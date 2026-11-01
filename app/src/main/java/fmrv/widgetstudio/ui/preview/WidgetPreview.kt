package fmrv.widgetstudio.ui.preview

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fmrv.widgetstudio.domain.model.WidgetInstance
import fmrv.widgetstudio.domain.model.WidgetShape
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.domain.model.WidgetStyle
import fmrv.widgetstudio.kinds.Kinds
import fmrv.widgetstudio.kinds.LoadEnv
import fmrv.widgetstudio.kinds.prepare
import fmrv.widgetstudio.render.ImgSource
import fmrv.widgetstudio.render.Colors
import fmrv.widgetstudio.render.Node
import fmrv.widgetstudio.render.imageRequests
import fmrv.widgetstudio.render.loadAll
import fmrv.widgetstudio.widget.renderEnv
import fmrv.widgetstudio.widget.widgetEntryPoint

private class Ready(val node: Node, val images: Map<ImgSource, ImageBitmap>)

private suspend fun loadPreview(context: Context, instance: WidgetInstance, size: WidgetSizeOption): Ready? {
    val kind = Kinds.byId(instance.kindId) ?: return null
    val entry = context.widgetEntryPoint()
    val prepared = kind.prepare(LoadEnv(context, entry, instance))
    val node = prepared.render(renderEnv(context, instance, kind, size.widthDp.toFloat(), size.heightDp.toFloat()))
    val images = entry.assetProvider().loadAll(node.imageRequests(), context.resources.displayMetrics.density)
    return Ready(node, images.mapValues { it.value.asImageBitmap() })
}

/** Rayon de coin du widget selon sa forme (identique au rendu Glance). */
fun cornerDp(shape: WidgetShape): Float = when (shape) {
    WidgetShape.SQUARE -> 8f
    WidgetShape.ROUNDED -> 28f
    WidgetShape.PILL -> 100f
}

/** Fond du widget (uni, dégradé, verre) découpé selon sa forme. */
@Composable
fun WidgetFrame(style: WidgetStyle, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val (start, end) = Colors.background(style)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerDp(style.shape).dp))
            .background(Brush.linearGradient(listOf(Color(start), Color(end)))),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Aperçu fidèle d'un widget : le même arbre que celui dessiné sur l'écran d'accueil, à sa taille réelle
 * puis réduit par [scale]. Les images et données (icônes, flux, appareil) sont chargées comme pour le vrai widget.
 */
@Composable
fun WidgetPreview(
    instance: WidgetInstance,
    size: WidgetSizeOption,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val context = LocalContext.current
    val ready by produceState<Ready?>(null, instance, size) { value = loadPreview(context, instance, size) }
    val w = size.widthDp.toFloat()
    val h = size.heightDp.toFloat()

    // La boîte intérieure garde la taille réelle (mise en page identique) ; seule sa représentation est réduite.
    Box(modifier.size((w * scale).dp, (h * scale).dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .requiredSize(w.dp, h.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin.Center
                },
        ) {
            ready?.let { r ->
                WidgetFrame(instance.style, Modifier.fillMaxSize()) { NodeView(r.node, r.images) }
            }
        }
    }
}
