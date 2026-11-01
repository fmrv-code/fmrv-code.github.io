package fmrv.widgetstudio.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Un widget configuré. Style et paramètres sont stockés en JSON : un nouveau type de widget n'impose aucune migration. */
@Entity(tableName = "widget_instances")
data class WidgetInstanceEntity(
    @PrimaryKey val appWidgetId: Int,
    val kindId: String,
    val name: String,
    val styleJson: String,
    val paramsJson: String,
    val updatedAt: Long,
)
