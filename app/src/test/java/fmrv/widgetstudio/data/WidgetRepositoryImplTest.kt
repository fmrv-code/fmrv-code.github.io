package fmrv.widgetstudio.data

import fmrv.widgetstudio.data.repository.WidgetRepositoryImpl
import fmrv.widgetstudio.domain.model.WidgetConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class WidgetRepositoryImplTest {

    private var clock = 1_000_000L
    private val repository = WidgetRepositoryImpl(FakeWidgetConfigDao()).also { it.now = { clock } }

    private fun config(id: Int, pkg: String = "com.example.app") =
        WidgetConfig(appWidgetId = id, name = "W$id", targetPackage = pkg, targetLabel = "App")

    @Test
    fun `save refuse un identifiant de widget invalide`() = runTest {
        assertFailsWith<IllegalArgumentException> { repository.save(config(0)) }
        assertFailsWith<IllegalArgumentException> { repository.save(config(-1)) }
    }

    @Test
    fun `save puis get restitue la configuration`() = runTest {
        repository.save(config(5))
        assertEquals(config(5), repository.get(5))
        assertNull(repository.get(6))
    }

    @Test
    fun `la configuration en attente n'apparait pas dans la liste des widgets`() = runTest {
        repository.save(config(5))
        repository.savePending(config(0))
        assertEquals(listOf(5), repository.observeWidgets().first().map { it.appWidgetId })
    }

    @Test
    fun `adoptPending attribue la configuration au widget pose puis la consomme`() = runTest {
        repository.savePending(config(0, pkg = "com.example.pinned"))

        val adopted = repository.adoptPending(42)

        assertNotNull(adopted)
        assertEquals(42, adopted.appWidgetId)
        assertEquals("com.example.pinned", repository.get(42)?.targetPackage)
        assertNull(repository.adoptPending(43), "la configuration en attente ne doit servir qu'une fois")
    }

    @Test
    fun `adoptPending ignore une configuration expiree`() = runTest {
        repository.savePending(config(0))
        clock += WidgetRepositoryImpl.PENDING_TTL_MS + 1

        assertNull(repository.adoptPending(42))
        assertNull(repository.get(42))
    }

    @Test
    fun `adoptPending ne touche pas un widget deja configure`() = runTest {
        repository.save(config(42, pkg = "com.example.existing"))
        repository.savePending(config(0, pkg = "com.example.pinned"))

        assertNull(repository.adoptPending(42))
        assertEquals("com.example.existing", repository.get(42)?.targetPackage)
        // La configuration en attente reste disponible pour le widget suivant.
        assertNotNull(repository.adoptPending(43))
    }

    @Test
    fun `adoptPending refuse un identifiant invalide`() = runTest {
        repository.savePending(config(0))
        assertNull(repository.adoptPending(0))
        assertNull(repository.adoptPending(-1))
    }

    @Test
    fun `retainOnly supprime les configurations orphelines et conserve l'attente`() = runTest {
        repository.save(config(1))
        repository.save(config(2))
        repository.save(config(3))
        repository.savePending(config(0))

        repository.retainOnly(setOf(2))

        assertEquals(listOf(2), repository.observeWidgets().first().map { it.appWidgetId })
        assertNotNull(repository.adoptPending(9))
    }

    @Test
    fun `delete retire le widget`() = runTest {
        repository.save(config(7))
        repository.delete(7)
        assertNull(repository.get(7))
    }
}
