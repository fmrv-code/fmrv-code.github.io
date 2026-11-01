package fmrv.widgetstudio.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.repository.AppIconProvider
import fmrv.widgetstudio.domain.repository.WidgetRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: WidgetRepository,
    private val icons: AppIconProvider,
) : ViewModel() {

    /** `null` tant que la première lecture n'est pas terminée (évite un flash d'état vide). */
    val widgets: StateFlow<List<WidgetConfig>?> = repository.observeWidgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun icon(packageName: String): ImageBitmap? =
        icons.icon(packageName, ICON_PX)?.asImageBitmap()

    private companion object {
        const val ICON_PX = 128
    }
}
