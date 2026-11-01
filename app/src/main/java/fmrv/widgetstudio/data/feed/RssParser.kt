package fmrv.widgetstudio.data.feed

import java.io.StringReader
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

data class RssItem(
    val title: String,
    val link: String?,
    val description: String,
    val publishedMillis: Long,
)

/** Analyse minimale d'un flux RSS 2.0 (titre, lien, description, date de publication). */
object RssParser {

    /** Retourne la liste vide si le contenu n'est pas un flux RSS exploitable. */
    fun parse(xml: String, limit: Int = 50): List<RssItem> {
        val items = ArrayList<RssItem>()
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && items.size < limit) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    readItem(parser)?.let(items::add)
                }
                event = parser.next()
            }
        } catch (e: XmlPullParserException) {
            return items
        }
        return items
    }

    private fun readItem(parser: XmlPullParser): RssItem? {
        var title = ""
        var link: String? = null
        var description = ""
        var published = 0L
        val depth = parser.depth
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) break
            if (event == XmlPullParser.END_TAG && parser.depth == depth) break
            if (event != XmlPullParser.START_TAG || parser.depth != depth + 1) continue
            when (parser.name) {
                "title" -> title = plainText(parser.nextText())
                "link" -> link = parser.nextText().trim().ifEmpty { null }
                "description" -> description = plainText(parser.nextText())
                "pubDate" -> published = parseDate(parser.nextText())
            }
        }
        if (title.isBlank()) return null
        return RssItem(title, link, description, published)
    }

    private fun parseDate(value: String): Long = try {
        ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        0L
    }

    private val tags = Regex("<[^>]*>")
    private val spaces = Regex("\\s+")
    private val numericEntity = Regex("&#(x?)([0-9a-fA-F]+);")

    /** Retire le HTML et décode les entités courantes. */
    fun plainText(html: String): String {
        val noTags = html.replace(tags, " ")
        val decoded = numericEntity.replace(noTags) { match ->
            val code = match.groupValues[2].toIntOrNull(if (match.groupValues[1].isEmpty()) 10 else 16)
            if (code != null && code in 1..0x10FFFF) String(Character.toChars(code)) else match.value
        }
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
        return decoded.replace(spaces, " ").trim()
    }
}
