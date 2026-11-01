package fmrv.widgetstudio.render

import fmrv.widgetstudio.domain.model.WidgetFont

/*
 * Arbre d'éléments décrivant l'intérieur d'un widget. Chaque type de widget le produit UNE fois ;
 * deux moteurs le dessinent : Glance (écran d'accueil) et Compose (aperçus de l'application).
 * Ce code est du Kotlin pur : il se teste sur JVM.
 */

enum class HAlign { START, CENTER, END }

enum class VAlign { TOP, CENTER, BOTTOM }

enum class TextWeight { NORMAL, MEDIUM, BOLD }

enum class TextAlign { START, CENTER, END }

enum class ImgScale { FIT, CROP, FILL }

/** Marges en dp. */
data class Pad(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f) {
    companion object {
        val None = Pad()

        fun all(value: Float) = Pad(value, value, value, value)

        fun sym(horizontal: Float = 0f, vertical: Float = 0f) = Pad(horizontal, vertical, horizontal, vertical)
    }
}

/** Action déclenchée au toucher. */
sealed interface Click {
    /** Lance une application ; ouvre la configuration du widget si elle n'est plus installée. */
    data class LaunchApp(val packageName: String) : Click

    data class OpenUri(val uri: String, val packageName: String? = null) : Click

    /** Action système (ex. réglages de stockage). */
    data class SystemAction(val action: String) : Click

    /** Action interne identifiée (cocher une tâche, actualiser…). */
    data class Callback(val id: String, val argument: String = "") : Click

    data object Refresh : Click

    data object Configure : Click
}

/** Modificateurs communs à tous les éléments. Les dimensions sont en dp. */
data class Mod(
    val width: Float? = null,
    val height: Float? = null,
    val fillWidth: Boolean = false,
    val fillHeight: Boolean = false,
    /** > 0 : l'élément se partage l'espace restant d'une ligne ou d'une colonne. */
    val weight: Float = 0f,
    val pad: Pad = Pad.None,
    /** Couleur ARGB de fond d'un élément (pastille, jauge). */
    val background: Int? = null,
    val corner: Float = 0f,
    val click: Click? = null,
) {
    companion object {
        val Fill = Mod(fillWidth = true, fillHeight = true)
    }
}

/** Image : ressource, icône d'application ou fichier (photo choisie par l'utilisateur). */
sealed interface ImgSource {
    data class Res(val id: Int) : ImgSource

    data class AppIcon(val packageName: String) : ImgSource

    data class File(val path: String) : ImgSource
}

/** Visuel d'une horloge analogique. */
enum class AnalogFace { LIGHT, DARK, CLASSIC, BOLD }

sealed interface Node {
    val mod: Mod
}

data class Box(
    override val mod: Mod = Mod(),
    val horizontal: HAlign = HAlign.CENTER,
    val vertical: VAlign = VAlign.CENTER,
    val children: List<Node> = emptyList(),
) : Node

data class Column(
    override val mod: Mod = Mod(),
    val horizontal: HAlign = HAlign.START,
    val vertical: VAlign = VAlign.TOP,
    val children: List<Node> = emptyList(),
) : Node

data class Row(
    override val mod: Mod = Mod(),
    val horizontal: HAlign = HAlign.START,
    val vertical: VAlign = VAlign.TOP,
    val children: List<Node> = emptyList(),
) : Node

data class Text(
    val text: String,
    val sizeSp: Float,
    val color: Int,
    val weight: TextWeight = TextWeight.NORMAL,
    val font: WidgetFont = WidgetFont.SANS,
    val align: TextAlign = TextAlign.START,
    val maxLines: Int = 1,
    override val mod: Mod = Mod(),
) : Node

data class Img(
    val source: ImgSource,
    val widthDp: Float,
    val heightDp: Float,
    val scale: ImgScale = ImgScale.FIT,
    /** Couleur de teinte ARGB (icônes monochromes). */
    val tint: Int? = null,
    val description: String? = null,
    override val mod: Mod = Mod(),
) : Node

data class Space(val widthDp: Float = 0f, val heightDp: Float = 0f) : Node {
    override val mod: Mod get() = Mod()
}

/**
 * Texte d'horloge animé par le système (sans réveiller l'application).
 * [pattern] n'utilise que les lettres communes à Android et à java.time : H, h, m, s, a, E, d, M, y.
 */
data class ClockText(
    val pattern: String,
    val sizeSp: Float,
    val color: Int,
    val font: WidgetFont = WidgetFont.SANS,
    val bold: Boolean = false,
    val align: TextAlign = TextAlign.START,
    /** Identifiant de fuseau (ex. "Europe/Paris") ; `null` pour celui de l'appareil. */
    val timeZone: String? = null,
    override val mod: Mod = Mod(),
) : Node

data class AnalogClock(
    val face: AnalogFace,
    val sizeDp: Float,
    override val mod: Mod = Mod(),
) : Node

/** Liste verticale défilante. */
data class ScrollList(
    val items: List<Node>,
    override val mod: Mod = Mod(),
) : Node
