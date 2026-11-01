package fmrv.widgetstudio.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class WidgetShape { SQUARE, ROUNDED, PILL }

/** Nature du fond : couleur unie, dégradé diagonal, ou « verre » (dégradé translucide). */
@Serializable
enum class BackgroundStyle { SOLID, GRADIENT, GLASS }

@Serializable
enum class WidgetFont { SANS, SERIF, MONO, CURSIVE }

/** Tailles proposées à l'épinglage ; l'utilisateur peut ensuite redimensionner le widget. */
enum class WidgetSizeOption(val widthDp: Int, val heightDp: Int) {
    SMALL(72, 72),
    MEDIUM(160, 160),
    WIDE(320, 160),
    BAR(320, 72),
    LARGE(320, 320),
}

enum class PinResult { REQUESTED, UNSUPPORTED, FAILED }

data class AppInfo(val packageName: String, val label: String)

/** Apparence commune à tous les widgets : fond, couleurs, forme, police. */
@Serializable
data class WidgetStyle(
    val background: BackgroundStyle = BackgroundStyle.SOLID,
    val colorStart: Int = DEFAULT_BACKGROUND,
    /** Deuxième couleur du dégradé (ignorée pour un fond uni ou verre). */
    val colorEnd: Int = DEFAULT_BACKGROUND,
    val textColor: Int = DEFAULT_TEXT,
    /** Couleur des éléments mis en avant : jour courant, pastilles, jauges. */
    val accentColor: Int = DEFAULT_ACCENT,
    val opacity: Float = 1f,
    val shape: WidgetShape = WidgetShape.ROUNDED,
    val font: WidgetFont = WidgetFont.SANS,
) {
    companion object {
        val DEFAULT_BACKGROUND: Int = 0xFF3E91FF.toInt()
        val DEFAULT_TEXT: Int = 0xFFFFFFFF.toInt()
        val DEFAULT_ACCENT: Int = 0xFFFFFFFF.toInt()
    }
}

/**
 * Paramètres propres à un type de widget, stockés sous forme de texte (clé → valeur).
 * Les booléens sont "1"/"0", les listes sont séparées par des virgules.
 */
@Serializable
data class Params(val values: Map<String, String> = emptyMap()) {

    fun string(key: String, default: String = ""): String = values[key] ?: default

    fun bool(key: String, default: Boolean = false): Boolean = values[key]?.let { it == "1" } ?: default

    fun int(key: String, default: Int = 0): Int = values[key]?.toIntOrNull() ?: default

    fun float(key: String, default: Float = 0f): Float = values[key]?.toFloatOrNull() ?: default

    fun list(key: String): List<String> =
        values[key].orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }

    fun with(key: String, value: String): Params = Params(values + (key to value))

    fun withBool(key: String, value: Boolean): Params = with(key, if (value) "1" else "0")

    fun withList(key: String, value: List<String>): Params = with(key, value.joinToString(","))

    fun without(key: String): Params = Params(values - key)

    /** Complète avec les valeurs par défaut sans écraser celles qui existent. */
    fun withDefaults(defaults: Map<String, String>): Params = Params(defaults + values)
}

/** Un widget configuré. [appWidgetId] vaut [NO_ID] tant qu'il n'est pas posé sur l'écran d'accueil. */
data class WidgetInstance(
    val appWidgetId: Int,
    val kindId: String,
    val name: String = "",
    val style: WidgetStyle = WidgetStyle(),
    val params: Params = Params(),
) {
    companion object {
        const val NO_ID = 0

        /** Ligne réservée en base : configuration en attente d'être attribuée à un widget épinglé. */
        const val PENDING_ID = -1

        /** Nombre maximal d'applications dans un dossier. */
        const val MAX_FOLDER_APPS = 8
    }
}

/** Rubriques de la galerie. */
enum class Category { CLOCKS, CALENDAR, APPS, INFO, DEVICE }
