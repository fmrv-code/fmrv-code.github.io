package fmrv.widgetstudio.data.joplin

import java.time.Instant

/**
 * Élément Joplin synchronisé en fichier Markdown. Format :
 *
 * ```
 * Titre
 *
 * Corps…
 *
 * id: …
 * parent_id: …
 * updated_time: 2024-01-01T00:00:00.000Z
 * type_: 1
 * ```
 */
data class JoplinItem(
    val id: String,
    val type: Int,
    val title: String,
    val body: String,
    val updatedMillis: Long,
    val isConflict: Boolean,
    val deletedMillis: Long,
) {
    val isNote: Boolean get() = type == TYPE_NOTE

    companion object {
        const val TYPE_NOTE = 1
    }
}

object JoplinItemParser {

    /** Retourne `null` si le texte n'est pas un élément Joplin valide (ex. fichier étranger). */
    fun parse(text: String): JoplinItem? {
        val lines = text.replace("\r\n", "\n").trimEnd().split('\n')

        // Les métadonnées sont le bloc de lignes non vides situé tout à la fin, séparé du contenu par une ligne vide.
        var metaStart = lines.size
        while (metaStart > 0 && lines[metaStart - 1].isNotEmpty()) metaStart--
        if (metaStart == 0 || metaStart == lines.size) return null

        val meta = HashMap<String, String>()
        for (line in lines.subList(metaStart, lines.size)) {
            val colon = line.indexOf(':')
            if (colon <= 0) return null
            meta[line.substring(0, colon).trim()] = line.substring(colon + 1).trim()
        }
        val id = meta["id"]?.takeIf { it.isNotEmpty() } ?: return null
        val type = meta["type_"]?.toIntOrNull() ?: return null

        val content = lines.subList(0, metaStart - 1)
        val title = content.firstOrNull().orEmpty()
        val body = content.drop(1).joinToString("\n").trim('\n')

        return JoplinItem(
            id = id,
            type = type,
            title = title,
            body = body,
            updatedMillis = parseTime(meta["updated_time"]),
            isConflict = meta["is_conflict"] == "1",
            deletedMillis = parseTime(meta["deleted_time"]),
        )
    }

    private fun parseTime(value: String?): Long {
        if (value.isNullOrEmpty()) return 0L
        return try {
            Instant.parse(value).toEpochMilli()
        } catch (e: java.time.format.DateTimeParseException) {
            value.toLongOrNull() ?: 0L
        }
    }

    private val image = Regex("""!\[[^\]]*]\([^)]*\)""")
    private val link = Regex("""\[([^\]]*)]\([^)]*\)""")
    private val heading = Regex("""(?m)^\s{0,3}#{1,6}\s*""")
    private val bullet = Regex("""(?m)^\s*(?:[-*+]|\d+\.)\s+(?:\[[ xX]]\s*)?""")
    private val emphasis = Regex("""[*_`~>]""")
    private val spaces = Regex("""\s+""")

    /** Extrait de texte brut (sans syntaxe Markdown) limité à [maxLength] caractères. */
    fun plainSnippet(markdown: String, maxLength: Int = 140): String {
        val plain = markdown
            .replace(image, "")
            .replace(link, "$1")
            .replace(heading, "")
            .replace(bullet, "")
            .replace(emphasis, "")
            .replace(spaces, " ")
            .trim()
        return if (plain.length <= maxLength) plain else plain.take(maxLength - 1).trimEnd() + "…"
    }
}
