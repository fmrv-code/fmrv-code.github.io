package fmrv.widgetstudio.ui.gallery

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fmrv.widgetstudio.R
import fmrv.widgetstudio.domain.model.TemplateSection
import fmrv.widgetstudio.domain.model.WidgetTemplate
import fmrv.widgetstudio.ui.components.OneUiCard
import fmrv.widgetstudio.ui.components.WidgetPreview
import fmrv.widgetstudio.ui.components.rememberHaptics

/** Galerie de modèles : chaque carte montre le rendu réel du widget dans son style de départ. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(onTemplate: (WidgetTemplate) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val haptics = rememberHaptics()
    val background = MaterialTheme.colorScheme.background

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.gallery_title)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background, scrolledContainerColor = background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.gallery_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            TemplateSection.entries.forEach { section ->
                val templates = Templates.all.filter { it.section == section }
                if (templates.isEmpty()) return@forEach
                item(key = "header_${section.name}") {
                    Text(
                        stringResource(sectionTitle(section)),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    )
                }
                items(templates.chunked(2), key = { row -> row.first().id }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { template ->
                            TemplateCard(template, Modifier.weight(1f)) {
                                haptics.tick()
                                onTemplate(template)
                            }
                        }
                        if (row.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@StringRes
private fun sectionTitle(section: TemplateSection): Int = when (section) {
    TemplateSection.APPS -> R.string.gallery_section_apps
    TemplateSection.INFO -> R.string.gallery_section_info
    TemplateSection.DEVICE -> R.string.gallery_section_device
}

@Composable
private fun TemplateCard(template: WidgetTemplate, modifier: Modifier, onClick: () -> Unit) {
    OneUiCard(modifier = modifier, onClick = onClick) {
        // Tuile « fond d'écran » pour que la transparence et le verre soient visibles.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .background(Brush.linearGradient(listOf(Color(0xFFE59A74), Color(0xFFB4533A)))),
            contentAlignment = Alignment.Center,
        ) {
            val size = template.size
            val scale = minOf(150f / size.widthDp, 96f / size.heightDp)
            WidgetPreview(config = template.config, icon = null, sizeOption = size, scale = scale)
        }
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(template.title), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                stringResource(template.subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
