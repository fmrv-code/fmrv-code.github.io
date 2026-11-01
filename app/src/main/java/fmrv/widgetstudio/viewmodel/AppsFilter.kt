package fmrv.widgetstudio.viewmodel

import fmrv.widgetstudio.domain.model.AppInfo

/** Filtre insensible à la casse sur le nom et le nom de package. */
internal fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
    val needle = query.trim()
    if (needle.isEmpty()) return apps
    return apps.filter {
        it.label.contains(needle, ignoreCase = true) || it.packageName.contains(needle, ignoreCase = true)
    }
}
