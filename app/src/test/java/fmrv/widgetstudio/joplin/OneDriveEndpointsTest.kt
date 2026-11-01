package fmrv.widgetstudio.joplin

import fmrv.widgetstudio.data.joplin.OneDriveEndpoints
import fmrv.widgetstudio.data.joplin.Pkce
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OneDriveEndpointsTest {

    @Test
    fun `le challenge PKCE correspond au vecteur de test de la RFC 7636`() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challengeOf("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test
    fun `un verifier PKCE est long, sans remplissage et different a chaque appel`() {
        val first = Pkce.newVerifier()
        assertTrue(first.length in 43..128)
        assertTrue(first.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertTrue(first != Pkce.newVerifier())
    }

    @Test
    fun `l'URL d'autorisation contient tous les parametres encodes`() {
        val url = OneDriveEndpoints.authorizeUrl("client-123", "challenge-abc", "state-xyz")

        assertTrue(url.startsWith("https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?"))
        assertTrue("client_id=client-123" in url)
        assertTrue("response_type=code" in url)
        assertTrue("redirect_uri=fmrv.widgetstudio%3A%2F%2Foauth" in url)
        assertTrue("scope=Files.Read%20offline_access" in url)
        assertTrue("state=state-xyz" in url)
        assertTrue("code_challenge=challenge-abc" in url)
        assertTrue("code_challenge_method=S256" in url)
    }

    @Test
    fun `le chemin du dossier est encode segment par segment`() {
        assertEquals("Apps/Joplin", OneDriveEndpoints.encodePath("Apps/Joplin"))
        assertEquals("Apps/Mes%20notes", OneDriveEndpoints.encodePath("/Apps/Mes notes/"))
        assertEquals("Apps/Joplin", OneDriveEndpoints.encodePath("Apps//Joplin"))
        assertEquals(
            "https://graph.microsoft.com/v1.0/me/drive/root:/Apps/Joplin:/children",
            OneDriveEndpoints.childrenUrl("Apps/Joplin"),
        )
    }
}
