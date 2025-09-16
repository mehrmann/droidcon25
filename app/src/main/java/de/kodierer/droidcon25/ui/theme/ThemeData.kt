package de.kodierer.droidcon25.ui.theme

import androidx.compose.ui.graphics.Color
import de.kodierer.droidcon25.generated.AppTheme

/**
 * Data class that represents a complete theme with its metadata and color data.
 *
 * This provides a unified interface to access theme information and colors,
 * bridging the gap between the generated theme enum and color objects.
 */
data class ThemeData(
    val theme: AppTheme,
    val name: String,
    val colorObject: ThemeColorObject,
    val appColors: AppColors
) {
    /**
     * Determines if this is a light theme based on background luminance.
     */
    fun isLightTheme(): Boolean = !appColors.isDarkTheme()

    /**
     * Determines if this is a dark theme based on background luminance.
     */
    fun isDarkTheme(): Boolean = appColors.isDarkTheme()

    /**
     * Gets a color by its semantic role name from the theme's color object.
     * This provides direct access to the raw theme colors without AppColors wrapper.
     */
    fun getColorByRole(role: String): Color? = when (role.lowercase()) {
        "primary" -> colorObject.primary
        "primarylight" -> colorObject.primaryLight
        "primarydark" -> colorObject.primaryDark
        "secondary" -> colorObject.secondary
        "secondarylight" -> colorObject.secondaryLight
        "secondarydark" -> colorObject.secondaryDark
        "tertiary" -> colorObject.tertiary
        "onprimary" -> colorObject.onPrimary
        "onsecondary" -> colorObject.onSecondary
        "ontertiary" -> colorObject.onTertiary
        "background" -> colorObject.background
        "surface" -> colorObject.surface
        "onbackground" -> colorObject.onBackground
        "onsurface" -> colorObject.onSurface
        "primarycontainer" -> colorObject.primaryContainer
        "secondarycontainer" -> colorObject.secondaryContainer
        "onprimarycontainer" -> colorObject.onPrimaryContainer
        "onsecondarycontainer" -> colorObject.onSecondaryContainer
        else -> null
    }

    /**
     * Gets the primary colors as a list (primary, primaryLight, primaryDark).
     * Useful for creating color variations or gradients.
     */
    fun getPrimaryColorVariations(): List<Color> = listOf(
        colorObject.primary,
        colorObject.primaryLight,
        colorObject.primaryDark
    )

    /**
     * Gets the secondary colors as a list (secondary, secondaryLight, secondaryDark).
     * Useful for creating color variations or gradients.
     */
    fun getSecondaryColorVariations(): List<Color> = listOf(
        colorObject.secondary,
        colorObject.secondaryLight,
        colorObject.secondaryDark
    )

    /**
     * Gets all container colors as a map for easy access.
     */
    fun getContainerColors(): Map<String, Color> = mapOf(
        "primary" to colorObject.primaryContainer,
        "secondary" to colorObject.secondaryContainer
    )

    /**
     * Returns a summary string with theme name and whether it's light/dark.
     */
    override fun toString(): String = "$name (${if (isDarkTheme()) "Dark" else "Light"} Theme)"
}