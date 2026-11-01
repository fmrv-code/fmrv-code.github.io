package fmrv.widgetstudio.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import fmrv.widgetstudio.ui.editor.EditorScreen
import fmrv.widgetstudio.ui.theme.WidgetStudioTheme
import fmrv.widgetstudio.ui.theme.resolveDarkTheme
import fmrv.widgetstudio.viewmodel.EditorViewModel
import fmrv.widgetstudio.viewmodel.SettingsViewModel

/**
 * Lancée par le système quand l'utilisateur pose (ou reconfigure) un widget.
 * Le résultat doit rester RESULT_CANCELED tant que la configuration n'est pas enregistrée :
 * le lanceur retire alors le widget fraîchement posé.
 */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    private val editorViewModel: EditorViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED, resultIntent(appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val dark = resolveDarkTheme(settings.themeMode, isSystemInDarkTheme())
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            WidgetStudioTheme(settings) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EditorScreen(
                        viewModel = editorViewModel,
                        widgetId = appWidgetId,
                        presetPackage = null,
                        onClose = { finish() },
                        onSaved = {
                            setResult(Activity.RESULT_OK, resultIntent(appWidgetId))
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun resultIntent(appWidgetId: Int): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
