package fmrv.widgetstudio.data.mail

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import fmrv.widgetstudio.data.joplin.RemoteException
import fmrv.widgetstudio.data.joplin.SecretBox
import fmrv.widgetstudio.domain.model.FailureReason
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.mailDataStore by preferencesDataStore(name = "mail")

/** Identifiants Gmail. Le mot de passe d'application est chiffré avec [SecretBox] avant d'être écrit. */
@Singleton
class MailSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val settings: Flow<MailSettings> = context.mailDataStore.data.map { prefs ->
        MailSettings(email = prefs[EMAIL].orEmpty(), connected = prefs[PASSWORD] != null && !prefs[EMAIL].isNullOrBlank())
    }

    suspend fun current(): MailSettings = settings.first()

    suspend fun credentials(): MailCredentials? {
        val prefs = context.mailDataStore.data.first()
        val email = prefs[EMAIL]?.takeIf { it.isNotBlank() } ?: return null
        val password = prefs[PASSWORD]?.let(SecretBox::decrypt) ?: return null
        return MailCredentials(email, password)
    }

    suspend fun save(email: String, appPassword: String) {
        context.mailDataStore.edit {
            it[EMAIL] = email.trim()
            // Google affiche le mot de passe d'application par groupes de 4 séparés par des espaces.
            it[PASSWORD] = SecretBox.encrypt(appPassword.filterNot(Char::isWhitespace))
        }
    }

    suspend fun clear() {
        context.mailDataStore.edit { it.remove(PASSWORD) }
    }

    private companion object {
        val EMAIL = stringPreferencesKey("email")
        val PASSWORD = stringPreferencesKey("password")
    }
}

/** Lecture des e-mails non lus de Gmail via IMAP sur TLS (imap.gmail.com:993). */
@Singleton
class ImapMailSource @Inject constructor(
    private val store: MailSettingsStore,
) : MailSource {

    private val client = ImapClient { tlsSocket(HOST, PORT) }

    override suspend fun isConfigured(): Boolean = store.credentials() != null

    override suspend fun fetchUnread(limit: Int): List<MailMessage> {
        val credentials = store.credentials() ?: throw RemoteException(FailureReason.AUTH, "Non configuré")
        return client.fetchUnread(credentials.email, credentials.appPassword, limit)
    }

    private fun tlsSocket(host: String, port: Int): Socket {
        val plain = Socket()
        try {
            plain.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val socket = factory.createSocket(plain, host, port, true) as SSLSocket
            socket.sslParameters = socket.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
            socket.startHandshake()
            // Vérification explicite du nom d'hôte du certificat, en plus du réglage ci-dessus.
            if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, socket.session)) {
                socket.close()
                throw SSLPeerUnverifiedException("Certificat invalide pour $host")
            }
            return socket
        } catch (e: Exception) {
            plain.close()
            throw e
        }
    }

    private companion object {
        const val HOST = "imap.gmail.com"
        const val PORT = 993
        const val CONNECT_TIMEOUT_MS = 15_000
    }
}
