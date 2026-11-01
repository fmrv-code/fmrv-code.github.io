package fmrv.widgetstudio.domain.model

enum class WidgetShape { SQUARE, ROUNDED, PILL }

enum class WidgetContentType {
    APP_SHORTCUT,
    APP_GRID,
    SEARCH_BAR,
    CLOCK_DATE,
    DEVICE_STATUS,
    WEATHER_CLOCK,
    NOTES_LIST,
    NEWS_FEED,
    MAIL_INBOX,
    ;

    /** Types dont le contenu est une liste alimentée par un service distant (cache local + actualisation). */
    val isList: Boolean get() = this == NOTES_LIST || this == NEWS_FEED || this == MAIL_INBOX

    /** Types qui ont besoin d'une actualisation périodique en arrière-plan (réseau). */
    val hasRemoteData: Boolean get() = isList || this == WEATHER_CLOCK

    /** Types qui lancent une seule application cible au toucher. */
    val usesTargetApp: Boolean get() = this == APP_SHORTCUT || this == SEARCH_BAR
}

enum class WidgetFont { SANS, SERIF, MONO, CURSIVE }

/** Disposition d'un raccourci d'application. */
enum class WidgetLayout { ICON, PILL }

/** Nature du fond : couleur unie, dégradé diagonal, ou « verre » (dégradé translucide). */
enum class BackgroundStyle { SOLID, GRADIENT, GLASS }

/** Tailles proposées à l'épinglage ; l'utilisateur peut ensuite redimensionner le widget. */
enum class WidgetSizeOption(val widthDp: Int, val heightDp: Int) {
    SMALL(72, 72),
    MEDIUM(160, 160),
    WIDE(320, 160),
    BAR(320, 72),
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
    val background: BackgroundStyle = BackgroundStyle.SOLID,
    val backgroundColor: Int = DEFAULT_BACKGROUND,
    /** Deuxième couleur du dégradé (ignorée pour un fond uni). */
    val backgroundEnd: Int = DEFAULT_BACKGROUND,
    val textColor: Int = DEFAULT_TEXT,
    /** Couleur des pastilles, jauges et icônes mises en avant. */
    val accentColor: Int = DEFAULT_ACCENT,
    val opacity: Float = 1f,
    val font: WidgetFont = WidgetFont.SANS,
    val layout: WidgetLayout = WidgetLayout.ICON,
    /** Applications d'un dossier ([WidgetContentType.APP_GRID]). */
    val packages: List<String> = emptyList(),
    /** Texte libre : libellé d'une pilule, texte d'invite d'une barre de recherche. */
    val label: String = "",
    val showLabel: Boolean = true,
    val use24Hour: Boolean = true,
    val showDate: Boolean = true,
) {
    companion object {
        const val NO_ID = 0

        /** Ligne réservée en base : configuration en attente d'être attribuée à un widget épinglé. */
        const val PENDING_ID = -1

        /** Nombre maximal d'applications dans un dossier. */
        const val MAX_GRID_APPS = 8

        val DEFAULT_BACKGROUND: Int = 0xFF3E91FF.toInt()
        val DEFAULT_TEXT: Int = 0xFFFFFFFF.toInt()
        val DEFAULT_ACCENT: Int = 0xFF3E91FF.toInt()
    }
}
