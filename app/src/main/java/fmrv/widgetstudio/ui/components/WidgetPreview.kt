package fmrv.widgetstudio.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.widget.WidgetStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private fun composeFont(font: WidgetFont): FontFamily = when (font) {
    WidgetFont.SANS -> FontFamily.SansSerif
    WidgetFont.SERIF -> FontFamily.Serif
    WidgetFont.MONO -> FontFamily.Monospace
    WidgetFont.CURSIVE -> FontFamily.Cursive
}

/**
 * Aperçu Compose d'un widget, calqué sur le rendu Glance (mêmes règles via [WidgetStyle]).
 * [scale] permet d'afficher des miniatures cohérentes.
 */
@Composable
fun WidgetPreview(
    config: WidgetConfig,
    icon: ImageBitmap?,
    sizeOption: WidgetSizeOption,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val widthDp = sizeOption.widthDp * scale
    val heightDp = sizeOption.heightDp * scale
    val textColor = Color(config.textColor)
    val shape = RoundedCornerShape((WidgetStyle.cornerRadiusDp(config.shape) * scale).dp)

    Box(
        modifier = modifier
            .size(widthDp.dp, heightDp.dp)
            .clip(shape)
            .background(Color(config.backgroundColor).copy(alpha = config.opacity)),
        contentAlignment = Alignment.Center,
    ) {
        when (config.contentType) {
            WidgetContentType.APP_SHORTCUT -> ShortcutPreview(
                config = config,
                icon = icon,
                widthDp = widthDp,
                heightDp = heightDp,
                // La visibilité du nom se décide sur la taille réelle du widget, pas sur celle de la miniature.
                labelVisible = WidgetStyle.isLabelVisible(config.showLabel, sizeOption.heightDp.toFloat()),
                textColor = textColor,
            )
            WidgetContentType.CLOCK_DATE -> ClockPreview(config, widthDp, heightDp, textColor)
        }
    }
}

@Composable
private fun ShortcutPreview(
    config: WidgetConfig,
    icon: ImageBitmap?,
    widthDp: Float,
    heightDp: Float,
    labelVisible: Boolean,
    textColor: Color,
) {
    val iconSize = WidgetStyle.iconSizeDp(widthDp, heightDp, labelVisible).dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(8.dp),
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(iconSize))
        } else {
            Icon(Icons.Outlined.Apps, contentDescription = null, tint = textColor, modifier = Modifier.size(iconSize))
        }
        if (labelVisible) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = config.targetLabel ?: config.name,
                color = textColor,
                fontSize = WidgetStyle.labelSizeSp(widthDp, heightDp).sp,
                fontWeight = FontWeight.Medium,
                fontFamily = composeFont(config.font),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClockPreview(config: WidgetConfig, widthDp: Float, heightDp: Float, textColor: Color) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val locale = Locale.getDefault()
    val moment = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
    val time = DateTimeFormatter.ofPattern(WidgetStyle.timePattern(config.use24Hour), locale).format(moment)
    val date = DateTimeFormatter.ofPattern(WidgetStyle.datePattern(widthDp), locale).format(moment)
    val timeSp = WidgetStyle.timeSizeSp(widthDp, heightDp, config.showDate)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            text = time,
            color = textColor,
            fontSize = timeSp.sp,
            fontFamily = composeFont(config.font),
            maxLines = 1,
        )
        if (config.showDate) {
            Text(
                text = date,
                color = textColor,
                fontSize = WidgetStyle.dateSizeSp(timeSp).sp,
                fontFamily = composeFont(config.font),
                maxLines = 1,
            )
        }
    }
}
