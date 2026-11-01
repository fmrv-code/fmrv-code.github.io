package fmrv.widgetstudio.feed

import fmrv.widgetstudio.data.feed.RssParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RssParserTest {

    private val rss = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
          <channel>
            <title>Franceinfo</title>
            <link>https://www.francetvinfo.fr</link>
            <item>
              <title><![CDATA[Réforme : ce qu'il faut retenir]]></title>
              <link>https://www.francetvinfo.fr/politique/reforme.html</link>
              <description><![CDATA[<p>Le texte a été adopté &amp; promulgué.</p><img src="x.jpg"/>]]></description>
              <pubDate>Sun, 01 Nov 2026 09:30:00 +0100</pubDate>
              <media:content url="https://exemple.fr/a.jpg"/>
            </item>
            <item>
              <title>Sport &amp; météo : un week-end agité</title>
              <link>https://www.francetvinfo.fr/sport/meteo.html</link>
              <description>Pluie attendue &#233;t&#233; &#x27;fort&#x27;</description>
              <pubDate>invalide</pubDate>
            </item>
            <item>
              <link>https://www.francetvinfo.fr/sans-titre.html</link>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    @Test
    fun `les articles sont extraits avec titre, lien, resume et date`() {
        val items = RssParser.parse(rss)

        assertEquals(2, items.size, "l'élément sans titre est ignoré")
        assertEquals("Réforme : ce qu'il faut retenir", items[0].title)
        assertEquals("https://www.francetvinfo.fr/politique/reforme.html", items[0].link)
        assertEquals("Le texte a été adopté & promulgué.", items[0].description)
        assertEquals(1793521800000L, items[0].publishedMillis) // 09:30 +01:00 = 08:30 UTC
    }

    @Test
    fun `les entites HTML sont decodees et une date invalide vaut zero`() {
        val second = RssParser.parse(rss)[1]

        assertEquals("Sport & météo : un week-end agité", second.title)
        assertEquals("Pluie attendue été 'fort'", second.description)
        assertEquals(0L, second.publishedMillis)
    }

    @Test
    fun `la limite est respectee`() {
        val many = "<rss><channel>" + (1..30).joinToString("") { "<item><title>T$it</title></item>" } + "</channel></rss>"
        assertEquals(5, RssParser.parse(many, limit = 5).size)
    }

    @Test
    fun `un contenu qui n'est pas un flux donne une liste vide`() {
        assertTrue(RssParser.parse("").isEmpty())
        assertTrue(RssParser.parse("<html><body>Erreur 404</body></html>").isEmpty())
        assertTrue(RssParser.parse("pas du xml <<<").isEmpty())
    }

    @Test
    fun `un lien absent reste nul`() {
        assertNull(RssParser.parse("<rss><channel><item><title>Seul</title></item></channel></rss>").single().link)
    }

    @Test
    fun `plainText retire le HTML et normalise les espaces`() {
        assertEquals("Bonjour le monde", RssParser.plainText("<b>Bonjour</b>&nbsp;  le\n monde"))
    }
}
