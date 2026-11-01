package fmrv.widgetstudio.data

import fmrv.widgetstudio.data.db.WidgetConfigEntity
import fmrv.widgetstudio.data.db.toDomain
import fmrv.widgetstudio.data.db.toEntity
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetShape
import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetConfigMapperTest {

    private val config = WidgetConfig(
        appWidgetId = 12,
        name = "Horloge",
        contentType = WidgetContentType.CLOCK_DATE,
        targetPackage = "com.example.clock",
        targetLabel = "Clock",
        shape = WidgetShape.PILL,
        backgroundColor = 0xFF112233.toInt(),
        textColor = 0xFFEEDDCC.toInt(),
        opacity = 0.4f,
        font = WidgetFont.MONO,
        showLabel = false,
        use24Hour = false,
        showDate = false,
    )

    @Test
    fun `l'aller-retour entite domaine conserve tous les champs`() {
        assertEquals(config, config.toEntity(updatedAt = 99L).toDomain())
    }

    @Test
    fun `une valeur d'enumeration inconnue retombe sur la valeur par defaut`() {
        val entity = config.toEntity(updatedAt = 1L).copy(
            contentType = "FUTURE_TYPE",
            shape = "HEXAGON",
            font = "COMIC",
        )
        val restored = entity.toDomain()
        assertEquals(WidgetContentType.APP_SHORTCUT, restored.contentType)
        assertEquals(WidgetShape.ROUNDED, restored.shape)
        assertEquals(WidgetFont.SANS, restored.font)
    }

    @Test
    fun `l'opacite lue en base est ramenee entre 0 et 1`() {
        val tooHigh: WidgetConfigEntity = config.toEntity(1L).copy(opacity = 3f)
        val tooLow: WidgetConfigEntity = config.toEntity(1L).copy(opacity = -1f)
        assertEquals(1f, tooHigh.toDomain().opacity)
        assertEquals(0f, tooLow.toDomain().opacity)
    }
}
