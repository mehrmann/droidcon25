package de.kodierer.droidcon25.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Data class representing the complete color palette for an app theme.
 *
 * This follows Material Design 3 color system guidelines and provides
 * colors for all semantic roles in the application.
 */
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
) {
    /**
     * Convenience constructor that creates AppColors from a ThemeColorObject.
     * This eliminates the need for individual theme color instances.
     */
    constructor(themeColors: ThemeColorObject) : this(
        primary = themeColors.primary,
        primaryLight = themeColors.primaryLight,
        primaryDark = themeColors.primaryDark,
        secondary = themeColors.secondary,
        secondaryLight = themeColors.secondaryLight,
        secondaryDark = themeColors.secondaryDark,
        tertiary = themeColors.tertiary,
        onPrimary = themeColors.onPrimary,
        onSecondary = themeColors.onSecondary,
        onTertiary = themeColors.onTertiary,
        background = themeColors.background,
        surface = themeColors.surface,
        onBackground = themeColors.onBackground,
        onSurface = themeColors.onSurface,
        primaryContainer = themeColors.primaryContainer,
        secondaryContainer = themeColors.secondaryContainer,
        onPrimaryContainer = themeColors.onPrimaryContainer,
        onSecondaryContainer = themeColors.onSecondaryContainer
    )
    /**
     * Determines if this is a dark theme based on background luminance.
     */
    fun isDarkTheme(): Boolean = background.luminance() < 0.5f

    /**
     * Gets a text color that provides good contrast against the given background.
     */
    fun getAccessibleTextColor(backgroundColor: Color): Color {
        return if (backgroundColor.luminance() > 0.5f) {
            onBackground // Use dark text on light backgrounds
        } else {
            background // Use light text on dark backgrounds
        }
    }

    /**
     * Gets a color by its semantic role name.
     * Useful for dynamic color selection based on string identifiers.
     */
    fun getColorByRole(role: String): Color? = when (role.lowercase()) {
        "primary" -> primary
        "primarylight" -> primaryLight
        "primarydark" -> primaryDark
        "secondary" -> secondary
        "secondarylight" -> secondaryLight
        "secondarydark" -> secondaryDark
        "tertiary" -> tertiary
        "onprimary" -> onPrimary
        "onsecondary" -> onSecondary
        "ontertiary" -> onTertiary
        "background" -> background
        "surface" -> surface
        "onbackground" -> onBackground
        "onsurface" -> onSurface
        "primarycontainer" -> primaryContainer
        "secondarycontainer" -> secondaryContainer
        "onprimarycontainer" -> onPrimaryContainer
        "onsecondarycontainer" -> onSecondaryContainer
        else -> null
    }
}