package fmrv.widgetstudio.data.joplin

import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** PKCE (RFC 7636) : prouve que celui qui échange le code est celui qui a démarré l'autorisation. */
object Pkce {
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun newVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    fun challengeOf(verifier: String): String =
        encoder.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
}

object OneDriveEndpoints {
    const val DEFAULT_FOLDER = "Apps/Joplin"

    /** Doit correspondre exactement à l'URI de redirection déclarée dans l'application Azure. */
    const val REDIRECT_URI = "fmrv.widgetstudio://oauth"

    /** Lecture seule des fichiers + jeton de rafraîchissement. */
    const val SCOPE = "Files.Read offline_access"

    const val TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
    private const val AUTHORIZE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize"
    private const val GRAPH = "https://graph.microsoft.com/v1.0"

    fun authorizeUrl(clientId: String, codeChallenge: String, state: String): String =
        AUTHORIZE_URL + "?" + listOf(
            "client_id" to clientId,
            "response_type" to "code",
            "redirect_uri" to REDIRECT_URI,
            "response_mode" to "query",
            "scope" to SCOPE,
            "state" to state,
            "code_challenge" to codeChallenge,
            "code_challenge_method" to "S256",
        ).joinToString("&") { (key, value) -> "$key=${encode(value)}" }

    fun childrenUrl(folderPath: String): String =
        "$GRAPH/me/drive/root:/${encodePath(folderPath)}:/children"

    fun contentUrl(itemId: String): String = "$GRAPH/me/drive/items/${encode(itemId)}/content"

    /** Encode chaque segment du chemin ; ignore les `/` en trop. */
    fun encodePath(path: String): String =
        path.split('/').filter { it.isNotBlank() }.joinToString("/") { encode(it.trim()) }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
