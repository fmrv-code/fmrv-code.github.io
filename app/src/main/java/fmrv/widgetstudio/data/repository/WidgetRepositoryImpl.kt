package fmrv.widgetstudio.data.repository

import fmrv.widgetstudio.data.db.WidgetInstanceDao
import fmrv.widgetstudio.data.db.toDomain
import fmrv.widgetstudio.data.db.toEntity
import fmrv.widgetstudio.domain.model.WidgetInstance
import fmrv.widgetstudio.domain.repository.WidgetRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class WidgetRepositoryImpl @Inject constructor(
    private val dao: WidgetInstanceDao,
) : WidgetRepository {

    /** Source de temps, remplaçable dans les tests. */
    internal var now: () -> Long = System::currentTimeMillis

    override fun observeWidgets(): Flow<List<WidgetInstance>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun get(appWidgetId: Int): WidgetInstance? = dao.get(appWidgetId)?.toDomain()

    override suspend fun save(instance: WidgetInstance) {
        require(instance.appWidgetId > 0) { "Un widget configuré doit avoir un identifiant système valide" }
        dao.upsert(instance.toEntity(updatedAt = now()))
    }

    override suspend fun delete(appWidgetId: Int) = dao.delete(appWidgetId)

    override suspend fun savePending(instance: WidgetInstance) {
        dao.upsert(instance.copy(appWidgetId = WidgetInstance.PENDING_ID).toEntity(updatedAt = now()))
    }

    override suspend fun adoptPending(appWidgetId: Int): WidgetInstance? {
        if (appWidgetId <= 0 || dao.get(appWidgetId) != null) return null
        val pending = dao.get(WidgetInstance.PENDING_ID) ?: return null
        dao.delete(WidgetInstance.PENDING_ID)
        if (now() - pending.updatedAt > PENDING_TTL_MS) return null
        val adopted = pending.toDomain().copy(appWidgetId = appWidgetId)
        dao.upsert(adopted.toEntity(updatedAt = now()))
        return adopted
    }

    override suspend fun retainOnly(appWidgetIds: Set<Int>) {
        dao.allIds().filter { it !in appWidgetIds }.forEach { dao.delete(it) }
    }

    companion object {
        /** Délai maximal entre la demande d'épinglage et la pose effective du widget. */
        const val PENDING_TTL_MS = 5 * 60 * 1000L
    }
}
