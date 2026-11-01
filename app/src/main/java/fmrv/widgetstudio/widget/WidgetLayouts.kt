package fmrv.widgetstudio.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.WidgetConfig

/** Redessine tous les widgets (valeurs de l'appareil rafraîchies) ; branché sur les jauges. */
class RefreshWidgetsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        UniversalWidget().updateAll(context)
    }
}

internal fun launchIntentFor(context: Context, packageName: String?): Intent? =
    packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** Pilule (grande pastille ronde + libellé) ou barre de recherche (icône + texte d'invite + loupe). */
@Composable
internal fun PillContent(
    context: Context,
    config: WidgetConfig,
    icon: Bitmap?,
    heightDp: Float,
    search: Boolean,
) {
    val circle = WidgetStyle.pillCircleDp(heightDp)
    val textSp = WidgetStyle.pillTextSp(circle)
    val textColor = Color(config.textColor)
    val label = if (search) {
        config.label.ifBlank { context.getString(R.string.search_hint_default) }
    } else {
        config.label.ifBlank { config.targetLabel ?: config.name }
    }

    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (search) {
            // Barre de recherche : l'icône de l'application, sans pastille.
            AppIcon(icon, config, (circle * 0.82f).dp)
        } else {
            Box(
                modifier = GlanceModifier
                    .size(circle.dp)
                    .background(ColorProvider(Color(config.accentColor)))
                    .cornerRadius((circle / 2f).dp),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(icon, config, (circle * 0.62f).dp)
            }
        }
        Spacer(modifier = GlanceModifier.width(12.dp))
        Text(
            text = label,
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = ColorProvider(if (search) textColor.copy(alpha = 0.78f) else textColor),
                fontSize = textSp.sp,
                fontWeight = if (search) FontWeight.Normal else FontWeight.Bold,
                fontFamily = glanceFont(config.font),
            ),
        )
        if (search) {
            Image(
                provider = ImageProvider(R.drawable.ic_search),
                contentDescription = null,
                modifier = GlanceModifier.size(36.dp).padding(6.dp),
                colorFilter = ColorFilter.tint(ColorProvider(textColor)),
            )
        }
    }
}

@Composable
private fun AppIcon(icon: Bitmap?, config: WidgetConfig, size: androidx.compose.ui.unit.Dp) {
    if (icon != null) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = config.targetLabel,
            modifier = GlanceModifier.size(size),
        )
    } else {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_placeholder),
            contentDescription = config.targetLabel,
            modifier = GlanceModifier.size(size),
            colorFilter = ColorFilter.tint(ColorProvider(Color(config.textColor))),
        )
    }
}

/** Dossier d'applications : grille d'icônes, chacune lance son application. */
@Composable
internal fun GridContent(
    context: Context,
    config: WidgetConfig,
    icons: Map<String, Bitmap>,
    widthDp: Float,
    heightDp: Float,
) {
    val packages = config.packages.filter { it in icons }.take(WidgetConfig.MAX_GRID_APPS)
    val columns = WidgetStyle.gridColumns(widthDp)
    val rows = WidgetStyle.gridRows(packages.size, columns)
    val labelVisible = WidgetStyle.showGridLabel(config.showLabel, heightDp) && config.name.isNotBlank()
    val iconDp = WidgetStyle.gridIconDp(widthDp, heightDp, columns, rows, labelVisible)
    val cellDp = (widthDp - 20f) / columns

    Column(
        modifier = GlanceModifier.fillMaxSize().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        packages.chunked(columns).forEach { rowPackages ->
            Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
                rowPackages.forEach { pkg ->
                    val intent = launchIntentFor(context, pkg)
                    val cell = GlanceModifier.width(cellDp.dp)
                    Box(
                        modifier = if (intent != null) cell.clickable(actionStartActivity(intent)) else cell,
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(icons.getValue(pkg)),
                            contentDescription = null,
                            modifier = GlanceModifier.size(iconDp.dp),
                        )
                    }
                }
            }
        }
        if (labelVisible) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = config.name,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(Color(config.textColor)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = glanceFont(config.font),
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

private val StorageColor = Color(0xFF3FB7C9)
private val BatteryColor = Color(0xFF58C27D)

/** « Entretien de l'appareil » : bouton rond + jauges de stockage, de mémoire et de batterie. */
@Composable
internal fun DeviceContent(
    context: Context,
    config: WidgetConfig,
    snapshot: DeviceSnapshot?,
    widthDp: Float,
    heightDp: Float,
) {
    if (snapshot == null) return
    val text = Color(config.textColor)
    val accent = Color(config.accentColor)
    val compact = heightDp < 120f
    val trackDp = if (compact) widthDp * 0.5f else widthDp - 24f
    val barDp = 34f
    val refresh = actionRunCallback<RefreshWidgetsAction>()
    val careIntent = deviceCareIntent(context)
    val careAction: Action = actionStartActivity(careIntent)

    val storage = "${DeviceFormat.size(snapshot.storageFreeBytes)}"
    val ram = DeviceFormat.size(snapshot.ramFreeBytes)

    if (compact) {
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CareButton(accent, careAction, (heightDp - 20f).coerceIn(36f, 56f))
            Spacer(modifier = GlanceModifier.width(10.dp))
            Gauge(R.drawable.ic_storage, storage, snapshot.storageUsedFraction, trackDp - 56f, StorageColor, text, barDp, refresh)
        }
        return
    }

    Column(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CareButton(accent, careAction, (heightDp * 0.3f).coerceIn(40f, 64f))
        Spacer(modifier = GlanceModifier.height(10.dp))
        Gauge(R.drawable.ic_storage, storage, snapshot.storageUsedFraction, trackDp, StorageColor, text, barDp, refresh)
        Spacer(modifier = GlanceModifier.height(8.dp))
        Gauge(R.drawable.ic_memory, ram, snapshot.ramUsedFraction, trackDp, accent, text, barDp, refresh)
        if (heightDp >= 190f) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            val battery = "${snapshot.batteryPercent} %"
            Gauge(R.drawable.ic_battery, battery, snapshot.batteryFraction, trackDp, BatteryColor, text, barDp, refresh)
        }
    }
}

@Composable
private fun CareButton(accent: Color, action: Action, diameterDp: Float) {
    Box(
        modifier = GlanceModifier
            .size(diameterDp.dp)
            .background(ColorProvider(accent))
            .cornerRadius((diameterDp / 2f).dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_clean),
            contentDescription = null,
            modifier = GlanceModifier.size((diameterDp * 0.5f).dp),
            colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
        )
    }
}

@Composable
private fun Gauge(
    @DrawableRes icon: Int,
    value: String,
    fraction: Float,
    trackDp: Float,
    color: Color,
    textColor: Color,
    barDp: Float,
    onClick: Action,
) {
    val fillDp = DeviceFormat.gaugeFillDp(trackDp, fraction, minDp = barDp, reserveDp = 72f)
    Row(
        modifier = GlanceModifier
            .width(trackDp.dp)
            .height(barDp.dp)
            .background(ColorProvider(textColor.copy(alpha = 0.16f)))
            .cornerRadius((barDp / 2f).dp)
            .clickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(fillDp.dp)
                .height(barDp.dp)
                .background(ColorProvider(color))
                .cornerRadius((barDp / 2f).dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = null,
                modifier = GlanceModifier.size(barDp.dp).padding(((barDp - 16f) / 2f).dp),
                colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
            )
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = value,
            maxLines = 1,
            modifier = GlanceModifier.padding(end = 12.dp),
            style = TextStyle(
                color = ColorProvider(textColor),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/** Entretien de l'appareil Samsung s'il est présent, sinon les réglages de stockage. */
private fun deviceCareIntent(context: Context): Intent =
    (launchIntentFor(context, SAMSUNG_DEVICE_CARE)
        ?: Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

private const val SAMSUNG_DEVICE_CARE = "com.samsung.android.lool"
