package fmrv.widgetstudio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fmrv.widgetstudio.data.settings.AppSettings
import fmrv.widgetstudio.data.settings.ThemeMode
import fmrv.widgetstudio.ui.components.LocalHapticsEnabled

private val OneUiBlue = Color(0xFF3E91FF)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0A74F0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E8FF),
    onPrimaryContainer = Color(0xFF00346B),
    secondary = Color(0xFF5B6B80),
    secondaryContainer = Color(0xFFE3EAF3),
    onSecondaryContainer = Color(0xFF1B2735),
    background = Color(0xFFF6F6F6),
    onBackground = Color(0xFF151515),
    surface = Color(0xFFF6F6F6),
    onSurface = Color(0xFF151515),
    onSurfaceVariant = Color(0xFF5E5E62),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9F9F9),
    surfaceContainer = Color(0xFFEFEFF1),
    surfaceContainerHigh = Color(0xFFE8E8EA),
    outlineVariant = Color(0xFFD9D9DC),
)

private val DarkColors = darkColorScheme(
    primary = OneUiBlue,
    onPrimary = Color(0xFF00305F),
    primaryContainer = Color(0xFF123A6B),
    onPrimaryContainer = Color(0xFFD8E8FF),
    secondary = Color(0xFFB4C3D8),
    secondaryContainer = Color(0xFF2B3748),
    onSecondaryContainer = Color(0xFFDCE6F5),
    background = Color(0xFF0F0F10),
    onBackground = Color(0xFFEDEDEF),
    surface = Color(0xFF0F0F10),
    onSurface = Color(0xFFEDEDEF),
    onSurfaceVariant = Color(0xFFA9A9AE),
    surfaceContainerLowest = Color(0xFF1C1C1E),
    surfaceContainerLow = Color(0xFF1A1A1C),
    surfaceContainer = Color(0xFF242426),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    outlineVariant = Color(0xFF3A3A3D),
)

/** One UI : grands rayons, jusqu'à 36 dp pour les éléments les plus proéminents. */
private val OneUiShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
)

/** Police système (One UI Sans sur Samsung), grands titres lisibles. */
private val OneUiTypography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
        ),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 24.sp),
    )
}

fun resolveDarkTheme(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK, ThemeMode.AMOLED -> true
}

@Composable
fun WidgetStudioTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = resolveDarkTheme(settings.themeMode, isSystemInDarkTheme())
    val context = LocalContext.current

    var colors = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    if (dark && settings.themeMode == ThemeMode.AMOLED) {
        colors = colors.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color(0xFF121212),
            surfaceContainerLow = Color(0xFF0E0E0E),
            surfaceContainer = Color(0xFF1A1A1A),
        )
    }

    CompositionLocalProvider(LocalHapticsEnabled provides settings.haptics) {
        MaterialTheme(colorScheme = colors, typography = OneUiTypography, shapes = OneUiShapes, content = content)
    }
}
