package fmrv.widgetstudio.feed

import fmrv.widgetstudio.data.feed.FeedFetcher
import fmrv.widgetstudio.data.feed.FeedUrls
import fmrv.widgetstudio.data.feed.ItemsCache
import fmrv.widgetstudio.data.feed.ItemsCacheStore
import fmrv.widgetstudio.data.feed.NewsRepositoryImpl
import fmrv.widgetstudio.data.joplin.RemoteException
import fmrv.widgetstudio.domain.model.FailureReason
import fmrv.widgetstudio.domain.model.RefreshResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class NewsRepositoryImplTest {

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

    private class FakeFetcher(val responses: MutableMap<String, Any>) : FeedFetcher {
        val requested = mutableListOf<String>()
        override suspend fun fetch(url: String): String {
            requested += url
            val response = responses[url] ?: throw RemoteException(FailureReason.UNKNOWN, "404")
            if (response is RemoteException) throw response
            return response as String
        }
    }

    private class FakeUrls(val urls: List<String>) : FeedUrls {
        override suspend fun candidates() = urls
    }

    private fun rss(vararg titles: String) =
        "<rss><channel>" + titles.joinToString("") { "<item><title>$it</title><link>https://x.fr/$it</link><description>Résumé $it</description></item>" } + "</channel></rss>"

    private val store = MemoryStore()

    private fun repository(fetcher: FakeFetcher, vararg urls: String) =
        NewsRepositoryImpl(fetcher, FakeUrls(urls.toList()), store).also { it.now = { 7_000L } }

    @Test
    fun `les articles sont mis en cache dans l'ordre du flux`() = runTest {
        val fetcher = FakeFetcher(mutableMapOf("https://a" to rss("Un", "Deux")))
        val repo = repository(fetcher, "https://a")

        assertEquals(RefreshResult.Success(2), repo.refresh())

        val snapshot = repo.snapshot()
        assertTrue(snapshot.connected)
        assertEquals(listOf("Un", "Deux"), snapshot.notes.map { it.title })
        assertEquals("https://x.fr/Un", snapshot.notes.first().link)
        assertEquals("Résumé Un", snapshot.notes.first().snippet)
        assertEquals(7_000L, snapshot.refreshedAtMillis)
    }

    @Test
    fun `une adresse en panne laisse la suivante prendre le relais`() = runTest {
        val fetcher = FakeFetcher(mutableMapOf("https://b" to rss("Secours")))
        val repo = repository(fetcher, "https://a", "https://b")

        assertEquals(RefreshResult.Success(1), repo.refresh())
        assertEquals(listOf("https://a", "https://b"), fetcher.requested)
    }

    @Test
    fun `une page HTML a la place du flux est ignoree`() = runTest {
        val fetcher = FakeFetcher(mutableMapOf("https://a" to "<html>Maintenance</html>", "https://b" to rss("Bon")))
        val repo = repository(fetcher, "https://a", "https://b")

        assertEquals(RefreshResult.Success(1), repo.refresh())
        assertEquals(listOf("Bon"), repo.snapshot().notes.map { it.title })
    }

    @Test
    fun `quand toutes les adresses echouent le cache existant est conserve`() = runTest {
        val fetcher = FakeFetcher(mutableMapOf("https://a" to rss("Ancien")))
        val repo = repository(fetcher, "https://a")
        repo.refresh()

        fetcher.responses["https://a"] = RemoteException(FailureReason.NETWORK)

        assertEquals(RefreshResult.Failure(FailureReason.NETWORK), repo.refresh())
        assertEquals(listOf("Ancien"), repo.snapshot().notes.map { it.title })
    }

    @Test
    fun `aucune adresse valide donne une erreur`() = runTest {
        assertEquals(
            RefreshResult.Failure(FailureReason.UNKNOWN),
            repository(FakeFetcher(mutableMapOf())).refresh(),
        )
    }

    @Test
    fun `clear efface les articles`() = runTest {
        val repo = repository(FakeFetcher(mutableMapOf("https://a" to rss("Un"))), "https://a")
        repo.refresh()
        repo.clear()
        assertTrue(repo.snapshot().notes.isEmpty())
    }
}
