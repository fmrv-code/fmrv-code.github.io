package fmrv.widgetstudio.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Carte flottante One UI : fond clair/sombre dédié, très grand rayon, sans ombre. */
@Composable
fun OneUiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    val shape = MaterialTheme.shapes.large
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, content = content)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, content = content)
    }
}

data class NavItem(val route: String, @StringRes val label: Int, val icon: ImageVector)

/** Barre de navigation flottante dans la moitié basse de l'écran, accessible au pouce. */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 6.dp,
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = {
                        if (currentRoute != item.route) {
                            haptics.tick()
                            onSelect(item.route)
                        }
                    },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(stringResource(item.label)) },
                )
            }
        }
    }
}

/** Charge l'icône d'une application en arrière-plan ; `null` tant qu'elle n'est pas prête. */
@Composable
fun rememberAppIcon(packageName: String?, load: suspend (String) -> ImageBitmap?): State<ImageBitmap?> =
    produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = if (packageName == null) null else load(packageName)
    }

@Composable
fun AppIconImage(icon: ImageBitmap?, size: Dp, modifier: Modifier = Modifier) {
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = modifier.size(size))
    } else {
        Surface(
            modifier = modifier.size(size),
            shape = RoundedCornerShape(size * 0.24f),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {}
    }
}
