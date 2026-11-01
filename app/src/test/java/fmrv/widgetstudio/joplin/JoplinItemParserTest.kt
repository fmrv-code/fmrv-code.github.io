package fmrv.widgetstudio.joplin

import fmrv.widgetstudio.data.joplin.JoplinItemParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JoplinItemParserTest {

    private val note = """
        Liste de courses

        - Pain
        - Lait

        Ne pas oublier le café

        id: 0123456789abcdef0123456789abcdef
        parent_id: fedcba9876543210fedcba9876543210
        created_time: 2024-03-01T10:00:00.000Z
        updated_time: 2024-03-02T08:30:00.000Z
        is_conflict: 0
        latitude: 0.00000000
        author: 
        source_url: 
        is_todo: 0
        deleted_time: 0
        type_: 1
    """.trimIndent()

    @Test
    fun `une note est analysee avec son titre, son corps et ses metadonnees`() {
        val item = assertNotNull(JoplinItemParser.parse(note))

        assertEquals("0123456789abcdef0123456789abcdef", item.id)
        assertTrue(item.isNote)
        assertEquals("Liste de courses", item.title)
        assertEquals("- Pain\n- Lait\n\nNe pas oublier le café", item.body)
        assertEquals(1709368200000L, item.updatedMillis)
        assertFalse(item.isConflict)
        assertEquals(0L, item.deletedMillis)
    }

    @Test
    fun `les fins de ligne Windows sont acceptees`() {
        val item = assertNotNull(JoplinItemParser.parse(note.replace("\n", "\r\n")))
        assertEquals("Liste de courses", item.title)
        assertEquals(1, item.type)
    }

    @Test
    fun `un carnet n'est pas une note`() {
        val folder = """
            Travail

            id: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
            parent_id: 
            updated_time: 2024-03-02T08:30:00.000Z
            type_: 2
        """.trimIndent()

        val item = assertNotNull(JoplinItemParser.parse(folder))

        assertFalse(item.isNote)
        assertEquals("Travail", item.title)
        assertEquals("", item.body)
    }

    @Test
    fun `une note vide garde son titre`() {
        val empty = "Sans contenu\n\nid: bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\nupdated_time: 2024-01-01T00:00:00.000Z\ntype_: 1"
        val item = assertNotNull(JoplinItemParser.parse(empty))
        assertEquals("Sans contenu", item.title)
        assertEquals("", item.body)
    }

    @Test
    fun `un conflit et une note supprimee sont signales`() {
        val conflict = note.replace("is_conflict: 0", "is_conflict: 1")
        assertTrue(assertNotNull(JoplinItemParser.parse(conflict)).isConflict)

        val deleted = note.replace("deleted_time: 0", "deleted_time: 2024-03-05T00:00:00.000Z")
        assertTrue(assertNotNull(JoplinItemParser.parse(deleted)).deletedMillis > 0)
    }

    @Test
    fun `un texte qui n'est pas un element Joplin est refuse`() {
        assertNull(JoplinItemParser.parse(""))
        assertNull(JoplinItemParser.parse("Juste du texte"))
        assertNull(JoplinItemParser.parse("Titre\n\nCorps sans metadonnees"))
        assertNull(JoplinItemParser.parse("Titre\n\nid: abc\nupdated_time: 2024-01-01T00:00:00.000Z"))
    }

    @Test
    fun `un corps contenant des lignes cle-valeur ne perturbe pas l'analyse`() {
        val tricky = "Recette\n\nnote: ceci est du texte\n\nid: cccccccccccccccccccccccccccccccc\ntype_: 1"
        val item = assertNotNull(JoplinItemParser.parse(tricky))
        assertEquals("Recette", item.title)
        assertEquals("note: ceci est du texte", item.body)
    }

    @Test
    fun `l'extrait retire la syntaxe Markdown`() {
        val markdown = "# Titre\n\n- [ ] Tâche **importante**\n- Voir [le site](https://exemple.fr)\n![img](:/abc)"
        assertEquals("Titre Tâche importante Voir le site", JoplinItemParser.plainSnippet(markdown))
    }

    @Test
    fun `l'extrait est tronque avec des points de suspension`() {
        val snippet = JoplinItemParser.plainSnippet("mot ".repeat(100), maxLength = 20)
        assertEquals(20, snippet.length)
        assertTrue(snippet.endsWith("…"))
    }
}
