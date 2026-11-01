package fmrv.widgetstudio.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetInstanceDao {
    @Query("SELECT * FROM widget_instances WHERE appWidgetId > 0 ORDER BY appWidgetId")
    fun observeAll(): Flow<List<WidgetInstanceEntity>>

    @Query("SELECT * FROM widget_instances WHERE appWidgetId = :id")
    suspend fun get(id: Int): WidgetInstanceEntity?

    @Upsert
    suspend fun upsert(entity: WidgetInstanceEntity)

    @Query("DELETE FROM widget_instances WHERE appWidgetId = :id")
    suspend fun delete(id: Int)

    @Query("SELECT appWidgetId FROM widget_instances WHERE appWidgetId > 0")
    suspend fun allIds(): List<Int>
}
