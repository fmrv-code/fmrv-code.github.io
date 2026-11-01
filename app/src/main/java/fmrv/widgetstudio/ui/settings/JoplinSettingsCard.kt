package fmrv.widgetstudio.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fmrv.widgetstudio.R
import fmrv.widgetstudio.ui.components.OneUiCard
import fmrv.widgetstudio.ui.components.rememberHaptics
import fmrv.widgetstudio.viewmodel.JoplinEvent
import fmrv.widgetstudio.viewmodel.JoplinSettingsViewModel

/** Connexion à OneDrive pour afficher les notes Joplin dans un widget. */
@Composable
fun JoplinSettingsCard(viewModel: JoplinSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = rememberHaptics()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is JoplinEvent.OpenUrl -> try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(event.url)))
                } catch (e: ActivityNotFoundException) {
                    // Aucun navigateur : l'utilisateur est prévenu par le message d'état ci-dessous.
                }
            }
        }
    }

    OneUiCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.joplin_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.joplin_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.connected) {
                val status = state.noteCount?.let {
                    val whenText = DateUtils.getRelativeTimeSpanString(
                        state.lastRefreshMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                    )
                    stringResource(R.string.joplin_status_connected, it, whenText.toString())
                } ?: stringResource(R.string.joplin_status_connected_empty)
                Text(status, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = stringResource(R.string.joplin_setup_steps, state.redirectUri),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.clientId,
                    onValueChange = viewModel::onClientIdChange,
                    label = { Text(stringResource(R.string.joplin_client_id)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = state.folder,
                onValueChange = viewModel::onFolderChange,
                label = { Text(stringResource(R.string.joplin_folder)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            state.message?.let { message ->
                Text(
                    text = if (message.arg != null) stringResource(message.res, message.arg) else stringResource(message.res),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (state.connected) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { haptics.confirm(); viewModel.refreshNow() },
                        enabled = !state.busy,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        if (state.busy) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(stringResource(R.string.joplin_refresh))
                        }
                    }
                    OutlinedButton(
                        onClick = { haptics.tick(); viewModel.disconnect() },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.joplin_disconnect))
                    }
                }
            } else {
                Button(
                    onClick = { haptics.confirm(); viewModel.connect() },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.joplin_connect))
                }
            }
        }
    }
}
