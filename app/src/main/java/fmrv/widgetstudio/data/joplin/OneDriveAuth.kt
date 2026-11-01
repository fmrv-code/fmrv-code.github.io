package fmrv.widgetstudio.data.joplin

import android.net.Uri
import fmrv.widgetstudio.domain.model.FailureReason
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface AuthResult {
    data object Success : AuthResult

    data class Failure(val reason: FailureReason) : AuthResult
}

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Long = 3600L,
)

/**
 * Connexion à OneDrive (Microsoft identity platform) par OAuth 2.0 « code d'autorisation + PKCE » :
 * application publique, aucun secret embarqué, aucun mot de passe vu par l'application.
 */
@Singleton
class OneDriveAuth @Inject constructor(
    private val store: JoplinSettingsStore,
    private val http: HttpClient,
    private val json: Json,
) : Connection {

    internal var now: () -> Long = System::currentTimeMillis

    private val tokenLock = Mutex()

    override suspend fun isConnected(): Boolean = store.refreshToken() != null

    /** Prépare l'autorisation et retourne l'URL à ouvrir dans le navigateur, ou `null` sans identifiant d'application. */
    suspend fun beginAuthorization(): String? {
        val clientId = store.current().clientId
        if (clientId.isBlank()) return null
        val verifier = Pkce.newVerifier()
        val state = UUID.randomUUID().toString()
        store.savePendingAuthorization(verifier, state)
        return OneDriveEndpoints.authorizeUrl(clientId, Pkce.challengeOf(verifier), state)
    }

    /** Termine l'autorisation à partir de l'URI de redirection reçue par `OAuthRedirectActivity`. */
    suspend fun completeAuthorization(redirect: Uri): AuthResult {
        val pending = store.takePendingAuthorization() ?: return AuthResult.Failure(FailureReason.AUTH)
        if (redirect.getQueryParameter("state") != pending.second) return AuthResult.Failure(FailureReason.AUTH)
        val code = redirect.getQueryParameter("code") ?: return AuthResult.Failure(FailureReason.AUTH)
        val clientId = store.current().clientId

        return try {
            val response = http.postForm(
                OneDriveEndpoints.TOKEN_URL,
                mapOf(
                    "client_id" to clientId,
                    "grant_type" to "authorization_code",
                    "code" to code,
                    "redirect_uri" to OneDriveEndpoints.REDIRECT_URI,
                    "code_verifier" to pending.first,
                    "scope" to OneDriveEndpoints.SCOPE,
                ),
            )
            if (!response.isSuccess) return AuthResult.Failure(FailureReason.AUTH)
            val token = json.decodeFromString<TokenResponse>(response.body)
            if (token.refreshToken == null) return AuthResult.Failure(FailureReason.AUTH)
            store.saveTokens(token.accessToken, token.refreshToken, now() + token.expiresInSeconds * 1000)
            AuthResult.Success
        } catch (e: RemoteException) {
            AuthResult.Failure(e.reason)
        } catch (e: kotlinx.serialization.SerializationException) {
            AuthResult.Failure(FailureReason.AUTH)
        }
    }

    /** Jeton d'accès valide, renouvelé automatiquement avec le jeton de rafraîchissement. */
    suspend fun accessToken(): String = tokenLock.withLock {
        val cached = store.accessToken()
        if (cached != null && cached.expiresAtMillis - EXPIRY_MARGIN_MS > now()) return cached.value

        val refresh = store.refreshToken() ?: throw RemoteException(FailureReason.AUTH, "Non connecté")
        val clientId = store.current().clientId
        if (clientId.isBlank()) throw RemoteException(FailureReason.AUTH, "Identifiant d'application manquant")

        val response = http.postForm(
            OneDriveEndpoints.TOKEN_URL,
            mapOf(
                "client_id" to clientId,
                "grant_type" to "refresh_token",
                "refresh_token" to refresh,
                "scope" to OneDriveEndpoints.SCOPE,
            ),
        )
        if (response.code == 400 || response.code == 401) {
            // Jeton révoqué ou expiré : il faut se reconnecter.
            store.clearTokens()
            throw RemoteException(FailureReason.AUTH, "Session expirée")
        }
        if (!response.isSuccess) throw RemoteException(FailureReason.NETWORK, "HTTP ${response.code}")
        val token = try {
            json.decodeFromString<TokenResponse>(response.body)
        } catch (e: kotlinx.serialization.SerializationException) {
            throw RemoteException(FailureReason.UNKNOWN, cause = e)
        }
        store.saveTokens(token.accessToken, token.refreshToken, now() + token.expiresInSeconds * 1000)
        token.accessToken
    }

    suspend fun disconnect() {
        store.clearTokens()
    }

    private companion object {
        const val EXPIRY_MARGIN_MS = 60_000L
    }
}
