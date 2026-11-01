package fmrv.widgetstudio.render

import android.graphics.Bitmap

/** Charge les images d'un arbre (icônes d'applications, photos) à la taille demandée. */
interface AssetProvider {
    suspend fun bitmap(source: ImgSource, sizePx: Int): Bitmap?
}

/** Charge toutes les images demandées ; celles qui échouent sont simplement absentes du résultat. */
suspend fun AssetProvider.loadAll(requests: Map<ImgSource, Float>, density: Float): Map<ImgSource, Bitmap> {
    val result = LinkedHashMap<ImgSource, Bitmap>()
    for ((source, dp) in requests) {
        bitmap(source, (dp * density).toInt().coerceAtLeast(16))?.let { result[source] = it }
    }
    return result
}
