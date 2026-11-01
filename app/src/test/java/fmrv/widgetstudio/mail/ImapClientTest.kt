package fmrv.widgetstudio.mail

import fmrv.widgetstudio.data.joplin.RemoteException
import fmrv.widgetstudio.data.mail.ImapClient
import fmrv.widgetstudio.domain.model.FailureReason
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** Faux serveur IMAP local : vérifie le dialogue exact tenu par le client, sans Gmail ni réseau. */
class ImapClientTest {

    private class FakeImapServer(private val script: (String) -> String?, private val greeting: String = "* OK IMAP4rev1 prêt\r\n") {
        private val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        val received = mutableListOf<String>()
        val port get() = server.localPort
        private val worker = thread(isDaemon = true) {
            try {
                server.accept().use { client ->
                    val out = client.getOutputStream()
                    val input = client.getInputStream().bufferedReader(Charsets.UTF_8)
                    out.write(greeting.toByteArray(Charsets.UTF_8))
                    out.flush()
                    while (true) {
                        val line = input.readLine() ?: break
                        synchronized(received) { received += line }
                        val reply = script(line) ?: break
                        out.write(reply.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                }
            } catch (e: IOException) {
                // le client a fermé : fin normale
            }
        }

        fun close() = server.close()
    }

    private var server: FakeImapServer? = null

    @AfterTest
    fun tearDown() {
        server?.close()
    }

    private fun header(from: String, subject: String): String =
        "From: $from\r\nSubject: $subject\r\nDate: Sun, 1 Nov 2026 10:00:00 +0000\r\n\r\n"

    private fun fetchResponse(seq: Int, uid: Int, date: String, headers: String): String {
        val size = headers.toByteArray(Charsets.UTF_8).size
        return "* $seq FETCH (UID $uid INTERNALDATE \"$date\" BODY[HEADER.FIELDS (FROM SUBJECT DATE)] {$size}\r\n$headers)\r\n"
    }

    private fun script(line: String): String? {
        val tag = line.substringBefore(' ')
        val command = line.substringAfter(' ')
        return when {
            command.startsWith("LOGIN") -> "$tag OK LOGIN terminé\r\n"
            command.startsWith("EXAMINE") -> "* 3 EXISTS\r\n$tag OK [READ-ONLY] EXAMINE terminé\r\n"
            command.startsWith("UID SEARCH") -> "* SEARCH 205 101 150\r\n$tag OK SEARCH terminé\r\n"
            command.startsWith("UID FETCH") ->
                fetchResponse(1, 150, "01-Nov-2026 09:00:00 +0000", header("=?UTF-8?Q?Zo=C3=A9?= <z@x.fr>", "=?UTF-8?Q?Caf=C3=A9_ce_soir?=")) +
                    fetchResponse(2, 205, "01-Nov-2026 11:30:00 +0100", header("\"Banque\" <b@x.fr>", "Relevé disponible")) +
                    "$tag OK FETCH terminé\r\n"
            command.startsWith("LOGOUT") -> "* BYE à bientôt\r\n$tag OK LOGOUT terminé\r\n"
            else -> "$tag BAD commande inconnue\r\n"
        }
    }

    private fun client(srv: FakeImapServer) = ImapClient { Socket(InetAddress.getLoopbackAddress(), srv.port) }

    @Test
    fun `les e-mails non lus sont lus, decodes et tries du plus recent au plus ancien`() = runTest {
        val srv = FakeImapServer(::script).also { server = it }

        val messages = client(srv).fetchUnread("moi@gmail.com", "abcdabcdabcdabcd", limit = 10)

        assertEquals(listOf("205", "150"), messages.map { it.uid })
        assertEquals("Relevé disponible", messages[0].subject)
        assertEquals("Banque", messages[0].from)
        assertEquals("Café ce soir", messages[1].subject)
        assertEquals("Zoé", messages[1].from)
        assertEquals(1793529000000L, messages[0].receivedMillis) // 11:30 +01:00 = 10:30 UTC
    }

    @Test
    fun `le dialogue est en lecture seule et ne demande que les en-tetes`() = runTest {
        val srv = FakeImapServer(::script).also { server = it }

        client(srv).fetchUnread("moi@gmail.com", "mdp", limit = 10)

        val commands = synchronized(srv.received) { srv.received.map { it.substringAfter(' ') } }
        assertEquals("LOGIN \"moi@gmail.com\" \"mdp\"", commands[0])
        assertEquals("EXAMINE INBOX", commands[1])
        assertEquals("UID SEARCH UNSEEN", commands[2])
        assertTrue(commands[3].startsWith("UID FETCH 101,150,205 (INTERNALDATE BODY.PEEK[HEADER.FIELDS"))
        assertEquals("LOGOUT", commands.last())
        assertFalse(commands.any { it.contains("STORE") || it.contains("SELECT") || it.contains("EXPUNGE") || it.contains("DELETE") })
    }

    @Test
    fun `la limite ne garde que les messages les plus recents`() = runTest {
        val srv = FakeImapServer(::script).also { server = it }

        client(srv).fetchUnread("moi@gmail.com", "mdp", limit = 2)

        val fetch = synchronized(srv.received) { srv.received.first { it.contains("UID FETCH") } }
        assertTrue(fetch.contains("UID FETCH 150,205 "), fetch)
    }

    @Test
    fun `aucun message non lu ne declenche pas de FETCH`() = runTest {
        val srv = FakeImapServer(script = { line ->
            val tag = line.substringBefore(' ')
            when {
                line.contains("UID SEARCH") -> "* SEARCH\r\n$tag OK\r\n"
                line.contains("LOGOUT") -> "* BYE\r\n$tag OK\r\n"
                else -> "$tag OK\r\n"
            }
        }).also { server = it }

        assertTrue(client(srv).fetchUnread("a@b.fr", "x", limit = 5).isEmpty())
        assertFalse(synchronized(srv.received) { srv.received.any { it.contains("FETCH") } })
    }

    @Test
    fun `des identifiants refuses donnent une erreur d'authentification`() = runTest {
        val srv = FakeImapServer(script = { line ->
            val tag = line.substringBefore(' ')
            if (line.contains("LOGIN")) "$tag NO [AUTHENTICATIONFAILED] Invalid credentials (Failure)\r\n" else "$tag OK\r\n"
        }).also { server = it }

        val error = assertFailsWith<RemoteException> { client(srv).fetchUnread("a@b.fr", "mauvais", limit = 5) }

        assertEquals(FailureReason.AUTH, error.reason)
    }

    @Test
    fun `les guillemets et antislashs des identifiants sont echappes`() = runTest {
        val srv = FakeImapServer(::script).also { server = it }

        client(srv).fetchUnread("a@b.fr", "pa\"ss\\w", limit = 5)

        assertEquals("LOGIN \"a@b.fr\" \"pa\\\"ss\\\\w\"", synchronized(srv.received) { srv.received[0].substringAfter(' ') })
    }

    @Test
    fun `des caracteres de controle dans le mot de passe sont refuses sans rien envoyer`() = runTest {
        val srv = FakeImapServer(::script).also { server = it }

        val error = assertFailsWith<RemoteException> { client(srv).fetchUnread("a@b.fr", "x\r\nA2 DELETE INBOX", limit = 5) }

        assertEquals(FailureReason.AUTH, error.reason)
        assertTrue(synchronized(srv.received) { srv.received.none { it.contains("DELETE") } })
    }

    @Test
    fun `un accueil inattendu ou une connexion coupee sont des erreurs`() = runTest {
        val bad = FakeImapServer(::script, greeting = "* BYE pas pour vous\r\n").also { server = it }
        assertEquals(FailureReason.UNKNOWN, assertFailsWith<RemoteException> { client(bad).fetchUnread("a@b.fr", "x", 5) }.reason)

        val cut = FakeImapServer({ null }).also { server = it }
        assertEquals(FailureReason.NETWORK, assertFailsWith<RemoteException> { client(cut).fetchUnread("a@b.fr", "x", 5) }.reason)
    }

    @Test
    fun `une machine injoignable est une erreur reseau`() = runTest {
        val unreachable = ImapClient { throw IOException("hors ligne") }
        assertEquals(FailureReason.NETWORK, assertFailsWith<RemoteException> { unreachable.fetchUnread("a@b.fr", "x", 5) }.reason)
    }
}
