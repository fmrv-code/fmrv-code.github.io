package fmrv.widgetstudio.kinds.feeds

import fmrv.widgetstudio.R
import fmrv.widgetstudio.kinds.RenderEnv
import java.time.Duration

/** « à l'instant », « il y a 5 min », « il y a 3 h », « il y a 2 j » : pur et testable. */
object RelativeTime {
    fun format(env: RenderEnv, thenMillis: Long): String {
        if (thenMillis <= 0L) return ""
        val minutes = Duration.ofMillis(env.now.toInstant().toEpochMilli() - thenMillis).toMinutes()
        return when {
            minutes < 1 -> env.string(R.string.time_now)
            minutes < 60 -> env.string(R.string.time_minutes_ago, minutes)
            minutes < 60 * 24 -> env.string(R.string.time_hours_ago, minutes / 60)
            else -> env.string(R.string.time_days_ago, minutes / (60 * 24))
        }
    }
}
