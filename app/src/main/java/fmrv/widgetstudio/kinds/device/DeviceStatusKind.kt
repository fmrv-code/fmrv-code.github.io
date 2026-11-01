package fmrv.widgetstudio.kinds.device

import android.content.Context
import android.provider.Settings
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.Category
import fmrv.widgetstudio.domain.model.StylePresets
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.kinds.LoadEnv
import fmrv.widgetstudio.kinds.ParamField
import fmrv.widgetstudio.kinds.RenderEnv
import fmrv.widgetstudio.kinds.WidgetKind
import fmrv.widgetstudio.render.Click
import fmrv.widgetstudio.render.Colors
import fmrv.widgetstudio.render.HAlign
import fmrv.widgetstudio.render.Img
import fmrv.widgetstudio.render.ImgSource
import fmrv.widgetstudio.render.Mod
import fmrv.widgetstudio.render.Node
import fmrv.widgetstudio.render.Pad
import fmrv.widgetstudio.render.Text
import fmrv.widgetstudio.render.TextAlign
import fmrv.widgetstudio.render.TextWeight
import fmrv.widgetstudio.render.VAlign
import fmrv.widgetstudio.render.box
import fmrv.widgetstudio.render.column
import fmrv.widgetstudio.render.row
import fmrv.widgetstudio.render.space
import fmrv.widgetstudio.widget.DeviceFormat
import fmrv.widgetstudio.widget.DeviceSnapshot
import fmrv.widgetstudio.widget.DeviceStatsReader

data class DeviceData(val snapshot: DeviceSnapshot, val careClick: Click)

private const val SAMSUNG_DEVICE_CARE = "com.samsung.android.lool"
private const val GAUGE_DP = 34f
private const val TEAL = 0xFF3FB7C9.toInt()
private const val GREEN = 0xFF58C27D.toInt()

/** Entretien de l'appareil : bouton rond et jauges de stockage, de mémoire et de batterie. */
object DeviceStatusKind : WidgetKind<DeviceData> {
    override val id = "device.status"
    override val category = Category.DEVICE
    override val title = R.string.kind_device
    override val description = R.string.kind_device_desc
    override val defaultSize = WidgetSizeOption.MEDIUM
    override val defaultStyle = StylePresets.GlassLight.style
    override val fields = emptyList<ParamField>()

    override suspend fun load(env: LoadEnv): DeviceData {
        val installed = isInstalled(env.context, SAMSUNG_DEVICE_CARE)
        return DeviceData(
            snapshot = DeviceStatsReader.read(env.context),
            careClick = if (installed) Click.LaunchApp(SAMSUNG_DEVICE_CARE) else Click.SystemAction(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        )
    }

    private fun isInstalled(context: Context, packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null

    override fun render(env: RenderEnv, data: DeviceData): Node {
        val s = data.snapshot
        val compact = env.heightDp < 120f
        val track = if (compact) env.widthDp * 0.5f - 56f else env.widthDp - 24f
        val storage = DeviceFormat.size(s.storageFreeBytes, env.locale)
        val ram = DeviceFormat.size(s.ramFreeBytes, env.locale)

        fun button(diameter: Float) = box(Mod(width = diameter, height = diameter, background = env.accent, corner = diameter / 2f, click = data.careClick)) {
            +Img(ImgSource.Res(R.drawable.ic_clean), diameter * 0.5f, diameter * 0.5f, tint = Colors.onColor(env.accent))
        }

        fun gauge(icon: Int, value: String, fraction: Float, color: Int, width: Float): Node {
            val fill = DeviceFormat.gaugeFillDp(width, fraction, minDp = GAUGE_DP, reserveDp = 72f)
            return row(
                Mod(width = width, height = GAUGE_DP, background = Colors.withOpacity(env.text, 0.16f), corner = GAUGE_DP / 2f, click = Click.Refresh),
                v = VAlign.CENTER,
            ) {
                +box(Mod(width = fill, height = GAUGE_DP, background = color, corner = GAUGE_DP / 2f), h = HAlign.START) {
                    +Img(ImgSource.Res(icon), 16f, 16f, tint = 0xFFFFFFFF.toInt(), mod = Mod(pad = Pad(left = 9f)))
                }
                +Text(value, 14f, env.text, TextWeight.MEDIUM, align = TextAlign.END, mod = Mod(weight = 1f, pad = Pad(right = 12f)))
            }
        }

        if (compact) {
            return row(Mod.Fill.copy(pad = Pad.sym(horizontal = 10f)), v = VAlign.CENTER) {
                +button((env.heightDp - 20f).coerceIn(36f, 56f))
                +space(w = 10f)
                +gauge(R.drawable.ic_storage, storage, s.storageUsedFraction, TEAL, track)
            }
        }
        return column(Mod.Fill.copy(pad = Pad.all(12f)), HAlign.CENTER, VAlign.CENTER) {
            +button((env.heightDp * 0.3f).coerceIn(40f, 64f))
            +space(h = 10f)
            +gauge(R.drawable.ic_storage, storage, s.storageUsedFraction, TEAL, track)
            +space(h = 8f)
            +gauge(R.drawable.ic_memory, ram, s.ramUsedFraction, env.accent, track)
            if (env.heightDp >= 190f) {
                +space(h = 8f)
                +gauge(R.drawable.ic_battery, "${s.batteryPercent} %", s.batteryFraction, GREEN, track)
            }
        }
    }
}
