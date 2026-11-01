package fmrv.widgetstudio.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeTest {

    private fun text(s: String) = Text(s, 12f, 0xFF000000.toInt())

    @Test
    fun `les constructeurs assemblent l'arbre dans l'ordre`() {
        val tree = column(h = HAlign.CENTER) {
            +text("a")
            +row { +text("b"); +text("c") }
            +null
        }
        assertEquals(2, tree.children.size)
        assertEquals(HAlign.CENTER, tree.horizontal)
        assertEquals(listOf("a", "b", "c"), tree.walk().filterIsInstance<Text>().map { it.text }.toList())
    }

    @Test
    fun `walk parcourt aussi les listes defilantes`() {
        val tree = box { +ScrollList(listOf(text("x"), text("y"))) }
        assertEquals(2, tree.walk().filterIsInstance<Text>().count())
    }

    @Test
    fun `les images a charger sont dedoublonnees avec leur plus grande taille`() {
        val tree = column {
            +Img(ImgSource.AppIcon("a.b"), 40f, 40f)
            +Img(ImgSource.AppIcon("a.b"), 64f, 64f)
            +Img(ImgSource.File("/p.jpg"), 200f, 100f)
            +Img(ImgSource.Res(1), 24f, 24f)
        }
        val requests = tree.imageRequests()
        assertEquals(2, requests.size)
        assertEquals(64f, requests[ImgSource.AppIcon("a.b")])
        assertEquals(200f, requests[ImgSource.File("/p.jpg")])
        assertTrue(ImgSource.Res(1) !in requests)
    }

    @Test
    fun `les marges et modificateurs ont des valeurs neutres par defaut`() {
        assertEquals(Pad.None, Mod().pad)
        assertEquals(Pad(5f, 5f, 5f, 5f), Pad.all(5f))
        assertEquals(Pad(3f, 1f, 3f, 1f), Pad.sym(3f, 1f))
    }
}
