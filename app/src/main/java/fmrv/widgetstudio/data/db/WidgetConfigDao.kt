package fmrv.widgetstudio.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetConfigDao {
    @Query("SELECT * FROM widget_configs WHERE appWidgetId > 0 ORDER BY appWidgetId")
    fun observeAll(): Flow<List<WidgetConfigEntity>>

    @Query("SELECT * FROM widget_configs WHERE appWidgetId = :id")
    suspend fun get(id: Int): WidgetConfigEntity?

    @Upsert
    suspend fun upsert(entity: WidgetConfigEntity)

    @Query("DELETE FROM widget_configs WHERE appWidgetId = :id")
    suspend fun delete(id: Int)

    @Query("SELECT appWidgetId FROM widget_configs WHERE appWidgetId > 0")
    suspend fun allIds(): List<Int>
}
