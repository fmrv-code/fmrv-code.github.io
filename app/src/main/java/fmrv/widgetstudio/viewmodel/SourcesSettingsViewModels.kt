package fmrv.widgetstudio.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fmrv.widgetstudio.R
import fmrv.widgetstudio.data.feed.NewsSettingsStore
import fmrv.widgetstudio.data.mail.MailSettingsStore
import fmrv.widgetstudio.domain.model.FailureReason
import fmrv.widgetstudio.domain.model.RefreshResult
import fmrv.widgetstudio.domain.repository.MailRepository
import fmrv.widgetstudio.domain.repository.NewsRepository
import fmrv.widgetstudio.domain.repository.WidgetUpdater
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
    val customUrl: String = "",
    val defaultUrl: String = NewsSettingsStore.DEFAULT_URLS.first(),
    val busy: Boolean = false,
    val message: UiMessage? = null,
)

@HiltViewModel
class NewsSettingsViewModel @Inject constructor(
    private val store: NewsSettingsStore,
    private val news: NewsRepository,
    private val updater: WidgetUpdater,
) : ViewModel() {

    private val _state = MutableStateFlow(NewsUiState())
    val state: StateFlow<NewsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Lu une seule fois pour ne pas écraser ce que l'utilisateur saisit.
            val saved = store.customUrl.first()
            _state.update { it.copy(customUrl = saved) }
        }
    }

    fun onUrlChange(value: String) {
        _state.update { it.copy(customUrl = value, message = null) }
        viewModelScope.launch { store.setCustomUrl(value) }
    }

    fun refreshNow() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val message = when (val result = news.refresh()) {
                is RefreshResult.Success -> UiMessage(R.string.news_refresh_ok, result.noteCount)
                RefreshResult.NotConnected -> UiMessage(R.string.joplin_error_unknown)
                is RefreshResult.Failure -> UiMessage(
                    if (result.reason == FailureReason.NETWORK) R.string.joplin_error_network else R.string.news_error_feed,
                )
            }
            updater.refreshAll()
            _state.update { it.copy(busy = false, message = message) }
        }
    }
}

data class MailUiState(
    val email: String = "",
    val appPassword: String = "",
    val connected: Boolean = false,
    val busy: Boolean = false,
    val unreadCount: Int? = null,
    val message: UiMessage? = null,
)

@HiltViewModel
class MailSettingsViewModel @Inject constructor(
    private val store: MailSettingsStore,
    private val mail: MailRepository,
    private val updater: WidgetUpdater,
) : ViewModel() {

    private val _state = MutableStateFlow(MailUiState())
    val state: StateFlow<MailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = store.current()
            _state.update { it.copy(email = saved.email, connected = saved.connected) }
            if (saved.connected) refreshCount()
        }
    }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, message = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(appPassword = value, message = null) }

    /** Enregistre les identifiants puis teste la connexion : en cas de refus ils sont aussitôt effacés. */
    fun connect() {
        val current = _state.value
        if (current.busy) return
        if (current.email.isBlank() || current.appPassword.isBlank()) {
            _state.update { it.copy(message = UiMessage(R.string.mail_need_credentials)) }
            return
        }
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            store.save(current.email, current.appPassword)
            when (val result = mail.refresh()) {
                is RefreshResult.Success -> {
                    updater.refreshAll()
                    _state.update {
                        it.copy(
                            busy = false,
                            connected = true,
                            appPassword = "", // le mot de passe n'est plus gardé en mémoire d'interface
                            unreadCount = result.noteCount,
                            message = UiMessage(R.string.mail_connected),
                        )
                    }
                }
                else -> {
                    store.clear()
                    mail.clear()
                    _state.update { it.copy(busy = false, connected = false, message = UiMessage(failureMessage(result))) }
                }
            }
        }
    }

    fun refreshNow() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val result = mail.refresh()
            updater.refreshAll()
            val message = when (result) {
                is RefreshResult.Success -> UiMessage(R.string.mail_refresh_ok, result.noteCount)
                else -> UiMessage(failureMessage(result))
            }
            _state.update { it.copy(busy = false, message = message) }
            refreshCount()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            store.clear()
            mail.clear()
            updater.refreshAll()
            _state.update { it.copy(connected = false, unreadCount = null, appPassword = "", message = UiMessage(R.string.mail_disconnected)) }
        }
    }

    private suspend fun refreshCount() {
        val snapshot = mail.snapshot()
        _state.update { it.copy(unreadCount = if (snapshot.refreshedAtMillis > 0) snapshot.notes.size else null) }
    }

    @StringRes
    private fun failureMessage(result: RefreshResult): Int = when (result) {
        is RefreshResult.Failure -> when (result.reason) {
            FailureReason.AUTH -> R.string.mail_error_auth
            FailureReason.NETWORK -> R.string.joplin_error_network
            else -> R.string.mail_error_unknown
        }
        RefreshResult.NotConnected -> R.string.mail_need_credentials
        is RefreshResult.Success -> R.string.mail_connected
    }
}
