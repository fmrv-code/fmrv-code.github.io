package fmrv.widgetstudio.ui.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetShape
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.ui.components.AppIconImage
import fmrv.widgetstudio.ui.components.WidgetPreview
import fmrv.widgetstudio.ui.components.rememberHaptics
import fmrv.widgetstudio.viewmodel.EditorEvent
import fmrv.widgetstudio.viewmodel.EditorViewModel

/**
 * Éditeur visuel de widget. Aperçu en haut, commandes dans la moitié basse (zone du pouce),
 * action principale ancrée tout en bas.
 *
 * Utilisé à la fois dans l'application (création : épinglage) et dans l'activité de configuration
 * appelée par le lanceur (widget déjà posé : enregistrement).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    widgetId: Int,
    presetPackage: String?,
    onClose: () -> Unit,
    onSaved: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(widgetId, presetPackage) { viewModel.start(widgetId, presetPackage) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.Saved -> {
                    haptics.confirm()
                    onSaved(event.appWidgetId)
                }
                EditorEvent.PinRequested -> onClose()
                is EditorEvent.Message -> snackbar.showSnackbar(context.getString(event.messageRes))
            }
        }
    }

    val config = state.config
    val isPlaced = state.isPlaced

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isPlaced) R.string.editor_title_edit else R.string.editor_title_new)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Button(
                    onClick = {
                        haptics.confirm()
                        if (isPlaced) viewModel.save() else viewModel.pinToHome()
                    },
                    enabled = state.canSave && !state.loading,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(if (isPlaced) R.string.editor_save else R.string.editor_add_to_home))
                }
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            // Fond type « fond d'écran » : rend la transparence du widget visible dans l'aperçu.
            Box(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF5D7BA8), Color(0xFFB7C6DE)))),
                contentAlignment = Alignment.Center,
            ) {
                val previewSize = if (isPlaced) WidgetSizeOption.MEDIUM else state.pinSize
                WidgetPreview(
                    config = config,
                    icon = state.previewIcon,
                    notes = state.previewNotes,
                    sizeOption = previewSize,
                    scale = if (previewSize == WidgetSizeOption.WIDE) 0.7f else 0.8f,
                )
            }

            Surface(
                modifier = Modifier.weight(0.62f).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Section(R.string.section_type) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(
                                selected = config.contentType == WidgetContentType.APP_SHORTCUT,
                                label = stringResource(R.string.type_shortcut),
                                onClick = { haptics.tick(); viewModel.update { it.copy(contentType = WidgetContentType.APP_SHORTCUT) } },
                            )
                            ChoiceChip(
                                selected = config.contentType == WidgetContentType.CLOCK_DATE,
                                label = stringResource(R.string.type_clock),
                                onClick = { haptics.tick(); viewModel.update { it.copy(contentType = WidgetContentType.CLOCK_DATE) } },
                            )
                            val defaultNames = listOf(
                                stringResource(R.string.type_notes),
                                stringResource(R.string.type_news),
                                stringResource(R.string.type_mail),
                            )
                            listOf(
                                WidgetContentType.NOTES_LIST to defaultNames[0],
                                WidgetContentType.NEWS_FEED to defaultNames[1],
                                WidgetContentType.MAIL_INBOX to defaultNames[2],
                            ).forEach { (type, typeName) ->
                                ChoiceChip(
                                    selected = config.contentType == type,
                                    label = typeName,
                                    onClick = {
                                        haptics.tick()
                                        viewModel.update {
                                            // Le nom suit le type tant qu'il est vide ou égal à un nom par défaut.
                                            val followsType = it.name.isBlank() || it.name in defaultNames
                                            it.copy(contentType = type, name = if (followsType) typeName else it.name)
                                        }
                                    },
                                )
                            }
                        }
                    }

                    if (config.contentType.isList) {
                        Text(
                            text = stringResource(listHint(config.contentType)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (!config.contentType.isList) Section(
                        if (config.contentType == WidgetContentType.APP_SHORTCUT) R.string.section_app else R.string.section_app_optional,
                    ) {
                        Surface(
                            onClick = { showPicker = true },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIconImage(state.previewIcon, 40.dp)
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = config.targetLabel ?: stringResource(R.string.app_choose),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                )
                                Text(
                                    stringResource(R.string.app_change),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                        if (config.contentType == WidgetContentType.CLOCK_DATE && config.targetPackage != null) {
                            TextButton(onClick = { viewModel.clearApp() }) { Text(stringResource(R.string.app_clear)) }
                        }
                    }

                    Section(R.string.section_name) {
                        OutlinedTextField(
                            value = config.name,
                            onValueChange = { value -> viewModel.update { it.copy(name = value.take(MAX_NAME_LENGTH)) } },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (!isPlaced) {
                        Section(R.string.section_size) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WidgetSizeOption.entries.forEach { size ->
                                    ChoiceChip(
                                        selected = state.pinSize == size,
                                        label = stringResource(sizeLabel(size)),
                                        onClick = { haptics.tick(); viewModel.setPinSize(size) },
                                    )
                                }
                            }
                        }
                    }

                    Section(R.string.section_shape) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WidgetShape.entries.forEach { shape ->
                                ChoiceChip(
                                    selected = config.shape == shape,
                                    label = stringResource(shapeLabel(shape)),
                                    onClick = { haptics.tick(); viewModel.update { it.copy(shape = shape) } },
                                )
                            }
                        }
                    }

                    Section(R.string.section_background) {
                        ColorPalette(selected = config.backgroundColor, onSelect = { color ->
                            haptics.tick(); viewModel.update { it.copy(backgroundColor = color) }
                        })
                    }

                    Section(R.string.section_text_color) {
                        ColorPalette(selected = config.textColor, onSelect = { color ->
                            haptics.tick(); viewModel.update { it.copy(textColor = color) }
                        })
                    }

                    Section(R.string.section_opacity) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = config.opacity,
                                onValueChange = { value -> viewModel.update { it.copy(opacity = value) } },
                                onValueChangeFinished = { haptics.tick() },
                                valueRange = 0.1f..1f,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${(config.opacity * 100).toInt()} %",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.width(56.dp),
                            )
                        }
                    }

                    Section(R.string.section_font) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WidgetFont.entries.forEach { font ->
                                ChoiceChip(
                                    selected = config.font == font,
                                    label = stringResource(fontLabel(font)),
                                    fontFamily = fontFamily(font),
                                    onClick = { haptics.tick(); viewModel.update { it.copy(font = font) } },
                                )
                            }
                        }
                    }

                    when (config.contentType) {
                        WidgetContentType.APP_SHORTCUT -> SwitchRow(R.string.option_show_label, config.showLabel) { value ->
                            haptics.tick(); viewModel.update { it.copy(showLabel = value) }
                        }
                        WidgetContentType.NOTES_LIST, WidgetContentType.NEWS_FEED, WidgetContentType.MAIL_INBOX -> Unit
                        WidgetContentType.CLOCK_DATE -> {
                            SwitchRow(R.string.option_24h, config.use24Hour) { value ->
                                haptics.tick(); viewModel.update { it.copy(use24Hour = value) }
                            }
                            SwitchRow(R.string.option_show_date, config.showDate) { value ->
                                haptics.tick(); viewModel.update { it.copy(showDate = value) }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showPicker) {
        AppPickerSheet(
            apps = state.apps,
            loadIcon = { viewModel.iconFor(it) },
            onSelect = { app ->
                haptics.confirm()
                viewModel.selectApp(app)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

private const val MAX_NAME_LENGTH = 40

@Composable
private fun Section(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceChip(selected: Boolean, label: String, onClick: () -> Unit, fontFamily: FontFamily? = null) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontFamily = fontFamily) },
        shape = CircleShape,
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

@Composable
private fun SwitchRow(@StringRes label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@StringRes
private fun listHint(type: WidgetContentType): Int = when (type) {
    WidgetContentType.NEWS_FEED -> R.string.news_source_hint
    WidgetContentType.MAIL_INBOX -> R.string.mail_source_hint
    else -> R.string.notes_source_hint
}

@StringRes
private fun shapeLabel(shape: WidgetShape): Int = when (shape) {
    WidgetShape.SQUARE -> R.string.shape_square
    WidgetShape.ROUNDED -> R.string.shape_rounded
    WidgetShape.PILL -> R.string.shape_pill
}

@StringRes
private fun fontLabel(font: WidgetFont): Int = when (font) {
    WidgetFont.SANS -> R.string.font_sans
    WidgetFont.SERIF -> R.string.font_serif
    WidgetFont.MONO -> R.string.font_mono
    WidgetFont.CURSIVE -> R.string.font_cursive
}

private fun fontFamily(font: WidgetFont): FontFamily = when (font) {
    WidgetFont.SANS -> FontFamily.SansSerif
    WidgetFont.SERIF -> FontFamily.Serif
    WidgetFont.MONO -> FontFamily.Monospace
    WidgetFont.CURSIVE -> FontFamily.Cursive
}

@StringRes
private fun sizeLabel(size: WidgetSizeOption): Int = when (size) {
    WidgetSizeOption.SMALL -> R.string.size_small
    WidgetSizeOption.MEDIUM -> R.string.size_medium
    WidgetSizeOption.WIDE -> R.string.size_wide
}
