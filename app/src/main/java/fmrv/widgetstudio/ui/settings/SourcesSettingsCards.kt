package fmrv.widgetstudio.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fmrv.widgetstudio.R
import fmrv.widgetstudio.ui.components.OneUiCard
import fmrv.widgetstudio.ui.components.rememberHaptics
import fmrv.widgetstudio.viewmodel.MailSettingsViewModel
import fmrv.widgetstudio.viewmodel.NewsSettingsViewModel
import fmrv.widgetstudio.viewmodel.UiMessage

@Composable
private fun CardTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun CardBody(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun MessageLine(message: UiMessage?) {
    if (message == null) return
    Text(
        text = if (message.arg != null) stringResource(message.res, message.arg) else stringResource(message.res),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ActionButton(text: String, busy: Boolean, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        shape = CircleShape,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(text)
        }
    }
}

/** Franceinfo : adresse du flux RSS « À la une » (modifiable). */
@Composable
fun NewsSettingsCard(viewModel: NewsSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()

    OneUiCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardTitle(stringResource(R.string.news_title))
            CardBody(stringResource(R.string.news_intro))
            OutlinedTextField(
                value = state.customUrl,
                onValueChange = viewModel::onUrlChange,
                label = { Text(stringResource(R.string.news_feed_url)) },
                placeholder = { Text(state.defaultUrl) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            MessageLine(state.message)
            ActionButton(stringResource(R.string.news_test), state.busy, Modifier.fillMaxWidth()) {
                haptics.confirm()
                viewModel.refreshNow()
            }
        }
    }
}

/** Gmail : adresse + mot de passe d'application, lecture IMAP seule. */
@Composable
fun MailSettingsCard(viewModel: MailSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()

    OneUiCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardTitle(stringResource(R.string.mail_title))
            CardBody(stringResource(R.string.mail_intro))

            if (state.connected) {
                Text(
                    text = state.unreadCount?.let { stringResource(R.string.mail_status_connected, state.email, it) }
                        ?: stringResource(R.string.mail_status_connected_empty, state.email),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                CardBody(stringResource(R.string.mail_setup_steps))
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text(stringResource(R.string.mail_address)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.appPassword,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text(stringResource(R.string.mail_app_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            MessageLine(state.message)

            if (state.connected) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ActionButton(stringResource(R.string.joplin_refresh), state.busy, modifier = Modifier.weight(1f)) {
                        haptics.confirm()
                        viewModel.refreshNow()
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
                ActionButton(stringResource(R.string.mail_connect), state.busy, modifier = Modifier.fillMaxWidth()) {
                    haptics.confirm()
                    viewModel.connect()
                }
            }
        }
    }
}
