package fmrv.widgetstudio.kinds

import fmrv.widgetstudio.render.Click

/** Règles de dimensionnement partagées par les types de widgets. */
object Fit {
    /** Taille d'un texte proportionnelle à la place disponible, bornée. */
    fun sp(widthDp: Float, heightDp: Float, byWidth: Float, byHeight: Float, min: Float, max: Float): Float =
        minOf(widthDp * byWidth, heightDp * byHeight).coerceIn(min, max)

    /** Rayon de coin « pilule » : la moitié de la plus petite dimension. */
    fun pill(widthDp: Float, heightDp: Float): Float = minOf(widthDp, heightDp) / 2f

    fun dateClick(): Click = Click.OpenUri("content://com.android.calendar/time/")
}
