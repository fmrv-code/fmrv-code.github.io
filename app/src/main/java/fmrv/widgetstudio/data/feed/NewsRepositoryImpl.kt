package fmrv.widgetstudio.data.feed

import fmrv.widgetstudio.data.joplin.HttpClient
import fmrv.widgetstudio.data.joplin.RemoteException
import fmrv.widgetstudio.domain.model.FailureReason
import fmrv.widgetstudio.domain.model.NoteSummary
import fmrv.widgetstudio.domain.model.NotesSnapshot
import fmrv.widgetstudio.domain.model.RefreshResult
import fmrv.widgetstudio.domain.repository.NewsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface FeedFetcher {
    suspend fun fetch(url: String): String
}

@Singleton
class HttpFeedFetcher @Inject constructor(private val http: HttpClient) : FeedFetcher {
    override suspend fun fetch(url: String): String {
        val response = http.get(url)
        if (!response.isSuccess) throw RemoteException(FailureReason.UNKNOWN, "HTTP ${response.code}")
        return response.body
    }
}

/** Adresses du flux à essayer, dans l'ordre (celle de l'utilisateur d'abord). */
interface FeedUrls {
    suspend fun candidates(): List<String>
}

class NewsRepositoryImpl(
    private val fetcher: FeedFetcher,
    private val urls: FeedUrls,
    private val store: ItemsCacheStore,
) : NewsRepository {

    @Inject
    constructor(fetcher: FeedFetcher, urls: FeedUrls, store: NewsCacheStore) :
        this(fetcher, urls, store as ItemsCacheStore)

    internal var now: () -> Long = System::currentTimeMillis

    private val refreshLock = Mutex()

    override suspend fun snapshot(): NotesSnapshot {
        val cache = store.load()
        return NotesSnapshot(
            connected = true, // aucun compte : toujours « connecté »
            notes = cache.items.map { NoteSummary(it.id, it.title, it.snippet, it.updatedMillis, it.link) },
            refreshedAtMillis = cache.refreshedAtMillis,
        )
    }

    override suspend fun refresh(): RefreshResult = refreshLock.withLock {
        var failure = FailureReason.UNKNOWN
        for (url in urls.candidates()) {
            try {
                val items = RssParser.parse(fetcher.fetch(url), MAX_ITEMS)
                if (items.isEmpty()) continue
                val cached = items.map {
                    CachedItem(
                        id = it.link ?: it.title,
                        title = it.title,
                        snippet = snippet(it.description),
                        updatedMillis = it.publishedMillis,
                        link = it.link,
                    )
                }
                store.save(ItemsCache(now(), cached))
                return RefreshResult.Success(cached.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RemoteException) {
                failure = e.reason
            } catch (e: Exception) {
                failure = FailureReason.UNKNOWN
            }
        }
        RefreshResult.Failure(failure)
    }

    override suspend fun clear() = store.clear()

    private fun snippet(text: String): String =
        if (text.length <= SNIPPET_MAX) text else text.take(SNIPPET_MAX - 1).trimEnd() + "…"

    companion object {
        const val MAX_ITEMS = 20
        private const val SNIPPET_MAX = 140
    }
}
