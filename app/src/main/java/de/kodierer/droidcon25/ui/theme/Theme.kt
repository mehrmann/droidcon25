package de.kodierer.droidcon25.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppTheme(val displayName: String) {
    ROYAL("Royal Purple"),
    OCEAN("Ocean Fire")
}

data class AppColors(
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val secondary: Color,
    val secondaryLight: Color,
    val secondaryDark: Color,
    val tertiary: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onTertiary: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val onSurface: Color,
    val primaryContainer: Color,
    val secondaryContainer: Color,
    val onPrimaryContainer: Color,
    val onSecondaryContainer: Color
)

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No AppColors provided")
}

private val RoyalColors = AppColors(
    primary = RoyalPrimary,
    primaryLight = RoyalPrimaryLight,
    primaryDark = RoyalPrimaryDark,
    secondary = RoyalSecondary,
    secondaryLight = RoyalSecondaryLight,
    secondaryDark = RoyalSecondaryDark,
    tertiary = RoyalTertiary,
    onPrimary = RoyalOnPrimary,
    onSecondary = RoyalOnSecondary,
    onTertiary = RoyalOnTertiary,
    background = RoyalBackground,
    surface = RoyalSurface,
    onBackground = RoyalOnBackground,
    onSurface = RoyalOnSurface,
    primaryContainer = RoyalPrimaryContainer,
    secondaryContainer = RoyalSecondaryContainer,
    onPrimaryContainer = RoyalOnPrimaryContainer,
    onSecondaryContainer = RoyalOnSecondaryContainer
)

private val OceanColors = AppColors(
    primary = OceanPrimary,
    primaryLight = OceanPrimaryLight,
    primaryDark = OceanPrimaryDark,
    secondary = OceanSecondary,
    secondaryLight = OceanSecondaryLight,
    secondaryDark = OceanSecondaryDark,
    tertiary = OceanTertiary,
    onPrimary = OceanOnPrimary,
    onSecondary = OceanOnSecondary,
    onTertiary = OceanOnTertiary,
    background = OceanBackground,
    surface = OceanSurface,
    onBackground = OceanOnBackground,
    onSurface = OceanOnSurface,
    primaryContainer = OceanPrimaryContainer,
    secondaryContainer = OceanSecondaryContainer,
    onPrimaryContainer = OceanOnPrimaryContainer,
    onSecondaryContainer = OceanOnSecondaryContainer
)


@Composable
fun Droidcon25DemoApplicationTheme(
    theme: AppTheme = AppTheme.ROYAL,
    content: @Composable () -> Unit
) {
    val appColors = when (theme) {
        AppTheme.ROYAL -> RoyalColors
        AppTheme.OCEAN -> OceanColors
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        content()
    }
}
