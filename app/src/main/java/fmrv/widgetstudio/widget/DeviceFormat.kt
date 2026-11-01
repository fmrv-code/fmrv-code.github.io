package fmrv.widgetstudio.widget

import java.util.Locale

/** Mesures de l'appareil affichées par le widget « Entretien de l'appareil ». */
data class DeviceSnapshot(
    val storageFreeBytes: Long,
    val storageTotalBytes: Long,
    val ramFreeBytes: Long,
    val ramTotalBytes: Long,
    val batteryPercent: Int,
    val charging: Boolean,
) {
    val storageUsedFraction: Float get() = DeviceFormat.usedFraction(storageFreeBytes, storageTotalBytes)
    val ramUsedFraction: Float get() = DeviceFormat.usedFraction(ramFreeBytes, ramTotalBytes)
    val batteryFraction: Float get() = (batteryPercent / 100f).coerceIn(0f, 1f)
}

object DeviceFormat {
    private const val GB = 1_000_000_000.0
    private const val MB = 1_000_000.0

    /** « 314,1 Go » (séparateur décimal selon la langue) ou « 850 Mo » sous 1 Go. */
    fun size(bytes: Long, locale: Locale = Locale.getDefault()): String =
        if (bytes >= GB) {
            String.format(locale, "%.1f Go", bytes / GB)
        } else {
            String.format(locale, "%d Mo", (bytes / MB).toLong())
        }

    /** Part utilisée (0..1) à partir de l'espace libre ; 0 si le total est inconnu. */
    fun usedFraction(free: Long, total: Long): Float =
        if (total <= 0L) 0f else (1f - free.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    /** Largeur de la partie remplie d'une jauge : jamais plus étroite que la pastille d'icône ni plus large que la place disponible. */
    fun gaugeFillDp(trackDp: Float, fraction: Float, minDp: Float, reserveDp: Float): Float {
        val max = (trackDp - reserveDp).coerceAtLeast(minDp)
        return minDp + (max - minDp) * fraction.coerceIn(0f, 1f)
    }
}
