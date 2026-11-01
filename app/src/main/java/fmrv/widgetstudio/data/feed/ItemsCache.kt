package fmrv.widgetstudio.data.feed

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
data class CachedItem(
    val id: String,
    val title: String,
    val snippet: String,
    val updatedMillis: Long,
    val link: String? = null,
)

@Serializable
data class ItemsCache(
    val refreshedAtMillis: Long = 0L,
    val items: List<CachedItem> = emptyList(),
)

interface ItemsCacheStore {
    suspend fun load(): ItemsCache

    suspend fun save(cache: ItemsCache)

    suspend fun clear()
}

/** Cache JSON dans le stockage privé de l'application (un fichier par source). */
open class FileItemsCacheStore(
    private val context: Context,
    private val json: Json,
    private val fileName: String,
) : ItemsCacheStore {

    private val mutex = Mutex()
    private val file: File get() = File(context.filesDir, fileName)

    override suspend fun load(): ItemsCache = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) json.decodeFromString<ItemsCache>(file.readText()) else ItemsCache()
            } catch (e: Exception) {
                ItemsCache() // cache corrompu : il se reconstruira à la prochaine actualisation
            }
        }
    }

    override suspend fun save(cache: ItemsCache) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val tmp = File(context.filesDir, "$fileName.tmp")
                tmp.writeText(json.encodeToString(ItemsCache.serializer(), cache))
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
}

@Singleton
class NewsCacheStore @Inject constructor(
    @ApplicationContext context: Context,
    json: Json,
) : FileItemsCacheStore(context, json, "news_cache.json")

@Singleton
class MailCacheStore @Inject constructor(
    @ApplicationContext context: Context,
    json: Json,
) : FileItemsCacheStore(context, json, "mail_cache.json")
