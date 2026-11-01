package fmrv.widgetstudio.viewmodel

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.AppInfo
import fmrv.widgetstudio.domain.model.Params
import fmrv.widgetstudio.domain.model.PinResult
import fmrv.widgetstudio.domain.model.StylePreset
import fmrv.widgetstudio.domain.model.WidgetInstance
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.domain.model.WidgetStyle
import fmrv.widgetstudio.domain.repository.AppIconProvider
import fmrv.widgetstudio.domain.repository.AppsSource
import fmrv.widgetstudio.domain.repository.WidgetPinner
import fmrv.widgetstudio.domain.repository.WidgetRepository
import fmrv.widgetstudio.domain.repository.WidgetUpdater
import fmrv.widgetstudio.kinds.KindCatalog
import fmrv.widgetstudio.kinds.WidgetKind
import fmrv.widgetstudio.kinds.appLabelKey
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
    /** `null` tant qu'aucun type de widget n'est choisi (widget posé depuis le lanceur sans modèle). */
    val instance: WidgetInstance? = null,
    val kind: WidgetKind<*>? = null,
    val apps: List<AppInfo> = emptyList(),
    val pinSize: WidgetSizeOption = WidgetSizeOption.MEDIUM,
    val loading: Boolean = true,
) {
    val needsKind: Boolean get() = !loading && instance == null

    /** Vrai quand le widget existe déjà sur l'écran d'accueil (édition) ; faux pour une création. */
    val isPlaced: Boolean get() = (instance?.appWidgetId ?: 0) > 0

    val canSave: Boolean
        get() {
            val current = instance ?: return false
            val k = kind ?: return false
            return k.isComplete(current.params.withDefaults(k.defaultParams))
        }
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
    private val catalog: KindCatalog,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    private var started = false

    /**
     * Initialise l'éditeur. Idempotent.
     * - [widgetId] > 0 : widget posé (édition, ou configuration initiale à la pose).
     * - [widgetId] == 0 : création depuis la galerie pour le type [kindId].
     * [presetPackage] pré-remplit le réglage "app" (depuis l'onglet Applications).
     */
    fun start(widgetId: Int, kindId: String?, presetPackage: String?) {
        if (started) return
        started = true
        viewModelScope.launch {
            val apps = runCatching { appsSource.loadApps() }.getOrDefault(emptyList())
            _state.update { it.copy(apps = apps) }
        }
        viewModelScope.launch { initInstance(widgetId, kindId, presetPackage) }
    }

    private suspend fun initInstance(widgetId: Int, kindId: String?, presetPackage: String?) {
        if (widgetId > 0) {
            val existing = repository.get(widgetId)
            if (existing != null) {
                setInstance(existing)
                return
            }
            // Widget tout juste posé après un épinglage depuis l'application : on reprend la configuration en attente.
            val adopted = repository.adoptPending(widgetId)
            if (adopted != null) {
                setInstance(adopted)
                updater.refresh(widgetId)
                _events.send(EditorEvent.Saved(widgetId))
                return
            }
            // Posé depuis le menu des widgets : l'utilisateur choisit d'abord un type.
            pendingWidgetId = widgetId
            _state.update { it.copy(loading = false, instance = null) }
            return
        }

        val kind = kindId?.let(catalog::byId)
        if (kind == null) {
            _state.update { it.copy(loading = false, instance = null) }
            return
        }
        var instance = newInstance(kind, WidgetInstance.NO_ID)
        if (presetPackage != null) {
            val label = appsSource.labelOf(presetPackage)
            instance = instance.copy(
                name = label.orEmpty(),
                params = instance.params.with("app", presetPackage).with(appLabelKey("app"), label.orEmpty()),
            )
        }
        setInstance(instance, pinSize = kind.defaultSize)
    }

    private fun newInstance(kind: WidgetKind<*>, appWidgetId: Int) = WidgetInstance(
        appWidgetId = appWidgetId,
        kindId = kind.id,
        name = "",
        style = kind.defaultStyle,
        params = Params(kind.defaultParams),
    )

    private fun setInstance(instance: WidgetInstance, pinSize: WidgetSizeOption? = null) {
        val kind = catalog.byId(instance.kindId)
        _state.update {
            it.copy(
                instance = instance,
                kind = kind,
                pinSize = pinSize ?: kind?.defaultSize ?: it.pinSize,
                loading = false,
            )
        }
    }

    /** Choix du type pour un widget posé depuis le lanceur. */
    fun chooseKind(kindId: String) {
        val kind = catalog.byId(kindId) ?: return
        val widgetId = _state.value.instance?.appWidgetId ?: pendingWidgetId
        setInstance(newInstance(kind, widgetId), pinSize = kind.defaultSize)
    }

    /** Identifiant du widget posé depuis le lanceur, conservé tant qu'aucun type n'est choisi. */
    private var pendingWidgetId: Int = WidgetInstance.NO_ID

    /** Suspend car l'icône est chargée à la demande (liste des applications). */
    suspend fun iconFor(packageName: String): ImageBitmap? =
        icons.icon(packageName, LIST_ICON_PX)?.asImageBitmap()

    fun setName(value: String) = updateInstance { it.copy(name = value.take(MAX_NAME_LENGTH)) }

    fun setStyle(transform: (WidgetStyle) -> WidgetStyle) = updateInstance { it.copy(style = transform(it.style)) }

    fun applyPreset(preset: StylePreset) = setStyle(preset::applyTo)

    fun setParam(key: String, value: String) = updateInstance { it.copy(params = it.params.with(key, value)) }

    fun setBool(key: String, value: Boolean) = updateInstance { it.copy(params = it.params.withBool(key, value)) }

    fun setPinSize(size: WidgetSizeOption) = _state.update { it.copy(pinSize = size) }

    /** Choisit l'application d'un réglage et mémorise son nom ; le nom du widget suit l'application tant qu'il n'a pas été personnalisé. */
    fun selectApp(key: String, app: AppInfo) = updateInstance { current ->
        val previousLabel = current.params.string(appLabelKey(key))
        val followsApp = current.name.isBlank() || current.name == previousLabel
        current.copy(
            name = if (followsApp) app.label else current.name,
            params = current.params.with(key, app.packageName).with(appLabelKey(key), app.label),
        )
    }

    fun clearApp(key: String) = updateInstance { it.copy(params = it.params.without(key).without(appLabelKey(key))) }

    /** Ajoute une application à une liste (dossier) ; ignore les doublons et le dépassement de [max]. */
    fun addToList(key: String, app: AppInfo, max: Int) = updateInstance { current ->
        val list = current.params.list(key)
        if (app.packageName in list || list.size >= max) current else current.copy(params = current.params.withList(key, list + app.packageName))
    }

    fun removeFromList(key: String, packageName: String) = updateInstance { current ->
        current.copy(params = current.params.withList(key, current.params.list(key) - packageName))
    }

    private fun updateInstance(transform: (WidgetInstance) -> WidgetInstance) {
        _state.update { state ->
            val current = state.instance ?: return@update state
            state.copy(instance = transform(current))
        }
    }

    /** Enregistre un widget déjà posé et le redessine. */
    fun save() {
        val state = _state.value
        val instance = state.instance ?: return
        if (!state.canSave || !state.isPlaced) return
        viewModelScope.launch {
            try {
                repository.save(instance)
                updater.refresh(instance.appWidgetId)
                _events.send(EditorEvent.Saved(instance.appWidgetId))
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
        val instance = state.instance ?: return
        if (!state.canSave || state.isPlaced) return
        viewModelScope.launch {
            try {
                repository.savePending(instance)
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

    private companion object {
        const val LIST_ICON_PX = 96
        const val MAX_NAME_LENGTH = 40
    }
}
