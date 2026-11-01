package fmrv.widgetstudio.domain.model

enum class WidgetShape { SQUARE, ROUNDED, PILL }

enum class WidgetContentType { APP_SHORTCUT, CLOCK_DATE, NOTES_LIST }

enum class WidgetFont { SANS, SERIF, MONO, CURSIVE }

/** Tailles proposées à l'épinglage ; l'utilisateur peut ensuite redimensionner le widget. */
enum class WidgetSizeOption(val widthDp: Int, val heightDp: Int) {
    SMALL(72, 72),
    MEDIUM(160, 160),
    WIDE(320, 160),
}

enum class PinResult { REQUESTED, UNSUPPORTED, FAILED }

data class AppInfo(val packageName: String, val label: String)

/**
 * Configuration complète d'un widget posé sur l'écran d'accueil.
 * [appWidgetId] est l'identifiant attribué par le système ([NO_ID] tant que le widget n'est pas posé).
 */
data class WidgetConfig(
    val appWidgetId: Int,
    val name: String = "",
    val contentType: WidgetContentType = WidgetContentType.APP_SHORTCUT,
    val targetPackage: String? = null,
    val targetLabel: String? = null,
    val shape: WidgetShape = WidgetShape.ROUNDED,
    val backgroundColor: Int = DEFAULT_BACKGROUND,
    val textColor: Int = DEFAULT_TEXT,
    val opacity: Float = 1f,
    val font: WidgetFont = WidgetFont.SANS,
    val showLabel: Boolean = true,
    val use24Hour: Boolean = true,
    val showDate: Boolean = true,
) {
    companion object {
        const val NO_ID = 0

        /** Ligne réservée en base : configuration en attente d'être attribuée à un widget épinglé. */
        const val PENDING_ID = -1

        val DEFAULT_BACKGROUND: Int = 0xFF3E91FF.toInt()
        val DEFAULT_TEXT: Int = 0xFFFFFFFF.toInt()
    }
}
