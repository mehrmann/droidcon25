package de.kodierer.droidcon25.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Interface that defines the color contract for all themes.
 * Theme color objects generated from JSON files implement this interface.
 */
interface ThemeColorObject {
    val primary: Color
    val primaryLight: Color
    val primaryDark: Color
    val secondary: Color
    val secondaryLight: Color
    val secondaryDark: Color
    val tertiary: Color
    val onPrimary: Color
    val onSecondary: Color
    val onTertiary: Color
    val background: Color
    val surface: Color
    val onBackground: Color
    val onSurface: Color
    val primaryContainer: Color
    val secondaryContainer: Color
    val onPrimaryContainer: Color
    val onSecondaryContainer: Color
}