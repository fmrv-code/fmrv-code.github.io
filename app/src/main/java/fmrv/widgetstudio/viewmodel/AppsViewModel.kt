package fmrv.widgetstudio.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fmrv.widgetstudio.domain.model.AppInfo
import fmrv.widgetstudio.domain.repository.AppIconProvider
import fmrv.widgetstudio.domain.repository.AppsSource
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class AppsUiState(
    val loading: Boolean = true,
    val query: String = "",
    val apps: List<AppInfo> = emptyList(),
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appsSource: AppsSource,
    private val icons: AppIconProvider,
) : ViewModel() {

    private val allApps = MutableStateFlow<List<AppInfo>?>(null)
    private val _query = MutableStateFlow("")

    val state: StateFlow<AppsUiState> = combine(allApps, _query) { apps, query ->
        AppsUiState(
            loading = apps == null,
            query = query,
            apps = filterApps(apps.orEmpty(), query),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            allApps.value = runCatching { appsSource.loadApps() }.getOrDefault(emptyList())
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    suspend fun icon(packageName: String): ImageBitmap? =
        icons.icon(packageName, ICON_PX)?.asImageBitmap()

    private companion object {
        const val ICON_PX = 96
    }
}
