package co.com.coexito.bupas.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BupasPrimary,
    onPrimary = BupasOnPrimary,
    primaryContainer = BupasPrimaryContainer,
    onPrimaryContainer = BupasOnPrimaryContainer,
    secondary = BupasSecondary,
    onSecondary = BupasOnSecondary,
    secondaryContainer = BupasSecondaryContainer,
    onSecondaryContainer = BupasOnSecondaryContainer,
    tertiary = BupasTertiary,
    onTertiary = BupasOnTertiary,
    tertiaryContainer = BupasTertiaryContainer,
    onTertiaryContainer = BupasOnTertiaryContainer,
    background = BupasBackgroundLight,
    onBackground = BupasOnBackgroundLight,
    surface = BupasSurfaceLight,
    onSurface = BupasOnSurfaceLight,
    surfaceVariant = BupasSurfaceVariantLight,
    onSurfaceVariant = BupasOnSurfaceVariantLight,
    outline = BupasOutlineLight,
    error = BupasError,
    onError = BupasOnError,
    errorContainer = BupasErrorContainer,
    onErrorContainer = BupasOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = BupasPrimaryLight,
    onPrimary = BupasOnPrimary,
    primaryContainer = BupasPrimaryDark,
    onPrimaryContainer = BupasOnPrimary,
    secondary = BupasSecondary,
    onSecondary = BupasOnSecondary,
    secondaryContainer = BupasOnSecondaryContainer,
    onSecondaryContainer = BupasSecondaryContainer,
    tertiary = BupasTertiaryContainer,
    onTertiary = BupasOnTertiaryContainer,
    background = BupasBackgroundDark,
    onBackground = BupasOnBackgroundDark,
    surface = BupasSurfaceDark,
    onSurface = BupasOnSurfaceDark,
    surfaceVariant = BupasSurfaceVariantDark,
    onSurfaceVariant = BupasOnSurfaceVariantDark,
    outline = BupasOutlineDark,
    error = BupasError,
    onError = BupasOnError,
    errorContainer = BupasOnErrorContainer,
    onErrorContainer = BupasErrorContainer
)

private fun darkBupasExtendedColors(): BupasExtendedColors = BupasExtendedColors(
    readOnlySurface = Color(0xFF1E293B),
    primaryDisabledContainer = Color(0xFF1E3A5F),
    borderDisabled = Color(0xFF334155)
)

object BupasTheme {
    val extendedColors: BupasExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBupasExtendedColors.current

    val spacing: BupasSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalBupasSpacing.current
}

@Composable
fun BupasTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    BupasTheme(darkTheme = darkTheme, content = content)
}

@Composable
fun BupasTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) darkBupasExtendedColors() else BupasExtendedColors()
    val spacing = BupasSpacing()

    CompositionLocalProvider(
        LocalBupasExtendedColors provides extendedColors,
        LocalBupasSpacing provides spacing
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BupasTypography,
            shapes = BupasShapes,
            content = content
        )
    }
}
