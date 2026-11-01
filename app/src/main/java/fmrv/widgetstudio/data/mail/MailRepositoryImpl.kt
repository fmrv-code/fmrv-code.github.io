package fmrv.widgetstudio.data.mail

import fmrv.widgetstudio.data.feed.CachedItem
import fmrv.widgetstudio.data.feed.ItemsCache
import fmrv.widgetstudio.data.feed.ItemsCacheStore
import fmrv.widgetstudio.data.feed.MailCacheStore
import fmrv.widgetstudio.data.joplin.RemoteException
import fmrv.widgetstudio.domain.model.FailureReason
import fmrv.widgetstudio.domain.model.NoteSummary
import fmrv.widgetstudio.domain.model.NotesSnapshot
import fmrv.widgetstudio.domain.model.RefreshResult
import fmrv.widgetstudio.domain.repository.MailRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MailRepositoryImpl(
    private val source: MailSource,
    private val store: ItemsCacheStore,
) : MailRepository {

    @Inject
    constructor(source: MailSource, store: MailCacheStore) : this(source, store as ItemsCacheStore)

    internal var now: () -> Long = System::currentTimeMillis

    private val refreshLock = Mutex()

    override suspend fun snapshot(): NotesSnapshot {
        val cache = store.load()
        return NotesSnapshot(
            connected = source.isConfigured(),
            notes = cache.items.map { NoteSummary(it.id, it.title, it.snippet, it.updatedMillis, it.link) },
            refreshedAtMillis = cache.refreshedAtMillis,
        )
    }

    override suspend fun refresh(): RefreshResult = refreshLock.withLock {
        if (!source.isConfigured()) return RefreshResult.NotConnected
        try {
            val messages = source.fetchUnread(MAX_MESSAGES)
            val items = messages.map {
                CachedItem(
                    id = it.uid,
                    title = it.subject.ifBlank { NO_SUBJECT },
                    snippet = it.from,
                    updatedMillis = it.receivedMillis,
                )
            }
            store.save(ItemsCache(now(), items))
            RefreshResult.Success(items.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RemoteException) {
            RefreshResult.Failure(e.reason)
        } catch (e: Exception) {
            RefreshResult.Failure(FailureReason.UNKNOWN)
        }
    }

    override suspend fun clear() = store.clear()

    companion object {
        const val MAX_MESSAGES = 20
        private const val NO_SUBJECT = "(sans objet)"
    }
}
