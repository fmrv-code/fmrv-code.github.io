package fmrv.widgetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.widget.DeviceFormat
import fmrv.widgetstudio.widget.WidgetStyle

private fun fontOf(font: WidgetFont): FontFamily = when (font) {
    WidgetFont.SANS -> FontFamily.SansSerif
    WidgetFont.SERIF -> FontFamily.Serif
    WidgetFont.MONO -> FontFamily.Monospace
    WidgetFont.CURSIVE -> FontFamily.Cursive
}

/** Couleurs des icônes de remplacement quand un dossier n'a pas encore d'applications. */
private val PlaceholderTints = listOf(
    Color(0xFF25D366), Color(0xFFE1306C), Color(0xFF3E91FF), Color(0xFFFF9500), Color(0xFF8E5CFF), Color(0xFFFF3B30),
)

@Composable
internal fun PillPreview(
    config: WidgetConfig,
    icon: ImageBitmap?,
    heightDp: Float,
    scale: Float,
    textColor: Color,
    search: Boolean,
) {
    val circle = WidgetStyle.pillCircleDp(heightDp) * scale
    val textSp = WidgetStyle.pillTextSp(WidgetStyle.pillCircleDp(heightDp)) * scale
    val label = if (search) {
        config.label.ifBlank { stringResource(R.string.search_hint_default) }
    } else {
        config.label.ifBlank { config.targetLabel ?: config.name }
    }
    Row(Modifier.fillMaxSize().padding(horizontal = (8 * scale).dp), verticalAlignment = Alignment.CenterVertically) {
        if (search) {
            AppIconImage(icon, (circle * 0.82f).dp)
        } else {
            Box(
                Modifier.size(circle.dp).clip(CircleShape).background(Color(config.accentColor)),
                contentAlignment = Alignment.Center,
            ) { AppIconImage(icon, (circle * 0.62f).dp) }
        }
        Spacer(Modifier.width((12 * scale).dp))
        Text(
            text = label,
            color = if (search) textColor.copy(alpha = 0.78f) else textColor,
            fontSize = textSp.sp,
            fontWeight = if (search) FontWeight.Normal else FontWeight.Bold,
            fontFamily = fontOf(config.font),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (search) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = textColor, modifier = Modifier.size((24 * scale).dp))
        }
    }
}

@Composable
internal fun GridPreview(
    config: WidgetConfig,
    icons: Map<String, ImageBitmap>,
    sizeOption: WidgetSizeOption,
    scale: Float,
    textColor: Color,
) {
    val widthDp = sizeOption.widthDp.toFloat()
    val heightDp = sizeOption.heightDp.toFloat()
    val packages = config.packages.take(WidgetConfig.MAX_GRID_APPS)
    // Sans application choisie, on montre une grille d'exemple pour donner une idée du rendu.
    val slots: List<ImageBitmap?> = if (packages.isEmpty()) List(6) { null } else packages.map { icons[it] }
    val columns = WidgetStyle.gridColumns(widthDp)
    val rows = WidgetStyle.gridRows(slots.size, columns)
    val labelVisible = WidgetStyle.showGridLabel(config.showLabel, heightDp) && config.name.isNotBlank()
    val iconDp = WidgetStyle.gridIconDp(widthDp, heightDp, columns, rows, labelVisible) * scale
    val cellDp = (widthDp - 20f) / columns * scale

    Column(
        Modifier.fillMaxSize().padding((10 * scale).dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        slots.chunked(columns).forEachIndexed { rowIndex, rowSlots ->
            Row(Modifier.fillMaxWidth().padding(vertical = (3 * scale).dp)) {
                rowSlots.forEachIndexed { colIndex, bitmap ->
                    Box(Modifier.width(cellDp.dp), contentAlignment = Alignment.Center) {
                        if (bitmap != null) {
                            AppIconImage(bitmap, iconDp.dp)
                        } else {
                            val tint = PlaceholderTints[(rowIndex * columns + colIndex) % PlaceholderTints.size]
                            Box(Modifier.size(iconDp.dp).clip(RoundedCornerShape((iconDp * 0.24f).dp)).background(tint))
                        }
                    }
                }
            }
        }
        if (labelVisible) {
            Text(
                text = config.name,
                color = textColor,
                fontSize = (12 * scale).sp,
                fontWeight = FontWeight.Medium,
                fontFamily = fontOf(config.font),
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun DevicePreview(config: WidgetConfig, sizeOption: WidgetSizeOption, scale: Float, textColor: Color) {
    val heightDp = sizeOption.heightDp.toFloat()
    val widthDp = sizeOption.widthDp.toFloat()
    val accent = Color(config.accentColor)
    val compact = heightDp < 120f
    val barDp = 34f * scale
    val trackDp = (if (compact) widthDp * 0.5f - 56f else widthDp - 24f) * scale
    val storage = DeviceFormat.size(314_100_000_000L)
    val ram = DeviceFormat.size(2_400_000_000L)

    if (compact) {
        Row(Modifier.fillMaxSize().padding(horizontal = (10 * scale).dp), verticalAlignment = Alignment.CenterVertically) {
            CareButtonPreview(accent, ((heightDp - 20f).coerceIn(36f, 56f)) * scale)
            Spacer(Modifier.width((10 * scale).dp))
            GaugePreview(Icons.Outlined.PieChart, storage, 0.4f, trackDp, barDp, scale, Color(0xFF3FB7C9), textColor)
        }
        return
    }
    Column(
        Modifier.fillMaxSize().padding((12 * scale).dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CareButtonPreview(accent, ((heightDp * 0.3f).coerceIn(40f, 64f)) * scale)
        Spacer(Modifier.height((10 * scale).dp))
        GaugePreview(Icons.Outlined.PieChart, storage, 0.4f, trackDp, barDp, scale, Color(0xFF3FB7C9), textColor)
        Spacer(Modifier.height((8 * scale).dp))
        GaugePreview(Icons.Outlined.Memory, ram, 0.7f, trackDp, barDp, scale, accent, textColor)
        if (heightDp >= 190f) {
            Spacer(Modifier.height((8 * scale).dp))
            GaugePreview(Icons.Outlined.BatteryFull, "76 %", 0.76f, trackDp, barDp, scale, Color(0xFF58C27D), textColor)
        }
    }
}

@Composable
private fun CareButtonPreview(accent: Color, diameterDp: Float) {
    Box(Modifier.size(diameterDp.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.CleaningServices, contentDescription = null, tint = Color.White, modifier = Modifier.size((diameterDp * 0.5f).dp))
    }
}

@Composable
private fun GaugePreview(
    icon: ImageVector,
    value: String,
    fraction: Float,
    trackDp: Float,
    barDp: Float,
    scale: Float,
    color: Color,
    textColor: Color,
) {
    val fill = DeviceFormat.gaugeFillDp(trackDp, fraction, minDp = barDp, reserveDp = 72f * scale)
    Row(
        Modifier.width(trackDp.dp).height(barDp.dp).clip(RoundedCornerShape((barDp / 2f).dp)).background(textColor.copy(alpha = 0.16f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.width(fill.dp).height(barDp.dp).clip(RoundedCornerShape((barDp / 2f).dp)).background(color),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(start = (9 * scale).dp).size((16 * scale).dp))
        }
        Spacer(Modifier.weight(1f))
        Text(value, color = textColor, fontSize = (14 * scale).sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.padding(end = (12 * scale).dp))
    }
}
