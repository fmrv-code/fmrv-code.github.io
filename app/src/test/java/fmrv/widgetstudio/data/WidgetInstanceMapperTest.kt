package fmrv.widgetstudio.data

import fmrv.widgetstudio.data.db.toDomain
import fmrv.widgetstudio.data.db.toEntity
import fmrv.widgetstudio.domain.model.BackgroundStyle
import fmrv.widgetstudio.domain.model.Params
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetInstance
import fmrv.widgetstudio.domain.model.WidgetShape
import fmrv.widgetstudio.domain.model.WidgetStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetInstanceMapperTest {

    private val instance = WidgetInstance(
        appWidgetId = 12,
        kindId = "calendar.month",
        name = "Mon calendrier",
        style = WidgetStyle(
            background = BackgroundStyle.GLASS,
            colorStart = 0xFF112233.toInt(),
            colorEnd = 0xFF445566.toInt(),
            textColor = 0xFFEEDDCC.toInt(),
            accentColor = 0xFFAA5500.toInt(),
            opacity = 0.4f,
            shape = WidgetShape.PILL,
            font = WidgetFont.MONO,
        ),
        params = Params(mapOf("monday" to "1", "apps" to "a.b,c.d", "title" to "Été \"2026\"")),
    )

    @Test
    fun `l'aller-retour entite domaine conserve tous les champs`() {
        assertEquals(instance, instance.toEntity(updatedAt = 99L).toDomain())
    }

    @Test
    fun `un JSON de style illisible redonne le style par defaut`() {
        val entity = instance.toEntity(1L).copy(styleJson = "pas du json")
        assertEquals(WidgetStyle(), entity.toDomain().style)
    }

    @Test
    fun `un JSON de parametres illisible redonne des parametres vides`() {
        val entity = instance.toEntity(1L).copy(paramsJson = "{{{")
        assertEquals(Params(), entity.toDomain().params)
    }

    @Test
    fun `un champ inconnu d'une version future est ignore`() {
        val entity = instance.toEntity(1L).copy(styleJson = """{"opacity":0.5,"nouveauChamp":"x"}""")
        assertEquals(0.5f, entity.toDomain().style.opacity)
    }

    @Test
    fun `l'opacite lue en base est ramenee entre 0 et 1`() {
        assertEquals(1f, instance.toEntity(1L).copy(styleJson = """{"opacity":3.0}""").toDomain().style.opacity)
        assertEquals(0f, instance.toEntity(1L).copy(styleJson = """{"opacity":-1.0}""").toDomain().style.opacity)
    }
}
