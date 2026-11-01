package fmrv.widgetstudio.data.mail

import fmrv.widgetstudio.data.joplin.RemoteException
import fmrv.widgetstudio.domain.model.FailureReason
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MailMessage(
    val uid: String,
    val subject: String,
    val from: String,
    val receivedMillis: Long,
)

/** Une réponse « non étiquetée » du serveur : texte avec les littéraux IMAP `{n}` extraits à part. */
internal class ImapResponse(val text: String, val literals: List<ByteArray>)

/**
 * Client IMAP minimal et **en lecture seule** : LOGIN, EXAMINE INBOX, UID SEARCH UNSEEN,
 * UID FETCH (en-têtes uniquement, sans marquer les messages comme lus), LOGOUT.
 * La connexion est fournie par [socketFactory] (TLS en production, socket simple dans les tests).
 */
class ImapClient(private val socketFactory: () -> Socket) {

    suspend fun fetchUnread(user: String, password: String, limit: Int): List<MailMessage> =
        withContext(Dispatchers.IO) {
            try {
                socketFactory().use { socket ->
                    socket.soTimeout = READ_TIMEOUT_MS
                    Session(socket.getInputStream(), socket.getOutputStream()).run(user, password, limit)
                }
            } catch (e: IOException) {
                throw RemoteException(FailureReason.NETWORK, cause = e)
            }
        }

    private class Session(input: InputStream, private val output: java.io.OutputStream) {
        private val reader = ImapReader(input)
        private var counter = 0

        fun run(user: String, password: String, limit: Int): List<MailMessage> {
            val greeting = reader.readResponse()
            if (!greeting.text.startsWith("* OK") && !greeting.text.startsWith("* PREAUTH")) {
                throw RemoteException(FailureReason.UNKNOWN, "Accueil IMAP inattendu")
            }

            command("LOGIN ${quote(user)} ${quote(password)}", authFailureIsAuth = true)
            try {
                command("EXAMINE INBOX")
                val search = command("UID SEARCH UNSEEN")
                val uids = search.firstOrNull { it.text.startsWith("* SEARCH") }
                    ?.text?.removePrefix("* SEARCH")?.trim()
                    ?.split(' ')?.mapNotNull { it.toLongOrNull() }
                    .orEmpty()
                    .sorted()
                if (uids.isEmpty()) return emptyList()

                val wanted = uids.takeLast(limit).joinToString(",")
                val fetch = command("UID FETCH $wanted (INTERNALDATE BODY.PEEK[HEADER.FIELDS (FROM SUBJECT DATE)])")
                return fetch.mapNotNull(::toMessage).sortedByDescending { it.receivedMillis }
            } finally {
                runCatching { command("LOGOUT") }
            }
        }

        private fun command(text: String, authFailureIsAuth: Boolean = false): List<ImapResponse> {
            val tag = "a${++counter}"
            output.write("$tag $text\r\n".toByteArray(Charsets.UTF_8))
            output.flush()
            val untagged = ArrayList<ImapResponse>()
            while (true) {
                val response = reader.readResponse()
                if (response.text.startsWith("$tag ")) {
                    val status = response.text.removePrefix("$tag ").substringBefore(' ').uppercase()
                    if (status == "OK") return untagged
                    if (status == "NO" && authFailureIsAuth) throw RemoteException(FailureReason.AUTH, "Identifiants refusés")
                    throw RemoteException(FailureReason.UNKNOWN, "IMAP $status")
                }
                untagged += response
            }
        }

        private fun toMessage(response: ImapResponse): MailMessage? {
            if (!response.text.contains(" FETCH ")) return null
            val uid = Regex("""\bUID (\d+)""").find(response.text)?.groupValues?.get(1) ?: return null
            val internalDate = Regex("""INTERNALDATE "([^"]+)"""").find(response.text)?.groupValues?.get(1)
            val headers = response.literals.firstOrNull()?.toString(Charsets.UTF_8)
                ?.let(MimeHeaders::parseHeaders).orEmpty()
            val subject = MimeHeaders.decodeWords(headers["subject"].orEmpty()).trim()
            val from = MimeHeaders.displayName(headers["from"].orEmpty())
            return MailMessage(uid, subject, from, parseInternalDate(internalDate))
        }

        /** Les arguments IMAP entre guillemets : `"` et `\` échappés ; les caractères de contrôle sont refusés. */
        private fun quote(value: String): String {
            if (value.any { it.code < 0x20 || it.code > 0x7E }) {
                throw RemoteException(FailureReason.AUTH, "Caractères non pris en charge")
            }
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }
    }

    internal companion object {
        const val READ_TIMEOUT_MS = 30_000
        private val internalDateFormat = DateTimeFormatter.ofPattern("d-MMM-yyyy HH:mm:ss Z", Locale.ENGLISH)

        fun parseInternalDate(value: String?): Long {
            if (value == null) return 0L
            return try {
                OffsetDateTime.parse(value.trim(), internalDateFormat).toInstant().toEpochMilli()
            } catch (e: DateTimeParseException) {
                0L
            }
        }
    }
}

/** Lecteur de réponses IMAP : lignes terminées par CRLF, avec gestion des littéraux `{n}`. */
internal class ImapReader(private val input: InputStream) {

    fun readResponse(): ImapResponse {
        val text = StringBuilder()
        val literals = ArrayList<ByteArray>()
        while (true) {
            val line = readLine()
            val literalSize = LITERAL.find(line)?.groupValues?.get(1)?.toIntOrNull()
            if (literalSize == null) {
                text.append(line)
                return ImapResponse(text.toString(), literals)
            }
            if (literalSize > MAX_LITERAL) throw IOException("Littéral IMAP trop grand")
            text.append(line.substring(0, line.lastIndexOf('{'))).append("{literal}")
            literals += readBytes(literalSize)
        }
    }

    private fun readLine(): String {
        val buffer = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b < 0) throw IOException("Connexion fermée par le serveur")
            if (b == '\n'.code) break
            if (b != '\r'.code) buffer.write(b)
            if (buffer.size() > MAX_LINE) throw IOException("Ligne IMAP trop longue")
        }
        return buffer.toString(Charsets.UTF_8.name())
    }

    private fun readBytes(count: Int): ByteArray {
        val bytes = ByteArray(count)
        var read = 0
        while (read < count) {
            val n = input.read(bytes, read, count - read)
            if (n < 0) throw IOException("Connexion fermée par le serveur")
            read += n
        }
        return bytes
    }

    private companion object {
        val LITERAL = Regex("""\{(\d+)}$""")
        const val MAX_LITERAL = 1_000_000
        const val MAX_LINE = 65_536
    }
}
