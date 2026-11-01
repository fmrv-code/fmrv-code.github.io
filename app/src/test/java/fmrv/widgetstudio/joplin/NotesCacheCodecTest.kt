package fmrv.widgetstudio.joplin

import fmrv.widgetstudio.data.joplin.CacheEntry
import fmrv.widgetstudio.data.joplin.CachedNote
import fmrv.widgetstudio.data.joplin.NotesCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class NotesCacheCodecTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `le cache fait l'aller-retour JSON`() {
        val cache = NotesCache(
            refreshedAtMillis = 42L,
            entries = listOf(
                CacheEntry("f1", "2024-01-01T00:00:00Z", CachedNote("n1", "Titre", "Extrait", 1L)),
                CacheEntry("f2", "2024-01-02T00:00:00Z", null),
            ),
        )
        assertEquals(cache, json.decodeFromString<NotesCache>(json.encodeToString(NotesCache.serializer(), cache)))
    }

    @Test
    fun `un cache issu d'une version future reste lisible`() {
        val future = """{"refreshedAtMillis":7,"entries":[],"newField":"x"}"""
        assertEquals(7L, json.decodeFromString<NotesCache>(future).refreshedAtMillis)
    }
}
