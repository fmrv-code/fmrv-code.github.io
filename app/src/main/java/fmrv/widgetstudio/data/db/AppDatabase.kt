package fmrv.widgetstudio.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WidgetInstanceEntity::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun widgetInstanceDao(): WidgetInstanceDao
}
