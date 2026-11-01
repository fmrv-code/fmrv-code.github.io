package fmrv.widgetstudio.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import fmrv.widgetstudio.domain.model.AppInfo
import fmrv.widgetstudio.domain.repository.AppsSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Liste les applications lançables via une requête `<queries>` sur l'intent LAUNCHER :
 * aucune permission QUERY_ALL_PACKAGES n'est nécessaire.
 */
@Singleton
class InstalledAppsSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppsSource {

    override suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        queryLaunchers(pm)
            .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    override suspend fun labelOf(packageName: String): String? = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        try {
            applicationInfo(pm, packageName).loadLabel(pm).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun queryLaunchers(pm: PackageManager): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
    }

    private fun applicationInfo(pm: PackageManager, packageName: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
}
