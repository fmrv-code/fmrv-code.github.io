package fmrv.widgetstudio.data.joplin

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CachedNote(
    val id: String,
    val title: String,
    val snippet: String,
    val updatedMillis: Long,
)

/**
 * Une entrée par fichier distant récemment modifié. [note] est `null` pour les fichiers qui ne sont pas des
 * notes (dossiers, étiquettes…) : on les mémorise pour ne pas les retélécharger à chaque actualisation.
 */
@Serializable
data class CacheEntry(
    val fileId: String,
    val fileModified: String,
    val note: CachedNote? = null,
)

@Serializable
data class NotesCache(
    val refreshedAtMillis: Long = 0L,
    val entries: List<CacheEntry> = emptyList(),
)

interface NotesCacheStore {
    suspend fun load(): NotesCache

    suspend fun save(cache: NotesCache)

    suspend fun clear()
}

/** Cache stocké en JSON dans le stockage privé de l'application. */
@Singleton
class FileNotesCacheStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) : NotesCacheStore {

    private val mutex = Mutex()
    private val file: File get() = File(context.filesDir, FILE_NAME)

    override suspend fun load(): NotesCache = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) json.decodeFromString<NotesCache>(file.readText()) else NotesCache()
            } catch (e: Exception) {
                NotesCache() // cache corrompu : on repart de zéro, il se reconstruira
            }
        }
    }

    override suspend fun save(cache: NotesCache) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val tmp = File(context.filesDir, "$FILE_NAME.tmp")
                tmp.writeText(json.encodeToString(NotesCache.serializer(), cache))
                if (!tmp.renameTo(file)) {
                    file.writeText(tmp.readText())
                    tmp.delete()
                }
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock { withContext(Dispatchers.IO) { file.delete() } }
    }

    private companion object {
        const val FILE_NAME = "joplin_notes_cache.json"
    }
}
