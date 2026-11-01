package fmrv.widgetstudio.domain

import fmrv.widgetstudio.domain.model.Params
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParamsTest {

    @Test
    fun `les valeurs absentes retombent sur le defaut`() {
        val p = Params()
        assertEquals("x", p.string("k", "x"))
        assertTrue(p.bool("k", true))
        assertEquals(7, p.int("k", 7))
        assertEquals(1.5f, p.float("k", 1.5f))
        assertEquals(emptyList(), p.list("k"))
    }

    @Test
    fun `les booleens et les listes font l'aller-retour`() {
        val p = Params().withBool("a", true).withBool("b", false).withList("apps", listOf("x.y", "z.w"))
        assertTrue(p.bool("a"))
        assertFalse(p.bool("b", true))
        assertEquals(listOf("x.y", "z.w"), p.list("apps"))
    }

    @Test
    fun `une valeur illisible retombe sur le defaut`() {
        val p = Params(mapOf("n" to "abc", "f" to "x"))
        assertEquals(3, p.int("n", 3))
        assertEquals(2f, p.float("f", 2f))
    }

    @Test
    fun `les valeurs par defaut ne remplacent jamais celles de l'utilisateur`() {
        val merged = Params(mapOf("a" to "user")).withDefaults(mapOf("a" to "default", "b" to "new"))
        assertEquals("user", merged.string("a"))
        assertEquals("new", merged.string("b"))
    }

    @Test
    fun `with et without ne modifient pas l'original`() {
        val original = Params(mapOf("a" to "1"))
        val changed = original.with("a", "2").with("b", "3")
        assertEquals("1", original.string("a"))
        assertEquals("2", changed.string("a"))
        assertEquals("", changed.without("b").string("b"))
    }

    @Test
    fun `une liste ignore les espaces et les elements vides`() {
        assertEquals(listOf("a", "b"), Params(mapOf("l" to " a, ,b ,")).list("l"))
    }
}
