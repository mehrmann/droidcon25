package de.kodierer.droidcon25.gradle

import org.gradle.api.GradleException

/**
 * Validates parsed themes for consistency and correctness.
 */
object ThemeValidator {

    /**
     * Validates a list of parsed themes.
     */
    fun validateParsedThemes(themes: List<ParsedTheme>) {
        if (themes.isEmpty()) {
            throw GradleException("No themes found in input directory")
        }

        validateUniqueThemeNames(themes)
        validateUniqueEnumNames(themes)
        validateConsistentColorKeys(themes)
        validateColorValues(themes)
    }

    /**
     * Ensures all theme names are unique.
     */
    private fun validateUniqueThemeNames(themes: List<ParsedTheme>) {
        val themeNames = themes.map { it.name }
        val duplicateNames = themeNames.groupBy { it }
            .filterValues { it.size > 1 }
            .keys

        if (duplicateNames.isNotEmpty()) {
            throw GradleException("Duplicate theme names found: ${duplicateNames.joinToString(", ")}")
        }
    }

    /**
     * Ensures all enum names are unique.
     */
    private fun validateUniqueEnumNames(themes: List<ParsedTheme>) {
        val enumNames = themes.map { it.enumName }
        val duplicateEnums = enumNames.groupBy { it }
            .filterValues { it.size > 1 }
            .keys

        if (duplicateEnums.isNotEmpty()) {
            throw GradleException("Duplicate enum names found: ${duplicateEnums.joinToString(", ")}")
        }
    }

    /**
     * Ensures all themes have the same color keys.
     */
    private fun validateConsistentColorKeys(themes: List<ParsedTheme>) {
        if (themes.size <= 1) return

        val firstThemeKeys = themes.first().colors.keys.sorted()

        themes.drop(1).forEach { theme ->
            val currentKeys = theme.colors.keys.sorted()
            if (currentKeys != firstThemeKeys) {
                val missing = firstThemeKeys - currentKeys.toSet()
                val extra = currentKeys - firstThemeKeys.toSet()

                val errorMessage = buildString {
                    append("Theme '${theme.name}' has inconsistent color keys compared to '${themes.first().name}'")
                    if (missing.isNotEmpty()) {
                        append("\n  Missing colors: ${missing.joinToString(", ")}")
                    }
                    if (extra.isNotEmpty()) {
                        append("\n  Extra colors: ${extra.joinToString(", ")}")
                    }
                }

                throw GradleException(errorMessage)
            }
        }
    }

    /**
     * Validates that all color values are valid hex colors.
     */
    private fun validateColorValues(themes: List<ParsedTheme>) {
        themes.forEach { theme ->
            theme.colors.forEach { (colorName, colorValue) ->
                if (!ColorUtils.isValidHexColor(colorValue)) {
                    throw GradleException(
                        "Invalid color value '$colorValue' for color '$colorName' in theme '${theme.name}'. " +
                        "Expected hex color format like #FF0000 or #F00"
                    )
                }
            }
        }
    }
}