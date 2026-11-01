package fmrv.widgetstudio.data.mail

import java.nio.charset.Charset
import java.util.Base64

/** Décodage des en-têtes d'e-mails (RFC 2047 « mots encodés », dépliage, adresses). */
object MimeHeaders {

    private val encodedWord = Regex("""=\?([^?\s]+)\?([bBqQ])\?([^?\s]*)\?=""")
    private val whitespaceBetweenWords = Regex("""(=\?[^?\s]+\?[bBqQ]\?[^?\s]*\?=)\s+(?==\?[^?\s]+\?[bBqQ]\?)""")

    /** Décode `=?UTF-8?B?...?=` et `=?ISO-8859-1?Q?...?=`. Un texte brut est retourné tel quel. */
    fun decodeWords(raw: String): String {
        // Les espaces entre deux mots encodés adjacents ne font pas partie du texte.
        val joined = whitespaceBetweenWords.replace(raw, "$1")
        return encodedWord.replace(joined) { match ->
            val charset = charsetOf(match.groupValues[1])
            val payload = match.groupValues[3]
            try {
                val bytes = if (match.groupValues[2].equals("B", ignoreCase = true)) {
                    Base64.getDecoder().decode(payload) // strict : un contenu corrompu lève une exception et reste affiché tel quel
                } else {
                    decodeQuotedPrintable(payload)
                }
                String(bytes, charset)
            } catch (e: IllegalArgumentException) {
                match.value
            }
        }
    }

    /** En-têtes pliés sur plusieurs lignes → nom (en minuscules) → valeur sur une ligne. */
    fun parseHeaders(block: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        var currentName: String? = null
        val value = StringBuilder()
        fun flush() {
            currentName?.let { result.putIfAbsent(it, value.toString().trim()) }
            currentName = null
            value.setLength(0)
        }
        for (line in block.replace("\r\n", "\n").split('\n')) {
            if (line.isEmpty()) continue
            if (line[0] == ' ' || line[0] == '\t') {
                value.append(' ').append(line.trim())
            } else {
                flush()
                val colon = line.indexOf(':')
                if (colon > 0) {
                    currentName = line.substring(0, colon).trim().lowercase()
                    value.append(line.substring(colon + 1).trim())
                }
            }
        }
        flush()
        return result
    }

    /** Nom affiché de l'expéditeur : `"Alice" <a@x.fr>` → `Alice` ; `a@x.fr` → `a@x.fr`. */
    fun displayName(fromHeader: String): String {
        val decoded = decodeWords(fromHeader).trim()
        val angle = decoded.indexOf('<')
        val name = if (angle > 0) decoded.substring(0, angle).trim().trim('"').trim() else ""
        if (name.isNotEmpty()) return name
        return decoded.removePrefix("<").removeSuffix(">").trim()
    }

    private fun charsetOf(name: String): Charset = try {
        Charset.forName(name)
    } catch (e: IllegalArgumentException) {
        Charsets.UTF_8
    }

    private fun decodeQuotedPrintable(text: String): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '_' -> out.write(' '.code)
                c == '=' && i + 2 < text.length && isHex(text[i + 1]) && isHex(text[i + 2]) -> {
                    out.write(text.substring(i + 1, i + 3).toInt(16))
                    i += 2
                }
                else -> out.write(c.code)
            }
            i++
        }
        return out.toByteArray()
    }

    private fun isHex(c: Char) = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
}
