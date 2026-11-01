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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import fmrv.widgetstudio.domain.model.Category
import fmrv.widgetstudio.domain.model.Params
import fmrv.widgetstudio.domain.model.StylePreset
import fmrv.widgetstudio.domain.model.StylePresets
import fmrv.widgetstudio.domain.model.WidgetFont
import fmrv.widgetstudio.domain.model.WidgetShape
import fmrv.widgetstudio.domain.model.WidgetSizeOption
import fmrv.widgetstudio.domain.model.WidgetStyle
import fmrv.widgetstudio.kinds.ChoiceOption
import fmrv.widgetstudio.kinds.Kinds
import fmrv.widgetstudio.kinds.ParamField
import fmrv.widgetstudio.kinds.WidgetKind
import fmrv.widgetstudio.kinds.appLabelKey
import fmrv.widgetstudio.render.Colors
import fmrv.widgetstudio.ui.components.AppIconImage
import fmrv.widgetstudio.ui.components.rememberAppIcon
import fmrv.widgetstudio.ui.components.rememberHaptics
import fmrv.widgetstudio.ui.preview.WidgetPreview
import fmrv.widgetstudio.viewmodel.EditorEvent
import fmrv.widgetstudio.viewmodel.EditorViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Éditeur visuel de widget. Aperçu en haut, commandes dans la moitié basse (zone du pouce), action principale
 * ancrée tout en bas. Le formulaire est généré à partir des champs déclarés par le type de widget.
 *
 * Utilisé à la fois dans l'application (création : épinglage) et dans l'activité de configuration
 * appelée par le lanceur (widget déjà posé : enregistrement).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    widgetId: Int,
    kindId: String?,
    presetPackage: String?,
    onClose: () -> Unit,
    onSaved: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var pickingField by remember { mutableStateOf<ParamField?>(null) }

    LaunchedEffect(widgetId, kindId, presetPackage) { viewModel.start(widgetId, kindId, presetPackage) }
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

    val instance = state.instance
    val kind = state.kind
    val isPlaced = state.isPlaced

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            kind != null -> stringResource(kind.title)
                            else -> stringResource(R.string.editor_title_new)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            if (instance != null) {
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
            }
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            instance == null || kind == null -> KindPicker(Modifier.padding(padding), viewModel::chooseKind)
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                // Fond type « fond d'écran » : rend la transparence et le verre visibles dans l'aperçu.
                Box(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFFE59A74), Color(0xFFB4533A)))),
                    contentAlignment = Alignment.Center,
                ) {
                    val size = if (isPlaced) kind.defaultSize else state.pinSize
                    WidgetPreview(instance, size, scale = previewScale(size))
                }

                Surface(
                    modifier = Modifier.weight(0.62f).fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        val params = instance.params.withDefaults(kind.defaultParams)
                        kind.fields.filter { it.visible(params) }.forEach { field ->
                            FieldEditor(
                                field = field,
                                params = params,
                                viewModel = viewModel,
                                onPickApp = { pickingField = field },
                                tick = haptics::tick,
                            )
                        }

                        Section(R.string.section_name) {
                            OutlinedTextField(
                                value = instance.name,
                                onValueChange = viewModel::setName,
                                singleLine = true,
                                placeholder = { Text(stringResource(kind.title)) },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (!isPlaced) {
                            Section(R.string.section_size) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WidgetSizeOption.entries.forEach { option ->
                                        ChoiceChip(state.pinSize == option, stringResource(sizeLabel(option)), {
                                            haptics.tick(); viewModel.setPinSize(option)
                                        })
                                    }
                                }
                            }
                        }

                        StyleSection(instance.style, haptics::tick, viewModel::setStyle, viewModel::applyPreset)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    pickingField?.let { field ->
        AppPickerSheet(
            apps = state.apps,
            loadIcon = { viewModel.iconFor(it) },
            onSelect = { app ->
                haptics.confirm()
                when (field) {
                    is ParamField.AppPick -> viewModel.selectApp(field.key, app)
                    is ParamField.AppList -> viewModel.addToList(field.key, app, field.max)
                    else -> Unit
                }
                pickingField = null
            },
            onDismiss = { pickingField = null },
        )
    }
}

private fun previewScale(size: WidgetSizeOption): Float = when (size) {
    WidgetSizeOption.SMALL -> 1.3f
    WidgetSizeOption.MEDIUM -> 0.82f
    WidgetSizeOption.WIDE -> 0.78f
    WidgetSizeOption.BAR -> 0.78f
    WidgetSizeOption.LARGE -> 0.52f
}

/** Choix du type pour un widget posé depuis le menu du lanceur, sans modèle de la galerie. */
@Composable
private fun KindPicker(modifier: Modifier, onPick: (String) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(stringResource(R.string.editor_choose_type), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        }
        items(Kinds.all, key = { it.id }) { kind ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth().clickable { onPick(kind.id) },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(kind.title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(kind.description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Contrôle correspondant à un champ déclaré par le type de widget. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FieldEditor(
    field: ParamField,
    params: Params,
    viewModel: EditorViewModel,
    onPickApp: () -> Unit,
    tick: () -> Unit,
) {
    when (field) {
        is ParamField.Toggle -> SwitchRow(field.label, params.bool(field.key)) { value ->
            tick(); viewModel.setBool(field.key, value)
        }
        is ParamField.Choice -> Section(field.label) {
            val current = params.string(field.key)
            if (field.options.size <= MAX_CHIP_OPTIONS) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    field.options.forEach { option ->
                        ChoiceChip(current == option.value, optionLabel(option), { tick(); viewModel.setParam(field.key, option.value) })
                    }
                }
            } else {
                DropdownField(field.options, current) { value -> tick(); viewModel.setParam(field.key, value) }
            }
        }
        is ParamField.Text -> Section(field.label) {
            OutlinedTextField(
                value = params.string(field.key),
                onValueChange = { viewModel.setParam(field.key, it.take(field.maxLength)) },
                singleLine = true,
                placeholder = if (field.hint != 0) ({ Text(stringResource(field.hint)) }) else null,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is ParamField.AppPick -> Section(field.label) {
            val pkg = params.string(field.key)
            val icon by rememberAppIcon(pkg.ifEmpty { null }) { viewModel.iconFor(it) }
            AppRow(params.string(appLabelKey(field.key)).ifEmpty { null }, icon, onPickApp)
            if (pkg.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearApp(field.key) }) { Text(stringResource(R.string.app_clear)) }
            }
        }
        is ParamField.AppList -> Section(field.label) {
            val list = params.list(field.key)
            list.forEach { pkg -> FolderAppRow(pkg, viewModel) { tick(); viewModel.removeFromList(field.key, pkg) } }
            if (list.size < field.max) {
                OutlinedButton(onClick = onPickApp, shape = CircleShape, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.folder_add))
                }
            } else {
                Text(stringResource(R.string.folder_full), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        is ParamField.Date -> Section(field.label) {
            DateField(params.string(field.key)) { iso -> tick(); viewModel.setParam(field.key, iso) }
        }
    }
}

private const val MAX_CHIP_OPTIONS = 6

@Composable
private fun optionLabel(option: ChoiceOption): String =
    if (option.labelRes != 0) stringResource(option.labelRes) else option.label

@Composable
private fun DropdownField(options: List<ChoiceOption>, current: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.value == current }
    Box {
        Surface(
            onClick = { open = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selected?.let { optionLabel(it) } ?: current, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.app_change), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { open = false; onSelect(option.value) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val parsed = runCatching { LocalDate.parse(value) }.getOrNull()
    Surface(
        onClick = { open = true },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(parsed?.toString() ?: stringResource(R.string.date_choose), modifier = Modifier.weight(1f))
            Text(stringResource(R.string.app_change), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = parsed?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    open = false
                }) { Text(stringResource(R.string.date_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.date_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

/** Style prédéfini, puis réglages fins du fond, des couleurs, de la forme et de la police. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleSection(
    style: WidgetStyle,
    tick: () -> Unit,
    onStyle: ((WidgetStyle) -> WidgetStyle) -> Unit,
    onPreset: (StylePreset) -> Unit,
) {
    Section(R.string.section_style) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(StylePresets.all) { preset ->
                PresetSwatch(preset, preset.isAppliedTo(style)) { tick(); onPreset(preset) }
            }
        }
    }
    Section(R.string.section_background_kind) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(style.background == BackgroundStyle.SOLID, stringResource(R.string.bg_solid), { tick(); onStyle { it.copy(background = BackgroundStyle.SOLID) } })
            ChoiceChip(style.background == BackgroundStyle.GRADIENT, stringResource(R.string.bg_gradient), { tick(); onStyle { it.copy(background = BackgroundStyle.GRADIENT) } })
            ChoiceChip(style.background == BackgroundStyle.GLASS, stringResource(R.string.bg_glass), { tick(); onStyle { it.copy(background = BackgroundStyle.GLASS) } })
        }
    }
    Section(R.string.section_color_start) {
        ColorPalette(style.colorStart, { c -> tick(); onStyle { it.copy(colorStart = c) } })
    }
    if (style.background == BackgroundStyle.GRADIENT) {
        Section(R.string.section_color_end) {
            ColorPalette(style.colorEnd, { c -> tick(); onStyle { it.copy(colorEnd = c) } })
        }
    }
    Section(R.string.section_text_color) {
        ColorPalette(style.textColor, { c -> tick(); onStyle { it.copy(textColor = c) } })
    }
    Section(R.string.section_accent) {
        ColorPalette(style.accentColor, { c -> tick(); onStyle { it.copy(accentColor = c) } })
    }
    Section(R.string.section_opacity) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = style.opacity,
                onValueChange = { value -> onStyle { it.copy(opacity = value) } },
                onValueChangeFinished = tick,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
            )
            Text("${(style.opacity * 100).toInt()} %", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(56.dp))
        }
    }
    Section(R.string.section_shape) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetShape.entries.forEach { shape ->
                ChoiceChip(style.shape == shape, stringResource(shapeLabel(shape)), { tick(); onStyle { it.copy(shape = shape) } })
            }
        }
    }
    Section(R.string.section_font) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetFont.entries.forEach { font ->
                ChoiceChip(style.font == font, stringResource(fontLabel(font)), { tick(); onStyle { it.copy(font = font) } }, fontFamily(font))
            }
        }
    }
}

/** Pastille d'un style : fond (dégradé, verre ou uni) sur un « fond d'écran » qui révèle la transparence. */
@Composable
private fun PresetSwatch(preset: StylePreset, selected: Boolean, onClick: () -> Unit) {
    val (start, end) = Colors.background(preset.style)
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
                Modifier.fillMaxSize().padding(4.dp)
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
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIconImage(icon, 40.dp)
            Spacer(Modifier.width(16.dp))
            Text(label ?: stringResource(R.string.app_choose), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1)
            Text(stringResource(R.string.app_change), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FolderAppRow(packageName: String, viewModel: EditorViewModel, onRemove: () -> Unit) {
    val icon by rememberAppIcon(packageName) { viewModel.iconFor(it) }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.heightIn(min = 56.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIconImage(icon, 36.dp)
            Spacer(Modifier.width(12.dp))
            Text(packageName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.folder_remove)) }
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
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
    WidgetSizeOption.LARGE -> R.string.size_large
}
