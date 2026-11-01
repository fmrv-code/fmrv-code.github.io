package fmrv.widgetstudio.mail

import fmrv.widgetstudio.data.mail.MimeHeaders
import kotlin.test.Test
import kotlin.test.assertEquals

class MimeHeadersTest {

    @Test
    fun `un texte brut est retourne tel quel`() {
        assertEquals("Réunion demain", MimeHeaders.decodeWords("Réunion demain"))
    }

    @Test
    fun `les mots encodes en base64 et en quoted-printable sont decodes`() {
        assertEquals("Café", MimeHeaders.decodeWords("=?UTF-8?B?Q2Fmw6k=?="))
        assertEquals("Café du matin", MimeHeaders.decodeWords("=?UTF-8?Q?Caf=C3=A9_du_matin?="))
        assertEquals("Été", MimeHeaders.decodeWords("=?ISO-8859-1?Q?=C9t=E9?="))
    }

    @Test
    fun `des mots encodes adjacents sont recolles sans espace`() {
        assertEquals("Bonjour tout le monde", MimeHeaders.decodeWords("=?UTF-8?Q?Bonjour_tout?= =?UTF-8?Q?_le_monde?="))
    }

    @Test
    fun `un melange de texte et de mots encodes est conserve`() {
        assertEquals("Re: Café noir", MimeHeaders.decodeWords("Re: =?UTF-8?Q?Caf=C3=A9?= noir"))
    }

    @Test
    fun `un jeu de caracteres inconnu ou un base64 invalide ne plante pas`() {
        assertEquals("abc", MimeHeaders.decodeWords("=?X-INCONNU?Q?abc?="))
        assertEquals("=?UTF-8?B?***?=", MimeHeaders.decodeWords("=?UTF-8?B?***?="))
    }

    @Test
    fun `les en-tetes plies sur plusieurs lignes sont reunis`() {
        val headers = MimeHeaders.parseHeaders(
            "From: Alice <a@x.fr>\r\nSubject: Un sujet\r\n tres long\r\n\tsur trois lignes\r\nDate: Sun, 1 Nov 2026 10:00:00 +0000\r\n\r\n",
        )

        assertEquals("Un sujet tres long sur trois lignes", headers["subject"])
        assertEquals("Alice <a@x.fr>", headers["from"])
    }

    @Test
    fun `le nom affiche de l'expediteur est extrait`() {
        assertEquals("Alice Martin", MimeHeaders.displayName("\"Alice Martin\" <alice@x.fr>"))
        assertEquals("Alice", MimeHeaders.displayName("Alice <alice@x.fr>"))
        assertEquals("alice@x.fr", MimeHeaders.displayName("alice@x.fr"))
        assertEquals("alice@x.fr", MimeHeaders.displayName("<alice@x.fr>"))
        assertEquals("Zoé", MimeHeaders.displayName("=?UTF-8?Q?Zo=C3=A9?= <z@x.fr>"))
        assertEquals("", MimeHeaders.displayName(""))
    }
}
