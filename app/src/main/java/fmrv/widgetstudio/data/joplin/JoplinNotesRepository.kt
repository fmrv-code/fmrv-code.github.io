package fmrv.widgetstudio.data.joplin

import fmrv.widgetstudio.domain.model.FailureReason
import fmrv.widgetstudio.domain.model.NoteSummary
import fmrv.widgetstudio.domain.model.NotesSnapshot
import fmrv.widgetstudio.domain.model.RefreshResult
import fmrv.widgetstudio.domain.repository.NotesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

@Singleton
class JoplinNotesRepository @Inject constructor(
    private val remote: RemoteFiles,
    private val store: NotesCacheStore,
    private val connection: Connection,
) : NotesRepository {

    internal var now: () -> Long = System::currentTimeMillis

    /** Évite deux actualisations simultanées (worker + bouton du widget). */
    private val refreshLock = Mutex()

    override suspend fun snapshot(): NotesSnapshot {
        val cache = store.load()
        return NotesSnapshot(
            connected = connection.isConnected(),
            notes = notesOf(cache),
            refreshedAtMillis = cache.refreshedAtMillis,
        )
    }

    override suspend fun refresh(): RefreshResult = refreshLock.withLock {
        if (!connection.isConnected()) return RefreshResult.NotConnected
        try {
            val files = remote.listMarkdownFiles(WINDOW_SIZE)
            val previous = store.load().entries.associateBy { it.fileId }
            val permits = Semaphore(PARALLEL_DOWNLOADS)

            val entries = coroutineScope {
                files.map { file ->
                    async {
                        permits.withPermit {
                            val known = previous[file.id]
                            if (known != null && known.fileModified == file.modified) {
                                known // inchangé depuis la dernière fois : pas de téléchargement
                            } else {
                                CacheEntry(file.id, file.modified, toNote(remote.read(file)))
                            }
                        }
                    }
                }.awaitAll()
            }

            val cache = NotesCache(refreshedAtMillis = now(), entries = entries)
            store.save(cache)
            RefreshResult.Success(notesOf(cache).size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RemoteException) {
            RefreshResult.Failure(e.reason)
        } catch (e: Exception) {
            RefreshResult.Failure(FailureReason.UNKNOWN)
        }
    }

    override suspend fun clear() = store.clear()

    private fun toNote(text: String): CachedNote? {
        val item = JoplinItemParser.parse(text) ?: return null
        // Les conflits et les notes à la corbeille ne doivent pas apparaître.
        if (!item.isNote || item.isConflict || item.deletedMillis > 0) return null
        return CachedNote(
            id = item.id,
            title = item.title,
            snippet = JoplinItemParser.plainSnippet(item.body),
            updatedMillis = item.updatedMillis,
        )
    }

    private fun notesOf(cache: NotesCache): List<NoteSummary> = cache.entries
        .mapNotNull { it.note }
        .sortedByDescending { it.updatedMillis }
        .take(MAX_NOTES)
        .map { NoteSummary(it.id, it.title, it.snippet, it.updatedMillis) }

    companion object {
        /** Nombre de fichiers récents examinés (les dossiers et étiquettes en font partie). */
        const val WINDOW_SIZE = 80
        const val MAX_NOTES = 20
        private const val PARALLEL_DOWNLOADS = 6
    }
}
