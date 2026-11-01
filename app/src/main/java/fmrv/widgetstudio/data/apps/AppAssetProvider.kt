package fmrv.widgetstudio.data.apps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import fmrv.widgetstudio.domain.repository.AppIconProvider
import fmrv.widgetstudio.render.AssetProvider
import fmrv.widgetstudio.render.ImgSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Charge les images des widgets. Les tailles sont plafonnées : un widget transmet ses bitmaps au lanceur
 * par Binder, dont la mémoire est limitée (quelques Mo en tout).
 */
@Singleton
class AppAssetProvider @Inject constructor(
    private val icons: AppIconProvider,
) : AssetProvider {

    override suspend fun bitmap(source: ImgSource, sizePx: Int): Bitmap? = when (source) {
        is ImgSource.AppIcon -> icons.icon(source.packageName, sizePx)
        is ImgSource.File -> decode(source.path, sizePx.coerceAtMost(MAX_PHOTO_PX))
        is ImgSource.Res -> null
    }

    private suspend fun decode(path: String, targetPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private companion object {
        const val MAX_PHOTO_PX = 720
    }
}
