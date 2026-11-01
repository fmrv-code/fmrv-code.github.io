package fmrv.widgetstudio.data

import fmrv.widgetstudio.data.db.WidgetInstanceDao
import fmrv.widgetstudio.data.db.WidgetInstanceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO en mémoire qui reproduit les requêtes du vrai DAO (notamment le filtre `appWidgetId > 0`). */
class FakeWidgetInstanceDao : WidgetInstanceDao {
    private val rows = MutableStateFlow<Map<Int, WidgetInstanceEntity>>(emptyMap())

    override fun observeAll(): Flow<List<WidgetInstanceEntity>> =
        rows.map { map -> map.values.filter { it.appWidgetId > 0 }.sortedBy { it.appWidgetId } }

    override suspend fun get(id: Int): WidgetInstanceEntity? = rows.value[id]

    override suspend fun upsert(entity: WidgetInstanceEntity) {
        rows.value = rows.value + (entity.appWidgetId to entity)
    }

    override suspend fun delete(id: Int) {
        rows.value = rows.value - id
    }

    override suspend fun allIds(): List<Int> = rows.value.keys.filter { it > 0 }
}
