package de.kodierer.droidcon25.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import de.kodierer.droidcon25.generated.AllThemes
import de.kodierer.droidcon25.generated.AppTheme

/**
 * CompositionLocal for accessing the current app colors throughout the Compose tree.
 *
 * This provides a stable, documented way to access theme colors in any composable
 * without having to pass them down through parameters.
 */
val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No AppColors provided. Make sure you wrap your content with Droidcon25DemoApplicationTheme.")
}

/**
 * CompositionLocal for accessing the current theme data throughout the Compose tree.
 *
 * This provides access to the full theme metadata and utility methods.
 */
val LocalThemeData = staticCompositionLocalOf<ThemeData> {
    error("No ThemeData provided. Make sure you wrap your content with Droidcon25DemoApplicationTheme.")
}

/**
 * Main theme composable that provides app colors and theme data to the composition tree.
 *
 * This composable sets up the theme system and makes colors available through
 * CompositionLocals. It automatically uses the generated theme data but provides
 * a stable, extensible API.
 *
 * @param theme The theme to apply. Defaults to the first available theme.
 * @param content The content to theme.
 */
@Composable
fun Droidcon25DemoApplicationTheme(
    theme: AppTheme = AllThemes.allAppThemes.firstOrNull() ?: error("No themes available"),
    content: @Composable () -> Unit
) {
    val themeData = AllThemes.getThemeData(theme)

    CompositionLocalProvider(
        LocalAppColors provides themeData.appColors,
        LocalThemeData provides themeData
    ) {
        content()
    }
}

/**
 * Extension function to get the current app colors from any composable.
 * Convenience function for accessing LocalAppColors.current.
 */
@Composable
fun currentAppColors(): AppColors = LocalAppColors.current

/**
 * Extension function to get the current theme data from any composable.
 * Convenience function for accessing LocalThemeData.current.
 */
@Composable
fun currentThemeData(): ThemeData = LocalThemeData.current