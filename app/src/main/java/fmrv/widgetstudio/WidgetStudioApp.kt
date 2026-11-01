package fmrv.widgetstudio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import fmrv.widgetstudio.widget.NotesRefreshWorker
import fmrv.widgetstudio.widget.WidgetReconciler
import fmrv.widgetstudio.widget.WidgetRefreshWorker
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class WidgetStudioApp : Application() {

    @Inject lateinit var reconciler: WidgetReconciler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        WidgetRefreshWorker.schedule(this)
        NotesRefreshWorker.schedulePeriodic(this)
        scope.launch {
            try {
                reconciler.run()
            } catch (e: Exception) {
                // Nettoyage opportuniste : un échec ici ne doit jamais empêcher le démarrage.
            }
        }
    }
}
