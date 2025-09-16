package de.kodierer.droidcon25

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import de.kodierer.droidcon25.generated.AllThemes
import de.kodierer.droidcon25.ui.theme.ThemeColorObject
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.pow

/**
 * Unit test to verify that all OnColor combinations meet W3C accessibility contrast standards.
 *
 * WCAG 2.1 Guidelines:
 * - A Level compliance: minimum contrast ratio of 3:1
 * - This provides basic accessibility while allowing more design flexibility
 */
class ColorContrastTest {

    @Test
    fun testAllThemesContrast() {
        // Dynamically test all themes - no need to add new test methods when themes are added
        AllThemes.themes.forEach { themeData ->
            testThemeColors(themeData.name, themeData.colorObject)
        }
    }

    private fun testThemeColors(themeName: String, colors: ThemeColorObject) {
        val colorPairs = listOf(
            "$themeName Primary/OnPrimary" to Pair(colors.primary, colors.onPrimary),
            "$themeName PrimaryLight/OnPrimary" to Pair(colors.primaryLight, colors.onPrimary),
            "$themeName PrimaryDark/OnPrimary" to Pair(colors.primaryDark, colors.onPrimary),
            "$themeName Secondary/OnSecondary" to Pair(colors.secondary, colors.onSecondary),
            "$themeName SecondaryLight/OnSecondary" to Pair(colors.secondaryLight, colors.onSecondary),
            "$themeName SecondaryDark/OnSecondary" to Pair(colors.secondaryDark, colors.onSecondary),
            "$themeName Tertiary/OnTertiary" to Pair(colors.tertiary, colors.onTertiary),
            "$themeName PrimaryContainer/OnPrimaryContainer" to Pair(colors.primaryContainer, colors.onPrimaryContainer),
            "$themeName SecondaryContainer/OnSecondaryContainer" to Pair(colors.secondaryContainer, colors.onSecondaryContainer),
            "$themeName Background/OnBackground" to Pair(colors.background, colors.onBackground),
            "$themeName Surface/OnSurface" to Pair(colors.surface, colors.onSurface)
        )

        val failedPairs = mutableListOf<String>()

        colorPairs.forEach { (name, colorPair) ->
            val contrastRatio = calculateContrastRatio(colorPair.first, colorPair.second)
            if (contrastRatio < 3.0) {
                failedPairs.add("$name: $contrastRatio")
            }
        }

        assertTrue(
            "The following $themeName theme color pairs do not meet WCAG A contrast standards (3:1): ${failedPairs.joinToString(", ")}",
            failedPairs.isEmpty()
        )
    }


    /**
     * Calculates contrast ratio according to WCAG 2.1 guidelines
     * Formula: (L1 + 0.05) / (L2 + 0.05)
     * Where L1 is the relative luminance of the lighter color and L2 is the relative luminance of the darker color
     */
    private fun calculateContrastRatio(color1: Color, color2: Color): Double {
        val luminance1 = getRelativeLuminance(color1)
        val luminance2 = getRelativeLuminance(color2)

        val lighterLuminance = maxOf(luminance1, luminance2)
        val darkerLuminance = minOf(luminance1, luminance2)

        return (lighterLuminance + 0.05) / (darkerLuminance + 0.05)
    }

    /**
     * Calculates relative luminance according to WCAG 2.1 guidelines
     * Formula: 0.2126 * R + 0.7152 * G + 0.0722 * B
     * Where R, G, B are the linearized RGB values
     */
    private fun getRelativeLuminance(color: Color): Double {
        val argb = color.toArgb()
        val r = (argb shr 16 and 0xFF) / 255.0
        val g = (argb shr 8 and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0

        return 0.2126 * linearizeColorComponent(r) +
               0.7152 * linearizeColorComponent(g) +
               0.0722 * linearizeColorComponent(b)
    }

    /**
     * Linearizes a color component according to sRGB specification
     */
    private fun linearizeColorComponent(component: Double): Double {
        return if (component <= 0.03928) {
            component / 12.92
        } else {
            ((component + 0.055) / 1.055).pow(2.4)
        }
    }
}