package fmrv.widgetstudio.data.apps

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.LruCache
import androidx.core.graphics.createBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import fmrv.widgetstudio.domain.repository.AppIconProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Charge l'icône d'une application et la découpe en carré arrondi.
 * La taille est bornée pour rester sous la limite mémoire des RemoteViews (Binder ≈ 1 Mo).
 */
@Singleton
class AppIconLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppIconProvider {

    private val cache = LruCache<String, Bitmap>(CACHE_ENTRIES)

    override suspend fun icon(packageName: String, sizePx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val size = sizePx.coerceIn(MIN_SIZE_PX, MAX_SIZE_PX)
        val key = "$packageName@$size"
        cache.get(key)?.let { return@withContext it }
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = createBitmap(size, size)
            val canvas = Canvas(bitmap)
            val radius = size * CORNER_RATIO
            val clip = Path().apply {
                addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, Path.Direction.CW)
            }
            canvas.clipPath(clip)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            cache.put(key, bitmap)
            bitmap
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private companion object {
        const val CACHE_ENTRIES = 64
        const val MIN_SIZE_PX = 24
        const val MAX_SIZE_PX = 256
        const val CORNER_RATIO = 0.24f
    }
}
