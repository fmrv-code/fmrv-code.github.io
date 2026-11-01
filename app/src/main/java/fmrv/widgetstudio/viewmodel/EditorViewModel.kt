package fmrv.widgetstudio.viewmodel

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.AppInfo
import fmrv.widgetstudio.domain.model.NoteSummary
import fmrv.widgetstudio.domain.model.PinResult
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.domain.repository.AppIconProvider
import fmrv.widgetstudio.domain.repository.AppsSource
import fmrv.widgetstudio.domain.repository.ListSources
import fmrv.widgetstudio.domain.repository.WidgetPinner
import fmrv.widgetstudio.domain.repository.WidgetRepository
import fmrv.widgetstudio.domain.repository.WidgetUpdater
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val config: WidgetConfig = WidgetConfig(appWidgetId = WidgetConfig.NO_ID),
    val apps: List<AppInfo> = emptyList(),
    val previewIcon: ImageBitmap? = null,
    val previewNotes: List<NoteSummary> = emptyList(),
    val pinSize: WidgetSizeOption = WidgetSizeOption.MEDIUM,
    val loading: Boolean = true,
) {
    /** Un raccourci exige une application cible ; l'horloge et la liste de notes peuvent s'en passer. */
    val canSave: Boolean
        get() = config.contentType != WidgetContentType.APP_SHORTCUT || config.targetPackage != null

    /** Vrai quand le widget existe déjà sur l'écran d'accueil (édition) ; faux pour une création. */
    val isPlaced: Boolean
        get() = config.appWidgetId > 0
}

sealed interface EditorEvent {
    data class Saved(val appWidgetId: Int) : EditorEvent

    data object PinRequested : EditorEvent

    data class Message(@StringRes val messageRes: Int) : EditorEvent
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: WidgetRepository,
    private val appsSource: AppsSource,
    private val icons: AppIconProvider,
    private val updater: WidgetUpdater,
    private val pinner: WidgetPinner,
    private val sources: ListSources,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    private var started = false

    /**
     * Initialise l'éditeur. Idempotent : peut être appelé à chaque recomposition d'effet.
     * - [widgetId] > 0 : édition d'un widget posé (ou configuration initiale à la pose).
     * - [widgetId] == 0 : création depuis l'application, éventuellement pour [presetPackage].
     */
    fun start(widgetId: Int, presetPackage: String?) {
        if (started) return
        started = true

        viewModelScope.launch {
            val apps = runCatching { appsSource.loadApps() }.getOrDefault(emptyList())
            _state.update { it.copy(apps = apps) }
        }
        viewModelScope.launch { initConfig(widgetId, presetPackage) }
    }

    private suspend fun initConfig(widgetId: Int, presetPackage: String?) {
        if (widgetId > 0) {
            val existing = repository.get(widgetId)
            if (existing != null) {
                setConfig(existing)
                return
            }
            // Widget tout juste posé après un épinglage depuis l'application : on reprend la configuration en attente.
            val adopted = repository.adoptPending(widgetId)
            if (adopted != null) {
                setConfig(adopted)
                updater.refresh(widgetId)
                _events.send(EditorEvent.Saved(widgetId))
                return
            }
            setConfig(WidgetConfig(appWidgetId = widgetId))
            return
        }

        var config = WidgetConfig(appWidgetId = WidgetConfig.NO_ID)
        if (presetPackage != null) {
            val label = appsSource.labelOf(presetPackage)
            if (label != null) {
                config = config.copy(targetPackage = presetPackage, targetLabel = label, name = label)
            }
        }
        setConfig(config)
    }

    private suspend fun setConfig(config: WidgetConfig) {
        val icon = loadIcon(config.targetPackage)
        _state.update { it.copy(config = config, previewIcon = icon, loading = false) }
        loadPreviewItems(config.contentType)
    }

    /** Aperçu : vrais éléments si le cache en contient, sinon l'interface affichera des exemples. */
    private suspend fun loadPreviewItems(type: WidgetContentType) {
        val cached = try {
            sources.forType(type)?.snapshot()?.notes.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        _state.update { if (it.config.contentType == type) it.copy(previewNotes = cached) else it }
    }

    private suspend fun loadIcon(packageName: String?): ImageBitmap? =
        packageName?.let { icons.icon(it, PREVIEW_ICON_PX)?.asImageBitmap() }

    /** Icône de liste pour le sélecteur d'applications. */
    suspend fun iconFor(packageName: String): ImageBitmap? =
        icons.icon(packageName, LIST_ICON_PX)?.asImageBitmap()

    fun update(transform: (WidgetConfig) -> WidgetConfig) {
        val before = _state.value.config.contentType
        _state.update { it.copy(config = transform(it.config)) }
        val after = _state.value.config.contentType
        if (after != before) {
            _state.update { it.copy(previewNotes = emptyList()) }
            viewModelScope.launch { loadPreviewItems(after) }
        }
    }

    fun setPinSize(size: WidgetSizeOption) {
        _state.update { it.copy(pinSize = size) }
    }

    fun selectApp(app: AppInfo) {
        _state.update { state ->
            val previous = state.config
            // Le nom suit l'application tant que l'utilisateur ne l'a pas personnalisé.
            val followsApp = previous.name.isBlank() || previous.name == previous.targetLabel
            state.copy(
                config = previous.copy(
                    targetPackage = app.packageName,
                    targetLabel = app.label,
                    name = if (followsApp) app.label else previous.name,
                ),
            )
        }
        viewModelScope.launch {
            val icon = loadIcon(app.packageName)
            _state.update { if (it.config.targetPackage == app.packageName) it.copy(previewIcon = icon) else it }
        }
    }

    fun clearApp() {
        _state.update { it.copy(config = it.config.copy(targetPackage = null, targetLabel = null), previewIcon = null) }
    }

    /** Enregistre un widget déjà posé et le redessine. */
    fun save() {
        val config = _state.value.config.normalized()
        if (!_state.value.canSave || !_state.value.isPlaced) return
        viewModelScope.launch {
            try {
                repository.save(config)
                updater.refresh(config.appWidgetId)
                _events.send(EditorEvent.Saved(config.appWidgetId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(EditorEvent.Message(R.string.error_save))
            }
        }
    }

    /** Mémorise la configuration puis demande au lanceur d'épingler un nouveau widget. */
    fun pinToHome() {
        val state = _state.value
        if (!state.canSave || state.isPlaced) return
        val config = state.config.normalized()
        viewModelScope.launch {
            try {
                repository.savePending(config)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(EditorEvent.Message(R.string.error_save))
                return@launch
            }
            when (pinner.requestPin(state.pinSize)) {
                PinResult.REQUESTED -> _events.send(EditorEvent.PinRequested)
                PinResult.UNSUPPORTED -> _events.send(EditorEvent.Message(R.string.error_pin_unsupported))
                PinResult.FAILED -> _events.send(EditorEvent.Message(R.string.error_pin_failed))
            }
        }
    }

    private fun WidgetConfig.normalized(): WidgetConfig = copy(
        name = name.trim().ifEmpty { targetLabel ?: DEFAULT_NAME },
        opacity = opacity.coerceIn(0f, 1f),
    )

    private companion object {
        const val PREVIEW_ICON_PX = 192
        const val LIST_ICON_PX = 96
        const val DEFAULT_NAME = "Widget"
    }
}
