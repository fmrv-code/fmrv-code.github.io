package fmrv.widgetstudio.ui.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.render.AnalogClock
import fmrv.widgetstudio.render.AnalogFace
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
import fmrv.widgetstudio.render.ClockText as ClockTextNode
import fmrv.widgetstudio.render.Column as ColumnNode
import fmrv.widgetstudio.render.Row as RowNode
import fmrv.widgetstudio.render.Text as TextNode
import fmrv.widgetstudio.render.TextAlign as NodeTextAlign
import androidx.compose.ui.text.style.TextAlign as ComposeTextAlign
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/** Dessine l'arbre d'éléments avec Compose : même description que le widget réel, donc aperçu fidèle. */
@Composable
fun NodeView(node: Node, assets: Map<ImgSource, ImageBitmap>, weigher: (((Modifier) -> Modifier)?) = null) {
    when (node) {
        is BoxNode -> Box(
            modifier = node.mod.toModifier(weigher),
            contentAlignment = BiasAlignment(node.horizontal.bias(), node.vertical.bias()),
        ) {
            node.children.forEach { NodeView(it, assets) }
        }
        is ColumnNode -> Column(
            modifier = node.mod.toModifier(weigher),
            verticalArrangement = node.vertical.arrangement(),
            horizontalAlignment = node.horizontal.alignment(),
        ) {
            val scope = this
            node.children.forEach { child -> NodeView(child, assets) { m -> with(scope) { m.weight(1f) } } }
        }
        is RowNode -> Row(
            modifier = node.mod.toModifier(weigher),
            horizontalArrangement = node.horizontal.arrangement(),
            verticalAlignment = node.vertical.alignment(),
        ) {
            val scope = this
            node.children.forEach { child -> NodeView(child, assets) { m -> with(scope) { m.weight(1f) } } }
        }
        is TextNode -> Text(
            text = node.text,
            color = Color(node.color),
            fontSize = node.sizeSp.sp,
            fontWeight = node.weight.compose(),
            fontFamily = node.font.compose(),
            textAlign = node.align.compose(),
            maxLines = node.maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = node.mod.toModifier(weigher),
        )
        is Img -> ImageView(node, assets, weigher)
        is Space -> Spacer(Modifier.width(node.widthDp.dp).height(node.heightDp.dp))
        is ClockTextNode -> ClockTextView(node, node.mod.toModifier(weigher))
        is AnalogClock -> AnalogClockView(node, node.mod.toModifier(weigher))
        is ScrollList -> Column(node.mod.toModifier(weigher)) {
            node.items.forEach { NodeView(it, assets) }
        }
    }
}

private fun Mod.toModifier(weigher: (((Modifier) -> Modifier)?)): Modifier {
    var m: Modifier = Modifier
    if (fillWidth && fillHeight) m = m.fillMaxSize()
    else if (fillWidth) m = m.fillMaxWidth()
    else if (fillHeight) m = m.fillMaxHeight()
    width?.let { m = m.width(it.dp) }
    height?.let { m = m.height(it.dp) }
    if (weight > 0f && weigher != null) m = weigher(m)
    if (corner > 0f) m = m.clip(RoundedCornerShape(corner.dp))
    background?.let { m = m.background(Color(it)) }
    if (pad.left != 0f || pad.top != 0f || pad.right != 0f || pad.bottom != 0f) {
        m = m.padding(start = pad.left.dp, top = pad.top.dp, end = pad.right.dp, bottom = pad.bottom.dp)
    }
    return m
}

@Composable
private fun ImageView(node: Img, assets: Map<ImgSource, ImageBitmap>, weigher: (((Modifier) -> Modifier)?)) {
    val modifier = node.mod.toModifier(weigher).size(node.widthDp.dp, node.heightDp.dp)
    val tint = node.tint?.let { ColorFilter.tint(Color(it)) }
    val scale = when (node.scale) {
        ImgScale.FIT -> ContentScale.Fit
        ImgScale.CROP -> ContentScale.Crop
        ImgScale.FILL -> ContentScale.FillBounds
    }
    when (val source = node.source) {
        is ImgSource.Res -> Image(painterResource(source.id), node.description, modifier, contentScale = scale, colorFilter = tint)
        else -> {
            val bitmap = assets[source]
            if (bitmap != null) {
                Image(bitmap, node.description, modifier, contentScale = scale, colorFilter = tint)
            } else {
                Box(modifier.clip(RoundedCornerShape((node.widthDp * 0.24f).dp)).background(Color(0x449E9E9E)))
            }
        }
    }
}

@Composable
private fun rememberNowMillis(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    return now
}

/** Horloge animée dans l'aperçu : même motif et même fuseau que le TextClock du widget. */
@Composable
private fun ClockTextView(node: ClockTextNode, modifier: Modifier) {
    val now = rememberNowMillis()
    val zone = remember(node.timeZone) {
        try {
            node.timeZone?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        } catch (e: java.time.DateTimeException) {
            ZoneId.systemDefault()
        }
    }
    val formatter = remember(node.pattern) { DateTimeFormatter.ofPattern(node.pattern, Locale.getDefault()) }
    Text(
        text = formatter.format(Instant.ofEpochMilli(now).atZone(zone)),
        color = Color(node.color),
        fontSize = node.sizeSp.sp,
        fontWeight = if (node.bold) FontWeight.Bold else FontWeight.Normal,
        fontFamily = node.font.compose(),
        textAlign = node.align.compose(),
        maxLines = 1,
        modifier = modifier,
    )
}

private data class FaceColors(val dial: Color, val ring: Color?, val tick: Color, val hand: Color, val bold: Boolean, val quartersOnly: Boolean)

private fun AnalogFace.colors(): FaceColors = when (this) {
    AnalogFace.LIGHT -> FaceColors(Color(0xFFF5F5F7), null, Color(0xFF1C1C1E), Color(0xFF1C1C1E), bold = false, quartersOnly = false)
    AnalogFace.DARK -> FaceColors(Color(0xFF1C1C1E), null, Color(0xFFE8E8EA), Color(0xFFF5F5F7), bold = false, quartersOnly = false)
    AnalogFace.CLASSIC -> FaceColors(Color.White, Color(0xFF1C1C1E), Color(0xFF1C1C1E), Color(0xFF1C1C1E), bold = false, quartersOnly = false)
    AnalogFace.BOLD -> FaceColors(Color(0xFFFF6B4A), null, Color.White, Color.White, bold = true, quartersOnly = true)
}

/** Cadran dessiné avec la même géométrie que les drawables du widget (viewport 100). */
@Composable
private fun AnalogClockView(node: AnalogClock, modifier: Modifier) {
    val now = rememberNowMillis()
    val colors = node.face.colors()
    val time = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime()
    val minute = time.minute + time.second / 60f
    val hour = (time.hour % 12) + minute / 60f

    Canvas(modifier.size(node.sizeDp.dp)) {
        val unit = size.minDimension / 100f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(colors.dial, radius = 47f * unit, center = c)
        colors.ring?.let { drawCircle(it, radius = 46f * unit, center = c, style = Stroke(width = 2.4f * unit)) }
        for (i in 0 until 12) {
            val major = i % 3 == 0
            if (colors.quartersOnly && !major) continue
            val angle = Math.toRadians(i * 30.0 - 90.0)
            val length = (if (major) 8f else 5f) * unit
            val outer = 44f * unit
            val from = Offset(c.x + (outer * Math.cos(angle)).toFloat(), c.y + (outer * Math.sin(angle)).toFloat())
            val to = Offset(c.x + ((outer - length) * Math.cos(angle)).toFloat(), c.y + ((outer - length) * Math.sin(angle)).toFloat())
            drawLine(colors.tick, from, to, strokeWidth = (if (major) 2.8f else 1.6f) * unit, cap = StrokeCap.Butt)
        }
        fun hand(angleDeg: Float, length: Float, width: Float) {
            val a = Math.toRadians(angleDeg - 90.0)
            drawLine(
                colors.hand, c,
                Offset(c.x + (length * unit * Math.cos(a)).toFloat(), c.y + (length * unit * Math.sin(a)).toFloat()),
                strokeWidth = width * unit, cap = StrokeCap.Round,
            )
        }
        hand(hour * 30f, 26f, if (colors.bold) 6.5f else 4.4f)
        hand(minute * 6f, 38f, if (colors.bold) 5f else 3.2f)
        drawCircle(colors.hand, radius = 3.2f * unit, center = c)
    }
}

private fun HAlign.bias(): Float = when (this) {
    HAlign.START -> -1f
    HAlign.CENTER -> 0f
    HAlign.END -> 1f
}

private fun VAlign.bias(): Float = when (this) {
    VAlign.TOP -> -1f
    VAlign.CENTER -> 0f
    VAlign.BOTTOM -> 1f
}

private fun HAlign.alignment(): Alignment.Horizontal = when (this) {
    HAlign.START -> Alignment.Start
    HAlign.CENTER -> Alignment.CenterHorizontally
    HAlign.END -> Alignment.End
}

private fun VAlign.alignment(): Alignment.Vertical = when (this) {
    VAlign.TOP -> Alignment.Top
    VAlign.CENTER -> Alignment.CenterVertically
    VAlign.BOTTOM -> Alignment.Bottom
}

private fun VAlign.arrangement() = when (this) {
    VAlign.TOP -> androidx.compose.foundation.layout.Arrangement.Top
    VAlign.CENTER -> androidx.compose.foundation.layout.Arrangement.Center
    VAlign.BOTTOM -> androidx.compose.foundation.layout.Arrangement.Bottom
}

private fun HAlign.arrangement() = when (this) {
    HAlign.START -> androidx.compose.foundation.layout.Arrangement.Start
    HAlign.CENTER -> androidx.compose.foundation.layout.Arrangement.Center
    HAlign.END -> androidx.compose.foundation.layout.Arrangement.End
}

private fun TextWeight.compose(): FontWeight = when (this) {
    TextWeight.NORMAL -> FontWeight.Normal
    TextWeight.MEDIUM -> FontWeight.Medium
    TextWeight.BOLD -> FontWeight.Bold
}

private fun WidgetFont.compose(): FontFamily = when (this) {
    WidgetFont.SANS -> FontFamily.SansSerif
    WidgetFont.SERIF -> FontFamily.Serif
    WidgetFont.MONO -> FontFamily.Monospace
    WidgetFont.CURSIVE -> FontFamily.Cursive
}

private fun NodeTextAlign.compose(): ComposeTextAlign = when (this) {
    NodeTextAlign.START -> ComposeTextAlign.Start
    NodeTextAlign.CENTER -> ComposeTextAlign.Center
    NodeTextAlign.END -> ComposeTextAlign.End
}
