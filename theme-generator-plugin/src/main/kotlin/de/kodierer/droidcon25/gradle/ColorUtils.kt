package de.kodierer.droidcon25.gradle

import org.gradle.api.GradleException
import kotlin.math.max
import kotlin.math.min

/**
 * Utilities for color processing, validation, and transformations.
 */
object ColorUtils {

    /**
     * Applies a color modifier (lighten, darken, mix, alpha) to a base color.
     */
    fun applyColorModifier(baseColor: String, modifier: ColorModifier): String {
        return when (modifier.type.lowercase()) {
            "lighten" -> lightenColor(parseHexColor(baseColor), modifier.value)
            "darken" -> darkenColor(parseHexColor(baseColor), modifier.value)
            "alpha" -> applyAlpha(parseHexColor(baseColor), modifier.value)
            "mix" -> {
                val mixColor = modifier.color ?: throw GradleException("Mix modifier requires a 'color' property")
                mixColors(parseHexColor(baseColor), mixColor, modifier.value)
            }
            else -> throw GradleException("Unsupported color modifier type: ${modifier.type}")
        }
    }

    /**
     * Parses a hex color string into RGB components.
     */
    fun parseHexColor(hex: String): Triple<Int, Int, Int> {
        val cleanHex = hex.removePrefix("#")
        return when (cleanHex.length) {
            3 -> {
                val r = cleanHex[0].digitToInt(16) * 17
                val g = cleanHex[1].digitToInt(16) * 17
                val b = cleanHex[2].digitToInt(16) * 17
                Triple(r, g, b)
            }
            6 -> {
                val r = cleanHex.substring(0, 2).toInt(16)
                val g = cleanHex.substring(2, 4).toInt(16)
                val b = cleanHex.substring(4, 6).toInt(16)
                Triple(r, g, b)
            }
            else -> throw GradleException("Invalid hex color format: $hex")
        }
    }

    /**
     * Lightens a color by the specified amount (0.0 to 1.0).
     */
    fun lightenColor(rgb: Triple<Int, Int, Int>, amount: Double): String {
        val (r, g, b) = rgb
        val newR = min(255, (r + (255 - r) * amount).toInt())
        val newG = min(255, (g + (255 - g) * amount).toInt())
        val newB = min(255, (b + (255 - b) * amount).toInt())
        return String.format("#%02X%02X%02X", newR, newG, newB)
    }

    /**
     * Darkens a color by the specified amount (0.0 to 1.0).
     */
    fun darkenColor(rgb: Triple<Int, Int, Int>, amount: Double): String {
        val (r, g, b) = rgb
        val newR = max(0, (r * (1 - amount)).toInt())
        val newG = max(0, (g * (1 - amount)).toInt())
        val newB = max(0, (b * (1 - amount)).toInt())
        return String.format("#%02X%02X%02X", newR, newG, newB)
    }

    /**
     * Applies alpha transparency to a color.
     */
    fun applyAlpha(rgb: Triple<Int, Int, Int>, alpha: Double): String {
        val (r, g, b) = rgb
        val alphaValue = (alpha * 255).toInt()
        return String.format("#%02X%02X%02X%02X", alphaValue, r, g, b)
    }

    /**
     * Mixes two colors with the specified ratio.
     */
    fun mixColors(rgb1: Triple<Int, Int, Int>, color2: String, ratio: Double): String {
        val rgb2 = parseHexColor(color2)
        val (r1, g1, b1) = rgb1
        val (r2, g2, b2) = rgb2

        val newR = (r1 * (1 - ratio) + r2 * ratio).toInt()
        val newG = (g1 * (1 - ratio) + g2 * ratio).toInt()
        val newB = (b1 * (1 - ratio) + b2 * ratio).toInt()

        return String.format("#%02X%02X%02X", newR, newG, newB)
    }

    /**
     * Normalizes a hex color to uppercase format with # prefix.
     */
    fun normalizeHexColor(color: String): String {
        val trimmed = color.trim()
        return if (trimmed.startsWith("#")) {
            trimmed.uppercase()
        } else {
            "#${trimmed.uppercase()}"
        }
    }

    /**
     * Validates if a string is a valid hex color.
     */
    fun isValidHexColor(color: String): Boolean {
        val cleanColor = color.removePrefix("#")
        return cleanColor.matches(Regex("^[0-9A-Fa-f]{3}$|^[0-9A-Fa-f]{6}$|^[0-9A-Fa-f]{8}$"))
    }

    /**
     * Checks if a value is a token reference (starts with '{' and ends with '}').
     */
    fun isValidTokenReference(value: String): Boolean {
        return value.startsWith("{") && value.endsWith("}")
    }

    /**
     * Sanitizes a property name to be valid Kotlin identifier.
     */
    fun sanitizePropertyName(colorName: String): String {
        return colorName.replace(Regex("[^a-zA-Z0-9_]"), "_")
            .let { if (it.first().isDigit()) "_$it" else it }
    }
}