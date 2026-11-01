package fmrv.widgetstudio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fmrv.widgetstudio.domain.model.WidgetInstance
import fmrv.widgetstudio.domain.repository.WidgetRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: WidgetRepository,
) : ViewModel() {

    /** `null` tant que la première lecture n'est pas terminée (évite un flash d'état vide). */
    val widgets: StateFlow<List<WidgetInstance>?> = repository.observeWidgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
