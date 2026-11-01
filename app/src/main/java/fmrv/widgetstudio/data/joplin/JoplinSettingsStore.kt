package fmrv.widgetstudio.data.joplin

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.joplinDataStore by preferencesDataStore(name = "joplin")

data class JoplinSettings(
    val clientId: String = "",
    val folderPath: String = OneDriveEndpoints.DEFAULT_FOLDER,
    val connected: Boolean = false,
)

data class AccessToken(val value: String, val expiresAtMillis: Long)

/** Réglages de la connexion OneDrive. Les jetons sont chiffrés avec [SecretBox] avant d'être écrits. */
@Singleton
class JoplinSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val settings: Flow<JoplinSettings> = context.joplinDataStore.data.map { prefs ->
        JoplinSettings(
            clientId = prefs[CLIENT_ID].orEmpty(),
            folderPath = prefs[FOLDER] ?: OneDriveEndpoints.DEFAULT_FOLDER,
            connected = prefs[REFRESH_TOKEN] != null,
        )
    }

    suspend fun current(): JoplinSettings = settings.first()

    suspend fun setClientId(value: String) {
        context.joplinDataStore.edit { it[CLIENT_ID] = value.trim() }
    }

    suspend fun setFolderPath(value: String) {
        context.joplinDataStore.edit { it[FOLDER] = value.trim() }
    }

    suspend fun saveTokens(access: String, refresh: String?, expiresAtMillis: Long) {
        context.joplinDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = SecretBox.encrypt(access)
            prefs[ACCESS_EXPIRES] = expiresAtMillis
            if (refresh != null) prefs[REFRESH_TOKEN] = SecretBox.encrypt(refresh)
        }
    }

    suspend fun refreshToken(): String? = context.joplinDataStore.data.first()[REFRESH_TOKEN]?.let(SecretBox::decrypt)

    suspend fun accessToken(): AccessToken? {
        val prefs = context.joplinDataStore.data.first()
        val value = prefs[ACCESS_TOKEN]?.let(SecretBox::decrypt) ?: return null
        return AccessToken(value, prefs[ACCESS_EXPIRES] ?: 0L)
    }

    suspend fun savePendingAuthorization(verifier: String, state: String) {
        context.joplinDataStore.edit {
            it[PENDING_VERIFIER] = SecretBox.encrypt(verifier)
            it[PENDING_STATE] = state
        }
    }

    /** Retourne puis efface (verifier, state) de l'autorisation en cours : usage unique. */
    suspend fun takePendingAuthorization(): Pair<String, String>? {
        val prefs = context.joplinDataStore.data.first()
        val verifier = prefs[PENDING_VERIFIER]?.let(SecretBox::decrypt)
        val state = prefs[PENDING_STATE]
        context.joplinDataStore.edit {
            it.remove(PENDING_VERIFIER)
            it.remove(PENDING_STATE)
        }
        return if (verifier != null && state != null) verifier to state else null
    }

    suspend fun clearTokens() {
        context.joplinDataStore.edit { prefs: androidx.datastore.preferences.core.MutablePreferences ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(ACCESS_EXPIRES)
            prefs.remove(REFRESH_TOKEN)
        }
    }

    private companion object {
        val CLIENT_ID = stringPreferencesKey("client_id")
        val FOLDER = stringPreferencesKey("folder")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val ACCESS_EXPIRES = longPreferencesKey("access_expires")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val PENDING_VERIFIER = stringPreferencesKey("pending_verifier")
        val PENDING_STATE = stringPreferencesKey("pending_state")
    }
}
