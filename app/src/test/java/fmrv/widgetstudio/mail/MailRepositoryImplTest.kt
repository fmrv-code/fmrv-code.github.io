package fmrv.widgetstudio.mail

import fmrv.widgetstudio.data.feed.ItemsCache
import fmrv.widgetstudio.data.feed.ItemsCacheStore
import fmrv.widgetstudio.data.joplin.RemoteException
import fmrv.widgetstudio.data.mail.MailMessage
import fmrv.widgetstudio.data.mail.MailRepositoryImpl
import fmrv.widgetstudio.data.mail.MailSource
import fmrv.widgetstudio.domain.model.FailureReason
import fmrv.widgetstudio.domain.model.RefreshResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class MailRepositoryImplTest {

    private class MemoryStore : ItemsCacheStore {
        var cache = ItemsCache()
        override suspend fun load() = cache
        override suspend fun save(cache: ItemsCache) {
            this.cache = cache
        }
        override suspend fun clear() {
            cache = ItemsCache()
        }
    }

    private class FakeSource : MailSource {
        var configured = true
        var messages = listOf<MailMessage>()
        var failure: RemoteException? = null
        override suspend fun isConfigured() = configured
        override suspend fun fetchUnread(limit: Int): List<MailMessage> {
            failure?.let { throw it }
            return messages
        }
    }

    private val source = FakeSource()
    private val repository = MailRepositoryImpl(source, MemoryStore()).also { it.now = { 9_000L } }

    @Test
    fun `sans identifiants rien n'est lu`() = runTest {
        source.configured = false

        assertEquals(RefreshResult.NotConnected, repository.refresh())
        assertFalse(repository.snapshot().connected)
    }

    @Test
    fun `les messages deviennent des elements objet et expediteur`() = runTest {
        source.messages = listOf(
            MailMessage("11", "Facture", "Service client", 5L),
            MailMessage("10", "  ", "Alice", 4L),
        )

        assertEquals(RefreshResult.Success(2), repository.refresh())

        val snapshot = repository.snapshot()
        assertTrue(snapshot.connected)
        assertEquals(listOf("Facture", "(sans objet)"), snapshot.notes.map { it.title })
        assertEquals(listOf("Service client", "Alice"), snapshot.notes.map { it.snippet })
        assertEquals(9_000L, snapshot.refreshedAtMillis)
    }

    @Test
    fun `une erreur d'authentification est signalee et le cache precedent reste`() = runTest {
        source.messages = listOf(MailMessage("1", "Garde-moi", "X", 1L))
        repository.refresh()

        source.failure = RemoteException(FailureReason.AUTH)

        assertEquals(RefreshResult.Failure(FailureReason.AUTH), repository.refresh())
        assertEquals(listOf("Garde-moi"), repository.snapshot().notes.map { it.title })
    }

    @Test
    fun `clear efface les messages`() = runTest {
        source.messages = listOf(MailMessage("1", "Secret", "X", 1L))
        repository.refresh()

        repository.clear()

        assertTrue(repository.snapshot().notes.isEmpty())
    }
}
