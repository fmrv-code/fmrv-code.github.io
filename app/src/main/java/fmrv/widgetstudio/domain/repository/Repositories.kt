package fmrv.widgetstudio.domain.repository

import android.graphics.Bitmap
import fmrv.widgetstudio.domain.model.AppInfo
import fmrv.widgetstudio.domain.model.PinResult
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import kotlinx.coroutines.flow.Flow

interface WidgetRepository {
    /** Widgets actuellement configurés (hors configuration en attente). */
    fun observeWidgets(): Flow<List<WidgetConfig>>

    suspend fun get(appWidgetId: Int): WidgetConfig?

    suspend fun save(config: WidgetConfig)

    suspend fun delete(appWidgetId: Int)

    /** Mémorise la configuration à attribuer au prochain widget épinglé. */
    suspend fun savePending(config: WidgetConfig)

    /**
     * Attribue la configuration en attente (si elle existe et n'a pas expiré) au widget [appWidgetId].
     * Retourne `null` si ce widget est déjà configuré ou s'il n'y a rien à attribuer.
     */
    suspend fun adoptPending(appWidgetId: Int): WidgetConfig?

    /** Supprime les configurations dont le widget n'existe plus sur l'écran d'accueil. */
    suspend fun retainOnly(appWidgetIds: Set<Int>)
}

interface AppsSource {
    /** Applications lançables installées, triées par nom. */
    suspend fun loadApps(): List<AppInfo>

    suspend fun labelOf(packageName: String): String?
}

interface AppIconProvider {
    /** Icône carrée aux coins arrondis, ou `null` si l'application n'est plus installée. */
    suspend fun icon(packageName: String, sizePx: Int): Bitmap?
}

interface WidgetPinner {
    fun requestPin(size: WidgetSizeOption): PinResult
}

interface WidgetUpdater {
    suspend fun refresh(appWidgetId: Int)

    suspend fun refreshAll()
}
