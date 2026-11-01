package fmrv.widgetstudio.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fmrv.widgetstudio.domain.model.WidgetConfig

/**
 * Dégradé diagonal en petit bitmap (64×64, ≈ 16 Ko) étiré par le widget : léger pour le Binder
 * et suffisant pour un dégradé lisse. Les coins arrondis sont découpés par le widget lui-même.
 */
fun WidgetBackground.gradientBitmap(config: WidgetConfig): Bitmap {
    val (start, end) = colors(config)
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), start, end, Shader.TileMode.CLAMP)
    }
    Canvas(bitmap).drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
    return bitmap
}
