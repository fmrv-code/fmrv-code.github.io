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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.res.stringResource
import fmrv.widgetstudio.R
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fmrv.widgetstudio.domain.model.NoteSummary
import fmrv.widgetstudio.domain.model.BackgroundStyle
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetLayout
import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.widget.WidgetBackground
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
    notes: List<NoteSummary> = emptyList(),
    icons: Map<String, ImageBitmap> = emptyMap(),
) {
    val widthDp = sizeOption.widthDp * scale
    val heightDp = sizeOption.heightDp * scale
    val textColor = Color(config.textColor)
    val shape = RoundedCornerShape((WidgetStyle.cornerRadiusDp(config.shape) * scale).dp)

    val (start, end) = WidgetBackground.colors(config)
    val fill = if (config.background == BackgroundStyle.SOLID) {
        Modifier.background(Color(start))
    } else {
        // Même dégradé diagonal que le widget réel.
        Modifier.background(Brush.linearGradient(listOf(Color(start), Color(end))))
    }

    Box(
        modifier = modifier
            .size(widthDp.dp, heightDp.dp)
            .clip(shape)
            .then(fill),
        contentAlignment = Alignment.Center,
    ) {
        when (config.contentType) {
            WidgetContentType.APP_SHORTCUT -> if (config.layout == WidgetLayout.PILL) {
                PillPreview(config, icon, sizeOption.heightDp.toFloat(), scale, textColor, search = false)
            } else {
                ShortcutPreview(
                    config = config,
                    icon = icon,
                    widthDp = widthDp,
                    heightDp = heightDp,
                    // La visibilité du nom se décide sur la taille réelle du widget, pas sur celle de la miniature.
                    labelVisible = WidgetStyle.isLabelVisible(config.showLabel, sizeOption.heightDp.toFloat()),
                    textColor = textColor,
                )
            }
            WidgetContentType.SEARCH_BAR -> PillPreview(config, icon, sizeOption.heightDp.toFloat(), scale, textColor, search = true)
            WidgetContentType.APP_GRID -> GridPreview(config, icons, sizeOption, scale, textColor)
            WidgetContentType.DEVICE_STATUS -> DevicePreview(config, sizeOption, scale, textColor)
            WidgetContentType.WEATHER_CLOCK -> Unit
            WidgetContentType.CLOCK_DATE -> ClockPreview(config, widthDp, heightDp, textColor)
            WidgetContentType.NOTES_LIST, WidgetContentType.NEWS_FEED, WidgetContentType.MAIL_INBOX ->
                NotesPreview(config, notes, widthDp, heightDp, textColor)
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

@Composable
private fun NotesPreview(
    config: WidgetConfig,
    notes: List<NoteSummary>,
    widthDp: Float,
    heightDp: Float,
    textColor: Color,
) {
    val samples = when (config.contentType) {
        WidgetContentType.NEWS_FEED -> listOf(
            NoteSummary("1", stringResource(R.string.preview_news_1), stringResource(R.string.preview_news_1_snippet), 0L),
            NoteSummary("2", stringResource(R.string.preview_news_2), stringResource(R.string.preview_news_2_snippet), 0L),
            NoteSummary("3", stringResource(R.string.preview_news_3), "", 0L),
        )
        WidgetContentType.MAIL_INBOX -> listOf(
            NoteSummary("1", stringResource(R.string.preview_mail_1), stringResource(R.string.preview_mail_1_from), 0L),
            NoteSummary("2", stringResource(R.string.preview_mail_2), stringResource(R.string.preview_mail_2_from), 0L),
            NoteSummary("3", stringResource(R.string.preview_mail_3), stringResource(R.string.preview_mail_3_from), 0L),
        )
        else -> listOf(
            NoteSummary("1", stringResource(R.string.preview_note_1), stringResource(R.string.preview_note_1_snippet), 0L),
            NoteSummary("2", stringResource(R.string.preview_note_2), stringResource(R.string.preview_note_2_snippet), 0L),
            NoteSummary("3", stringResource(R.string.preview_note_3), stringResource(R.string.preview_note_3_snippet), 0L),
            NoteSummary("4", stringResource(R.string.preview_note_4), "", 0L),
        )
    }
    val shown = notes.ifEmpty { samples }
    val titleSp = WidgetStyle.notesTitleSizeSp(widthDp, heightDp)
    val showSnippet = WidgetStyle.showNoteSnippet(heightDp)
    val font = composeFont(config.font)

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (WidgetStyle.showNotesHeader(heightDp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = config.name.ifBlank { stringResource(listTitle(config.contentType)) },
                    color = textColor,
                    fontSize = (titleSp + 2f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = font,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            }
        }
        shown.take(MAX_PREVIEW_NOTES).forEach { note ->
            Column(Modifier.padding(vertical = 3.dp)) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.notes_untitled) },
                    color = textColor,
                    fontSize = titleSp.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = font,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showSnippet && note.snippet.isNotBlank()) {
                    Text(
                        text = note.snippet,
                        color = textColor.copy(alpha = 0.72f),
                        fontSize = WidgetStyle.notesSnippetSizeSp(titleSp).sp,
                        fontFamily = font,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private const val MAX_PREVIEW_NOTES = 6

private fun listTitle(type: WidgetContentType): Int = when (type) {
    WidgetContentType.NEWS_FEED -> R.string.type_news
    WidgetContentType.MAIL_INBOX -> R.string.type_mail
    else -> R.string.type_notes
}
