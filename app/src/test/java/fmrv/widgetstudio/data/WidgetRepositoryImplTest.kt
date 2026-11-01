package fmrv.widgetstudio.data

import fmrv.widgetstudio.data.repository.WidgetRepositoryImpl
import fmrv.widgetstudio.domain.model.WidgetInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class WidgetRepositoryImplTest {

    private var clock = 1_000_000L
    private val repository = WidgetRepositoryImpl(FakeWidgetInstanceDao()).also { it.now = { clock } }

    private fun widget(id: Int, kind: String = "clock.digital") = WidgetInstance(appWidgetId = id, kindId = kind, name = "W$id")

    @Test
    fun `save refuse un identifiant de widget invalide`() = runTest {
        assertFailsWith<IllegalArgumentException> { repository.save(widget(0)) }
        assertFailsWith<IllegalArgumentException> { repository.save(widget(-1)) }
    }

    @Test
    fun `save puis get restitue le widget`() = runTest {
        repository.save(widget(5))
        assertEquals(widget(5), repository.get(5))
        assertNull(repository.get(6))
    }

    @Test
    fun `la configuration en attente n'apparait pas dans la liste des widgets`() = runTest {
        repository.save(widget(5))
        repository.savePending(widget(0))
        assertEquals(listOf(5), repository.observeWidgets().first().map { it.appWidgetId })
    }

    @Test
    fun `adoptPending attribue la configuration au widget pose puis la consomme`() = runTest {
        repository.savePending(widget(0, kind = "calendar.month"))

        val adopted = repository.adoptPending(42)

        assertNotNull(adopted)
        assertEquals(42, adopted.appWidgetId)
        assertEquals("calendar.month", repository.get(42)?.kindId)
        assertNull(repository.adoptPending(43), "la configuration en attente ne doit servir qu'une fois")
    }

    @Test
    fun `adoptPending ignore une configuration expiree`() = runTest {
        repository.savePending(widget(0))
        clock += WidgetRepositoryImpl.PENDING_TTL_MS + 1

        assertNull(repository.adoptPending(42))
        assertNull(repository.get(42))
    }

    @Test
    fun `adoptPending ne touche pas un widget deja configure`() = runTest {
        repository.save(widget(42, kind = "existing"))
        repository.savePending(widget(0, kind = "pending"))

        assertNull(repository.adoptPending(42))
        assertEquals("existing", repository.get(42)?.kindId)
        assertNotNull(repository.adoptPending(43))
    }

    @Test
    fun `retainOnly supprime les configurations orphelines et conserve l'attente`() = runTest {
        repository.save(widget(1))
        repository.save(widget(2))
        repository.save(widget(3))
        repository.savePending(widget(0))

        repository.retainOnly(setOf(2))

        assertEquals(listOf(2), repository.observeWidgets().first().map { it.appWidgetId })
        assertNotNull(repository.adoptPending(9))
    }
}
