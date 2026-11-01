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
import fmrv.widgetstudio.domain.model.Category
import fmrv.widgetstudio.domain.model.Params
import fmrv.widgetstudio.domain.model.WidgetInstance
import fmrv.widgetstudio.kinds.Kinds
import fmrv.widgetstudio.kinds.WidgetKind
import fmrv.widgetstudio.ui.components.OneUiCard
import fmrv.widgetstudio.ui.components.rememberHaptics
import fmrv.widgetstudio.ui.preview.WidgetPreview

/** Galerie : toutes les familles de widgets du catalogue, chaque carte montrant le vrai rendu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(onKind: (WidgetKind<*>) -> Unit) {
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
            Category.entries.forEach { category ->
                val kinds = Kinds.inCategory(category)
                if (kinds.isEmpty()) return@forEach
                item(key = "header_${category.name}") {
                    Text(
                        stringResource(categoryTitle(category)),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    )
                }
                items(kinds.chunked(2), key = { row -> row.first().id }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { kind ->
                            KindCard(kind, Modifier.weight(1f)) {
                                haptics.tick()
                                onKind(kind)
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
fun categoryTitle(category: Category): Int = when (category) {
    Category.CLOCKS -> R.string.cat_clocks
    Category.CALENDAR -> R.string.cat_calendar
    Category.APPS -> R.string.cat_apps
    Category.INFO -> R.string.cat_info
    Category.DEVICE -> R.string.cat_device
}

/** Instance de démonstration : style et réglages par défaut du type, sans identifiant de widget. */
fun sampleInstance(kind: WidgetKind<*>): WidgetInstance = WidgetInstance(
    appWidgetId = WidgetInstance.NO_ID,
    kindId = kind.id,
    style = kind.defaultStyle,
    params = Params(kind.defaultParams),
)

@Composable
private fun KindCard(kind: WidgetKind<*>, modifier: Modifier, onClick: () -> Unit) {
    OneUiCard(modifier = modifier, onClick = onClick) {
        // Tuile « fond d'écran » pour que la transparence et le verre soient visibles.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .background(Brush.linearGradient(listOf(Color(0xFFE59A74), Color(0xFFB4533A)))),
            contentAlignment = Alignment.Center,
        ) {
            val size = kind.defaultSize
            val scale = minOf(150f / size.widthDp, 100f / size.heightDp)
            WidgetPreview(sampleInstance(kind), size, scale = scale)
        }
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(kind.title), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                stringResource(kind.description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
