package fmrv.widgetstudio.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fmrv.widgetstudio.R
import fmrv.widgetstudio.data.joplin.JoplinSettingsStore
import fmrv.widgetstudio.data.joplin.OneDriveAuth
import fmrv.widgetstudio.data.joplin.OneDriveEndpoints
import fmrv.widgetstudio.domain.model.FailureReason
import fmrv.widgetstudio.domain.model.RefreshResult
import fmrv.widgetstudio.domain.repository.NotesRepository
import fmrv.widgetstudio.domain.repository.WidgetUpdater
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiMessage(@StringRes val res: Int, val arg: Int? = null)

data class JoplinUiState(
    val clientId: String = "",
    val folder: String = OneDriveEndpoints.DEFAULT_FOLDER,
    val connected: Boolean = false,
    val busy: Boolean = false,
    val noteCount: Int? = null,
    val lastRefreshMillis: Long = 0L,
    val message: UiMessage? = null,
    val redirectUri: String = OneDriveEndpoints.REDIRECT_URI,
)

sealed interface JoplinEvent {
    /** URL de connexion Microsoft à ouvrir dans le navigateur. */
    data class OpenUrl(val url: String) : JoplinEvent
}

@HiltViewModel
class JoplinSettingsViewModel @Inject constructor(
    private val store: JoplinSettingsStore,
    private val auth: OneDriveAuth,
    private val notes: NotesRepository,
    private val updater: WidgetUpdater,
) : ViewModel() {

    private val _state = MutableStateFlow(JoplinUiState())
    val state: StateFlow<JoplinUiState> = _state.asStateFlow()

    private val _events = Channel<JoplinEvent>(Channel.BUFFERED)
    val events: Flow<JoplinEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            // Les champs ne sont lus qu'une fois : on ne réécrit pas ce que l'utilisateur est en train de saisir.
            val initial = store.current()
            _state.update { it.copy(clientId = initial.clientId, folder = initial.folderPath) }
            refreshSnapshot()
        }
        viewModelScope.launch {
            // La connexion peut aboutir pendant que cet écran est ouvert (retour du navigateur).
            store.settings.collect { settings ->
                _state.update { it.copy(connected = settings.connected) }
                if (settings.connected) refreshSnapshot()
            }
        }
    }

    fun onClientIdChange(value: String) {
        _state.update { it.copy(clientId = value, message = null) }
        viewModelScope.launch { store.setClientId(value) }
    }

    fun onFolderChange(value: String) {
        _state.update { it.copy(folder = value, message = null) }
        viewModelScope.launch { store.setFolderPath(value.ifBlank { OneDriveEndpoints.DEFAULT_FOLDER }) }
    }

    fun connect() {
        viewModelScope.launch {
            // On s'assure que les derniers champs saisis sont bien enregistrés avant de démarrer.
            store.setClientId(_state.value.clientId)
            val url = auth.beginAuthorization()
            if (url == null) {
                _state.update { it.copy(message = UiMessage(R.string.joplin_need_client_id)) }
            } else {
                _events.send(JoplinEvent.OpenUrl(url))
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            auth.disconnect()
            notes.clear()
            updater.refreshAll()
            _state.update { it.copy(noteCount = null, lastRefreshMillis = 0L, message = UiMessage(R.string.joplin_disconnected)) }
        }
    }

    fun refreshNow() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val message = when (val result = notes.refresh()) {
                is RefreshResult.Success -> UiMessage(R.string.joplin_refresh_ok, result.noteCount)
                RefreshResult.NotConnected -> UiMessage(R.string.joplin_error_not_connected)
                is RefreshResult.Failure -> UiMessage(errorMessage(result.reason))
            }
            updater.refreshAll()
            _state.update { it.copy(busy = false, message = message) }
            refreshSnapshot()
        }
    }

    private suspend fun refreshSnapshot() {
        val snapshot = notes.snapshot()
        _state.update {
            it.copy(
                noteCount = if (snapshot.refreshedAtMillis > 0) snapshot.notes.size else null,
                lastRefreshMillis = snapshot.refreshedAtMillis,
            )
        }
    }

    @StringRes
    private fun errorMessage(reason: FailureReason): Int = when (reason) {
        FailureReason.NETWORK -> R.string.joplin_error_network
        FailureReason.AUTH -> R.string.joplin_error_auth
        FailureReason.FOLDER_NOT_FOUND -> R.string.joplin_error_folder
        FailureReason.UNKNOWN -> R.string.joplin_error_unknown
    }
}
