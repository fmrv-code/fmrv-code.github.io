package fmrv.widgetstudio.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fmrv.widgetstudio.R
import fmrv.widgetstudio.ui.apps.AppsScreen
import fmrv.widgetstudio.ui.components.FloatingNavBar
import fmrv.widgetstudio.ui.components.NavItem
import fmrv.widgetstudio.ui.editor.EditorScreen
import fmrv.widgetstudio.ui.gallery.GalleryScreen
import fmrv.widgetstudio.ui.home.HomeScreen
import fmrv.widgetstudio.ui.settings.SettingsScreen
import fmrv.widgetstudio.viewmodel.EditorViewModel

object Routes {
    const val GALLERY = "gallery"
    const val HOME = "home"
    const val APPS = "apps"
    const val SETTINGS = "settings"
    const val EDITOR = "editor?id={id}&pkg={pkg}&kind={kind}"

    private const val ARG_ID = "id"
    private const val ARG_PKG = "pkg"
    private const val ARG_KIND = "kind"

    fun editor(widgetId: Int = 0, packageName: String? = null, kindId: String? = null): String = buildString {
        append("editor?$ARG_ID=$widgetId")
        if (packageName != null) append("&$ARG_PKG=${Uri.encode(packageName)}")
        if (kindId != null) append("&$ARG_KIND=${Uri.encode(kindId)}")
    }
}

private val topLevelItems = listOf(
    NavItem(Routes.GALLERY, R.string.nav_gallery, Icons.Outlined.Dashboard),
    NavItem(Routes.HOME, R.string.nav_widgets, Icons.Outlined.Widgets),
    NavItem(Routes.APPS, R.string.nav_apps, Icons.Outlined.Apps),
    NavItem(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings),
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBar = topLevelItems.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically(tween(250)) { it } + fadeIn(),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(),
            ) {
                FloatingNavBar(
                    items = topLevelItems,
                    currentRoute = currentRoute,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Routes.GALLERY,
            modifier = Modifier.padding(bottom = inner.calculateBottomPadding()),
            enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(300)) { it / 14 } },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) + slideOutVertically(tween(260)) { it / 14 } },
        ) {
            composable(Routes.GALLERY) {
                GalleryScreen(onKind = { kind -> navController.navigate(Routes.editor(kindId = kind.id)) })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onCreateWidget = {
                        navController.navigate(Routes.GALLERY) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onEditWidget = { id -> navController.navigate(Routes.editor(widgetId = id)) },
                )
            }
            composable(Routes.APPS) {
                AppsScreen(onAppSelected = { pkg -> navController.navigate(Routes.editor(packageName = pkg, kindId = "app.icon")) })
            }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(
                route = Routes.EDITOR,
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                    navArgument("pkg") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("kind") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val viewModel: EditorViewModel = hiltViewModel()
                EditorScreen(
                    viewModel = viewModel,
                    widgetId = entry.arguments?.getInt("id") ?: 0,
                    presetPackage = entry.arguments?.getString("pkg"),
                    kindId = entry.arguments?.getString("kind"),
                    onClose = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
        }
    }
}
