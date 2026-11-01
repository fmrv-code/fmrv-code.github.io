package fmrv.widgetstudio.widget

import fmrv.widgetstudio.domain.model.WidgetShape

/**
 * Règles de mise en forme partagées entre le rendu Glance et l'aperçu Compose de l'éditeur,
 * pour que les deux restent visuellement identiques. Toutes les valeurs sont en dp / sp.
 */
object WidgetStyle {
    fun cornerRadiusDp(shape: WidgetShape): Float = when (shape) {
        WidgetShape.SQUARE -> 8f
        WidgetShape.ROUNDED -> 28f
        WidgetShape.PILL -> 100f
    }

    /** En dessous de cette hauteur, le nom de l'application est masqué pour laisser la place à l'icône. */
    fun isLabelVisible(showLabel: Boolean, heightDp: Float): Boolean = showLabel && heightDp >= MIN_HEIGHT_FOR_LABEL

    fun iconSizeDp(widthDp: Float, heightDp: Float, labelVisible: Boolean): Float =
        (minOf(widthDp, heightDp) * if (labelVisible) 0.48f else 0.66f).coerceIn(28f, 72f)

    fun labelSizeSp(widthDp: Float, heightDp: Float): Float =
        (minOf(widthDp, heightDp) * 0.11f).coerceIn(10f, 14f)

    fun timeSizeSp(widthDp: Float, heightDp: Float, showDate: Boolean): Float =
        minOf(widthDp * 0.26f, heightDp * if (showDate) 0.42f else 0.55f).coerceIn(18f, 64f)

    fun dateSizeSp(timeSizeSp: Float): Float = (timeSizeSp * 0.36f).coerceIn(10f, 18f)

    fun notesTitleSizeSp(widthDp: Float, heightDp: Float): Float =
        (minOf(widthDp, heightDp) * 0.085f).coerceIn(11f, 15f)

    fun notesSnippetSizeSp(titleSizeSp: Float): Float = (titleSizeSp * 0.8f).coerceIn(9f, 12f)

    /** Les extraits ne tiennent que sur un widget assez haut. */
    fun showNoteSnippet(heightDp: Float): Boolean = heightDp >= 100f

    fun showNotesHeader(heightDp: Float): Boolean = heightDp >= 80f

    /** Diamètre de la pastille ronde d'une pilule ou d'une barre de recherche. */
    fun pillCircleDp(heightDp: Float): Float = (heightDp - 16f).coerceIn(28f, 64f)

    fun pillTextSp(circleDp: Float): Float = (circleDp * 0.42f).coerceIn(14f, 28f)

    /** 4 colonnes d'icônes sur un widget large, 3 sinon. */
    fun gridColumns(widthDp: Float): Int = if (widthDp >= 200f) 4 else 3

    fun gridRows(count: Int, columns: Int): Int = if (count <= 0) 0 else (count + columns - 1) / columns

    /** Taille des icônes d'un dossier : tient dans la grille avec un peu d'air autour. */
    fun gridIconDp(widthDp: Float, heightDp: Float, columns: Int, rows: Int, labelVisible: Boolean): Float {
        val cellW = (widthDp - 20f) / columns
        val cellH = (heightDp - 20f - if (labelVisible) 18f else 0f) / rows.coerceAtLeast(1)
        return (minOf(cellW, cellH) * 0.74f).coerceIn(24f, 52f)
    }

    fun showGridLabel(showLabel: Boolean, heightDp: Float): Boolean = showLabel && heightDp >= 140f

    fun timePattern(use24Hour: Boolean): String = if (use24Hour) "HH:mm" else "h:mm"

    fun datePattern(widthDp: Float): String = if (widthDp >= WIDE_DATE_MIN_WIDTH) "EEEE d MMMM" else "EEE d MMM"

    private const val MIN_HEIGHT_FOR_LABEL = 72f
    private const val WIDE_DATE_MIN_WIDTH = 150f
}
