package fmrv.widgetstudio.joplin

import fmrv.widgetstudio.data.joplin.Connection
import fmrv.widgetstudio.data.joplin.JoplinNotesRepository
import fmrv.widgetstudio.data.joplin.NotesCache
import fmrv.widgetstudio.data.joplin.NotesCacheStore
import fmrv.widgetstudio.data.joplin.RemoteException
import fmrv.widgetstudio.data.joplin.RemoteFile
import fmrv.widgetstudio.data.joplin.RemoteFiles
import fmrv.widgetstudio.domain.model.FailureReason
import fmrv.widgetstudio.domain.model.RefreshResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class JoplinNotesRepositoryTest {

    private class FakeRemote : RemoteFiles {
        val files = mutableListOf<Pair<RemoteFile, String>>()
        var failure: RemoteException? = null
        val reads = mutableListOf<String>()

        fun put(id: String, modified: String, text: String) {
            files.removeAll { it.first.id == id }
            files += RemoteFile(id, "$id.md", modified) to text
        }

        override suspend fun listMarkdownFiles(limit: Int): List<RemoteFile> {
            failure?.let { throw it }
            return files.map { it.first }.sortedByDescending { it.modified }.take(limit)
        }

        override suspend fun read(file: RemoteFile): String {
            reads += file.id
            return files.first { it.first.id == file.id }.second
        }
    }

    private class MemoryStore : NotesCacheStore {
        var cache = NotesCache()
        override suspend fun load() = cache
        override suspend fun save(cache: NotesCache) {
            this.cache = cache
        }
        override suspend fun clear() {
            cache = NotesCache()
        }
    }

    private class FakeConnection(var connected: Boolean = true) : Connection {
        override suspend fun isConnected() = connected
    }

    private val remote = FakeRemote()
    private val store = MemoryStore()
    private val connection = FakeConnection()
    private val repository = JoplinNotesRepository(remote, store, connection).also { it.now = { 5_000L } }

    private fun noteText(id: String, title: String, body: String, updated: String, extra: String = "") =
        "$title\n\n$body\n\nid: $id\nupdated_time: $updated\nis_conflict: 0\n$extra" + "type_: 1"

    @Test
    fun `sans connexion rien n'est lu`() = runTest {
        connection.connected = false

        assertEquals(RefreshResult.NotConnected, repository.refresh())
        assertFalse(repository.snapshot().connected)
        assertTrue(remote.reads.isEmpty())
    }

    @Test
    fun `les notes sont triees de la plus recente a la plus ancienne`() = runTest {
        remote.put("a", "2024-01-01T00:00:00Z", noteText("a", "Ancienne", "x", "2024-01-01T00:00:00.000Z"))
        remote.put("b", "2024-03-01T00:00:00Z", noteText("b", "Récente", "y", "2024-03-01T00:00:00.000Z"))

        assertEquals(RefreshResult.Success(2), repository.refresh())

        val snapshot = repository.snapshot()
        assertTrue(snapshot.connected)
        assertEquals(listOf("Récente", "Ancienne"), snapshot.notes.map { it.title })
        assertEquals(5_000L, snapshot.refreshedAtMillis)
    }

    @Test
    fun `les carnets, conflits, notes supprimees et fichiers etrangers sont ignores`() = runTest {
        remote.put("n", "2024-03-01T00:00:00Z", noteText("n", "Valide", "ok", "2024-03-01T00:00:00.000Z"))
        remote.put("f", "2024-03-02T00:00:00Z", "Carnet\n\nid: f\nupdated_time: 2024-03-02T00:00:00.000Z\ntype_: 2")
        remote.put("c", "2024-03-03T00:00:00Z", noteText("c", "Conflit", "x", "2024-03-03T00:00:00.000Z").replace("is_conflict: 0", "is_conflict: 1"))
        remote.put("d", "2024-03-04T00:00:00Z", noteText("d", "Corbeille", "x", "2024-03-04T00:00:00.000Z", extra = "deleted_time: 2024-03-05T00:00:00.000Z\n"))
        remote.put("x", "2024-03-05T00:00:00Z", "pas du tout du Joplin")

        assertEquals(RefreshResult.Success(1), repository.refresh())
        assertEquals(listOf("Valide"), repository.snapshot().notes.map { it.title })
    }

    @Test
    fun `les fichiers inchanges ne sont pas retelecharges, les modifies le sont`() = runTest {
        remote.put("a", "2024-01-01T00:00:00Z", noteText("a", "A", "x", "2024-01-01T00:00:00.000Z"))
        remote.put("f", "2024-01-02T00:00:00Z", "Carnet\n\nid: f\nupdated_time: 2024-01-02T00:00:00.000Z\ntype_: 2")
        repository.refresh()
        remote.reads.clear()

        repository.refresh()
        assertTrue(remote.reads.isEmpty(), "rien n'a changé : aucun téléchargement attendu")

        remote.put("a", "2024-02-01T00:00:00Z", noteText("a", "A modifiée", "x", "2024-02-01T00:00:00.000Z"))
        repository.refresh()
        assertEquals(listOf("a"), remote.reads)
        assertEquals(listOf("A modifiée"), repository.snapshot().notes.map { it.title })
    }

    @Test
    fun `l'extrait est du texte brut`() = runTest {
        remote.put("a", "2024-01-01T00:00:00Z", noteText("a", "T", "# Plan\n- **Un** [lien](http://x)", "2024-01-01T00:00:00.000Z"))
        repository.refresh()

        assertEquals("Plan Un lien", repository.snapshot().notes.single().snippet)
    }

    @Test
    fun `une erreur distante est traduite sans perdre le cache existant`() = runTest {
        remote.put("a", "2024-01-01T00:00:00Z", noteText("a", "Garde-moi", "x", "2024-01-01T00:00:00.000Z"))
        repository.refresh()

        remote.failure = RemoteException(FailureReason.NETWORK)
        assertEquals(RefreshResult.Failure(FailureReason.NETWORK), repository.refresh())

        remote.failure = RemoteException(FailureReason.FOLDER_NOT_FOUND)
        assertEquals(RefreshResult.Failure(FailureReason.FOLDER_NOT_FOUND), repository.refresh())

        assertEquals(listOf("Garde-moi"), repository.snapshot().notes.map { it.title })
    }

    @Test
    fun `clear efface les notes`() = runTest {
        remote.put("a", "2024-01-01T00:00:00Z", noteText("a", "A", "x", "2024-01-01T00:00:00.000Z"))
        repository.refresh()

        repository.clear()

        assertTrue(repository.snapshot().notes.isEmpty())
    }

    @Test
    fun `le nombre de notes affichees est plafonne`() = runTest {
        repeat(30) { i ->
            val id = "n%02d".format(i)
            remote.put(id, "2024-01-%02dT00:00:00Z".format(i + 1), noteText(id, "Note $i", "x", "2024-01-%02dT00:00:00.000Z".format(i + 1)))
        }

        repository.refresh()

        val notes = repository.snapshot().notes
        assertEquals(JoplinNotesRepository.MAX_NOTES, notes.size)
        assertEquals("Note 29", notes.first().title)
    }
}
