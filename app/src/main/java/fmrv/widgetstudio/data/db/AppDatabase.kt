package fmrv.widgetstudio.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WidgetConfigEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun widgetConfigDao(): WidgetConfigDao
}
