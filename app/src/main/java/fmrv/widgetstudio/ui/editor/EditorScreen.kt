package fmrv.widgetstudio.ui.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.BackgroundStyle
import fmrv.widgetstudio.domain.model.WidgetConfig
import fmrv.widgetstudio.domain.model.WidgetContentType
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetLayout
import fmrv.widgetstudio.domain.model.WidgetShape
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.domain.model.WidgetTemplate
import fmrv.widgetstudio.ui.components.AppIconImage
import fmrv.widgetstudio.ui.components.WidgetPreview
import fmrv.widgetstudio.ui.components.rememberAppIcon
import fmrv.widgetstudio.ui.components.rememberHaptics
import fmrv.widgetstudio.viewmodel.EditorEvent
import fmrv.widgetstudio.viewmodel.EditorViewModel
import fmrv.widgetstudio.widget.WidgetBackground

/** Types proposés dans le sélecteur (utile surtout quand le lanceur pose un widget sans modèle choisi). */
private val EditableTypes = listOf(
    WidgetContentType.APP_SHORTCUT to R.string.type_shortcut,
    WidgetContentType.APP_GRID to R.string.tpl_grid,
    WidgetContentType.SEARCH_BAR to R.string.tpl_search,
    WidgetContentType.CLOCK_DATE to R.string.type_clock,
    WidgetContentType.DEVICE_STATUS to R.string.tpl_device,
    WidgetContentType.NEWS_FEED to R.string.type_news,
    WidgetContentType.NOTES_LIST to R.string.type_notes,
    WidgetContentType.MAIL_INBOX to R.string.type_mail,
)

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
    template: WidgetTemplate? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(widgetId, presetPackage, template?.id) { viewModel.start(widgetId, presetPackage, template) }
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
    val type = config.contentType
    val isPlaced = state.isPlaced

    // Icônes des applications du dossier, pour l'aperçu.
    val folderIcons by produceState<Map<String, ImageBitmap>>(emptyMap(), config.packages) {
        value = config.packages.mapNotNull { pkg -> viewModel.iconFor(pkg)?.let { pkg to it } }.toMap()
    }

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
                    .weight(0.36f)
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFFE59A74), Color(0xFFB4533A)))),
                contentAlignment = Alignment.Center,
            ) {
                val previewSize = if (isPlaced) WidgetSizeOption.MEDIUM else state.pinSize
                WidgetPreview(
                    config = config,
                    icon = state.previewIcon,
                    notes = state.previewNotes,
                    icons = folderIcons,
                    sizeOption = previewSize,
                    scale = previewScale(previewSize),
                )
            }

            Surface(
                modifier = Modifier.weight(0.64f).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    TypeSection(config.contentType, haptics::tick) { newType ->
                        viewModel.update { it.copy(contentType = newType) }
                    }

                    ContentSection(
                        config = config,
                        previewIcon = state.previewIcon,
                        folderIcons = folderIcons,
                        loadIcon = { viewModel.iconFor(it) },
                        onPickApp = { showPicker = true },
                        onUpdate = viewModel::update,
                        onRemovePackage = viewModel::removePackage,
                        onClearApp = viewModel::clearApp,
                        onTick = haptics::tick,
                    )

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

                    StyleSection(config, haptics::tick, viewModel::update)

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
                if (type == WidgetContentType.APP_GRID) viewModel.addPackage(app) else viewModel.selectApp(app)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

private const val MAX_NAME_LENGTH = 40

private fun previewScale(size: WidgetSizeOption): Float = when (size) {
    WidgetSizeOption.WIDE, WidgetSizeOption.BAR -> 0.78f
    WidgetSizeOption.MEDIUM -> 0.8f
    WidgetSizeOption.SMALL -> 1.2f
}

@Composable
private fun TypeSection(current: WidgetContentType, tick: () -> Unit, onSelect: (WidgetContentType) -> Unit) {
    Section(R.string.section_type) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
            items(EditableTypes) { (type, label) ->
                ChoiceChip(
                    selected = current == type,
                    label = stringResource(label),
                    onClick = { tick(); onSelect(type) },
                )
            }
        }
    }
}

/** Réglages propres au type de widget : application cible, dossier, libellés, options. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.ContentSection(
    config: WidgetConfig,
    previewIcon: ImageBitmap?,
    folderIcons: Map<String, ImageBitmap>,
    loadIcon: suspend (String) -> ImageBitmap?,
    onPickApp: () -> Unit,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
    onRemovePackage: (String) -> Unit,
    onClearApp: () -> Unit,
    onTick: () -> Unit,
) {
    when (config.contentType) {
        WidgetContentType.APP_SHORTCUT -> {
            Section(R.string.section_app) { AppRow(config.targetLabel, previewIcon, onPickApp) }
            Section(R.string.section_layout) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(config.layout == WidgetLayout.ICON, stringResource(R.string.layout_icon), {
                        onTick(); onUpdate { it.copy(layout = WidgetLayout.ICON) }
                    })
                    ChoiceChip(config.layout == WidgetLayout.PILL, stringResource(R.string.layout_pill), {
                        onTick(); onUpdate { it.copy(layout = WidgetLayout.PILL, shape = WidgetShape.PILL) }
                    })
                }
            }
            if (config.layout == WidgetLayout.PILL) {
                Section(R.string.section_label_pill) { LabelField(config.label, R.string.pill_hint, onUpdate) }
            } else {
                SwitchRow(R.string.option_show_label, config.showLabel) { value ->
                    onTick(); onUpdate { it.copy(showLabel = value) }
                }
            }
        }
        WidgetContentType.SEARCH_BAR -> {
            Section(R.string.section_app) { AppRow(config.targetLabel, previewIcon, onPickApp) }
            Section(R.string.section_label_search) { LabelField(config.label, R.string.search_hint, onUpdate) }
        }
        WidgetContentType.APP_GRID -> {
            Section(R.string.section_apps_folder) {
                config.packages.forEach { pkg ->
                    FolderAppRow(pkg, folderIcons[pkg], loadIcon) { onTick(); onRemovePackage(pkg) }
                }
                if (config.packages.size < WidgetConfig.MAX_GRID_APPS) {
                    OutlinedButton(
                        onClick = onPickApp,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.folder_add))
                    }
                } else {
                    Text(
                        stringResource(R.string.folder_full),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(R.string.grid_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SwitchRow(R.string.option_show_label, config.showLabel) { value ->
                onTick(); onUpdate { it.copy(showLabel = value) }
            }
        }
        WidgetContentType.CLOCK_DATE -> {
            Section(R.string.section_app_optional) {
                AppRow(config.targetLabel, previewIcon, onPickApp)
                if (config.targetPackage != null) {
                    TextButton(onClick = onClearApp) { Text(stringResource(R.string.app_clear)) }
                }
            }
            SwitchRow(R.string.option_24h, config.use24Hour) { value -> onTick(); onUpdate { it.copy(use24Hour = value) } }
            SwitchRow(R.string.option_show_date, config.showDate) { value -> onTick(); onUpdate { it.copy(showDate = value) } }
        }
        WidgetContentType.DEVICE_STATUS -> Hint(R.string.device_hint)
        WidgetContentType.WEATHER_CLOCK -> Unit
        WidgetContentType.NOTES_LIST -> Hint(R.string.notes_source_hint)
        WidgetContentType.NEWS_FEED -> Hint(R.string.news_source_hint)
        WidgetContentType.MAIL_INBOX -> Hint(R.string.mail_source_hint)
    }
}

/** Style prédéfini, puis réglages fins du fond, des couleurs, de la forme et de la police. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleSection(
    config: WidgetConfig,
    tick: () -> Unit,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
) {
    Section(R.string.section_style) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(StylePresets.all) { preset ->
                PresetSwatch(preset, preset.isAppliedTo(config)) { tick(); onUpdate { preset.applyTo(it) } }
            }
        }
    }

    Section(R.string.section_background_kind) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(config.background == BackgroundStyle.SOLID, stringResource(R.string.bg_solid), {
                tick(); onUpdate { it.copy(background = BackgroundStyle.SOLID) }
            })
            ChoiceChip(config.background == BackgroundStyle.GRADIENT, stringResource(R.string.bg_gradient), {
                tick(); onUpdate { it.copy(background = BackgroundStyle.GRADIENT) }
            })
            ChoiceChip(config.background == BackgroundStyle.GLASS, stringResource(R.string.bg_glass), {
                tick(); onUpdate { it.copy(background = BackgroundStyle.GLASS) }
            })
        }
    }

    Section(R.string.section_color_start) {
        ColorPalette(selected = config.backgroundColor, onSelect = { color ->
            tick(); onUpdate { it.copy(backgroundColor = color) }
        })
    }
    if (config.background == BackgroundStyle.GRADIENT) {
        Section(R.string.section_color_end) {
            ColorPalette(selected = config.backgroundEnd, onSelect = { color ->
                tick(); onUpdate { it.copy(backgroundEnd = color) }
            })
        }
    }

    Section(R.string.section_text_color) {
        ColorPalette(selected = config.textColor, onSelect = { color -> tick(); onUpdate { it.copy(textColor = color) } })
    }
    Section(R.string.section_accent) {
        ColorPalette(selected = config.accentColor, onSelect = { color -> tick(); onUpdate { it.copy(accentColor = color) } })
    }

    Section(R.string.section_opacity) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = config.opacity,
                onValueChange = { value -> onUpdate { it.copy(opacity = value) } },
                onValueChangeFinished = tick,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(config.opacity * 100).toInt()} %",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(56.dp),
            )
        }
    }

    Section(R.string.section_shape) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetShape.entries.forEach { shape ->
                ChoiceChip(config.shape == shape, stringResource(shapeLabel(shape)), {
                    tick(); onUpdate { it.copy(shape = shape) }
                })
            }
        }
    }

    Section(R.string.section_font) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetFont.entries.forEach { font ->
                ChoiceChip(config.font == font, stringResource(fontLabel(font)), {
                    tick(); onUpdate { it.copy(font = font) }
                }, fontFamily(font))
            }
        }
    }
}

/** Pastille d'un style : aperçu du fond (dégradé, verre ou uni) sur un damier qui révèle la transparence. */
@Composable
private fun PresetSwatch(preset: StylePreset, selected: Boolean, onClick: () -> Unit) {
    val sample = WidgetConfig(
        appWidgetId = WidgetConfig.NO_ID,
        background = preset.background,
        backgroundColor = preset.start,
        backgroundEnd = preset.end,
        opacity = preset.opacity,
    )
    val (start, end) = WidgetBackground.colors(sample)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier.width(64.dp).clickable(role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFE59A74), Color(0xFFB4533A))))
                .border(if (selected) 3.dp else 1.dp, borderColor, RoundedCornerShape(18.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(Brush.linearGradient(listOf(Color(start), Color(end))), RoundedCornerShape(14.dp)),
            )
        }
        Text(
            stringResource(preset.name),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRow(label: String?, icon: ImageBitmap?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconImage(icon, 40.dp)
            Spacer(Modifier.width(16.dp))
            Text(
                text = label ?: stringResource(R.string.app_choose),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderAppRow(
    packageName: String,
    icon: ImageBitmap?,
    loadIcon: suspend (String) -> ImageBitmap?,
    onRemove: () -> Unit,
) {
    val loaded by rememberAppIcon(packageName, loadIcon)
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconImage(icon ?: loaded, 36.dp)
            Spacer(Modifier.width(12.dp))
            Text(packageName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.folder_remove))
            }
        }
    }
}

@Composable
private fun LabelField(value: String, @StringRes hint: Int, onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onUpdate { it.copy(label = text.take(MAX_NAME_LENGTH)) } },
        singleLine = true,
        supportingText = { Text(stringResource(hint)) },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Hint(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

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
    WidgetSizeOption.BAR -> R.string.size_bar
}
