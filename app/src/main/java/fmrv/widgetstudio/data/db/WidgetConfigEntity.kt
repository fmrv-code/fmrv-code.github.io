package fmrv.widgetstudio.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widget_configs")
data class WidgetConfigEntity(
    @PrimaryKey val appWidgetId: Int,
    val name: String,
    val contentType: String,
    val targetPackage: String?,
    val targetLabel: String?,
    val shape: String,
    val background: String,
    val backgroundColor: Int,
    val backgroundEnd: Int,
    val textColor: Int,
    val accentColor: Int,
    val opacity: Float,
    val font: String,
    val layout: String,
    /** Noms de packages séparés par des virgules (un nom de package ne contient jamais de virgule). */
    val packages: String,
    val label: String,
    val showLabel: Boolean,
    val use24Hour: Boolean,
    val showDate: Boolean,
    val updatedAt: Long,
)
