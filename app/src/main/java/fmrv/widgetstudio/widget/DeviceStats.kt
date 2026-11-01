package fmrv.widgetstudio.widget

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs

object DeviceStatsReader {

    fun read(context: Context): DeviceSnapshot {
        val stat = StatFs(Environment.getDataDirectory().path)
        val memory = ActivityManager.MemoryInfo().also {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it)
        }
        // Intent « collant » : aucune permission ni récepteur nécessaire.
        val battery: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        return DeviceSnapshot(
            storageFreeBytes = stat.availableBlocksLong * stat.blockSizeLong,
            storageTotalBytes = stat.blockCountLong * stat.blockSizeLong,
            ramFreeBytes = memory.availMem,
            ramTotalBytes = memory.totalMem,
            batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else 0,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
        )
    }
}
