package de.kodierer.droidcon25.gradle

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.assertDoesNotThrow
import org.gradle.api.GradleException
import kotlin.test.*

class ThemeGeneratorPluginTest {

    private lateinit var task: TestableGenerateThemesTask
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setup() {
        task = TestableGenerateThemesTask()
    }

    @Nested
    @DisplayName("Token Studio Format Parsing")
    inner class TokenStudioFormatTest {

        @Test
        @DisplayName("Should parse basic Token Studio format")
        fun testBasicTokenStudioParsing() {
            val tokenContent = """
                {
                  "color": {
                    "primary": {
                      "value": "#4682B4",
                      "type": "color",
                      "description": "Primary brand color"
                    },
                    "secondary": {
                      "value": "#708090",
                      "type": "color",
                      "description": "Secondary color"
                    }
                  },
                  "${'$'}themes": {
                    "test-theme": {
                      "name": "Test Theme",
                      "enumName": "TEST_THEME"
                    }
                  }
                }
            """.trimIndent()

            val tokenSet = json.decodeFromString<TokenSet>(tokenContent)
            val themes = task.parseTokenStudioFormat("test-theme", tokenSet)

            assertEquals(1, themes.size)
            val theme = themes.first()
            assertEquals("Test Theme", theme.name)
            assertEquals("TEST_THEME", theme.enumName)
            assertEquals("#4682B4", theme.colors["primary"])
            assertEquals("#708090", theme.colors["secondary"])
        }

        @Test
        @DisplayName("Should parse W3C DTCG format")
        fun testW3CDTCGFormat() {
            val tokenContent = """
                {
                  "color": {
                    "primary": {
                      "value": "",
                      "${'$'}value": "#FF5722",
                      "${'$'}type": "color",
                      "${'$'}description": "Primary color in DTCG format"
                    }
                  }
                }
            """.trimIndent()

            val tokenSet = json.decodeFromString<TokenSet>(tokenContent)
            val themes = task.parseTokenStudioFormat("dtcg-test", tokenSet)

            assertEquals(1, themes.size)
            assertEquals("#FF5722", themes.first().colors["primary"])
        }
    }

    @Nested
    @DisplayName("Token Reference Resolution")
    inner class TokenReferenceTest {

        @Test
        @DisplayName("Should resolve simple token references")
        fun testSimpleTokenReferences() {
            val colorTokens = mapOf(
                "primary" to ColorToken("#4682B4", "color"),
                "surface" to ColorToken("{color.primary}", "color")
            )

            val resolved = task.resolveTokenReferences(colorTokens)

            assertEquals("#4682B4", resolved["primary"])
            assertEquals("#4682B4", resolved["surface"])
        }

        @Test
        @DisplayName("Should resolve chained token references")
        fun testChainedTokenReferences() {
            val colorTokens = mapOf(
                "primary" to ColorToken("#4682B4", "color"),
                "surface" to ColorToken("{color.primary}", "color"),
                "background" to ColorToken("{color.surface}", "color")
            )

            val resolved = task.resolveTokenReferences(colorTokens)

            assertEquals("#4682B4", resolved["primary"])
            assertEquals("#4682B4", resolved["surface"])
            assertEquals("#4682B4", resolved["background"])
        }

        @Test
        @DisplayName("Should handle circular references gracefully")
        fun testCircularReferences() {
            val colorTokens = mapOf(
                "primary" to ColorToken("{color.secondary}", "color"),
                "secondary" to ColorToken("{color.primary}", "color")
            )

            val resolved = task.resolveTokenReferences(colorTokens)

            // Should not crash and return the reference strings as fallback
            assertEquals("{color.secondary}", resolved["primary"])
            assertEquals("{color.primary}", resolved["secondary"])
        }
    }

    @Nested
    @DisplayName("Color Modifier Functionality")
    inner class ColorModifierTest {

        @Test
        @DisplayName("Should apply lighten modifier correctly")
        fun testLightenModifier() {
            val baseColor = "#808080" // Medium gray
            val modifier = ColorModifier("lighten", 0.5)

            val result = task.applyColorModifier(baseColor, modifier)

            // Should be lighter than original
            assertNotEquals(baseColor, result)
            assertTrue(result.startsWith("#"))
            assertEquals(7, result.length)
        }

        @Test
        @DisplayName("Should apply darken modifier correctly")
        fun testDarkenModifier() {
            val baseColor = "#808080" // Medium gray
            val modifier = ColorModifier("darken", 0.3)

            val result = task.applyColorModifier(baseColor, modifier)

            assertNotEquals(baseColor, result)
            assertTrue(result.startsWith("#"))
            assertEquals(7, result.length)
        }

        @Test
        @DisplayName("Should apply alpha modifier correctly")
        fun testAlphaModifier() {
            val baseColor = "#FF5722"
            val modifier = ColorModifier("alpha", 0.5)

            val result = task.applyColorModifier(baseColor, modifier)

            // Alpha modifier should produce ARGB format (8 characters)
            assertTrue(result.startsWith("#"))
            assertEquals(9, result.length) // #AARRGGBB

            // Alpha value should be approximately 0.5 * 255 = 127 = 0x7F
            val alphaHex = result.substring(1, 3)
            val alphaValue = alphaHex.toInt(16)
            assertTrue(alphaValue in 120..135) // Allow some rounding tolerance
        }

        @Test
        @DisplayName("Should apply mix modifier correctly")
        fun testMixModifier() {
            val baseColor = "#FF0000" // Pure red
            val modifier = ColorModifier("mix", 0.5, color = "#0000FF") // Mix with pure blue

            val result = task.applyColorModifier(baseColor, modifier)

            assertNotEquals(baseColor, result)
            assertTrue(result.startsWith("#"))
            assertEquals(7, result.length)
            // Result should be some shade of purple (mix of red and blue)
        }

        @Test
        @DisplayName("Should handle unknown modifier types gracefully")
        fun testUnknownModifierType() {
            val baseColor = "#FF5722"
            val modifier = ColorModifier("unknown", 0.5)

            val result = task.applyColorModifier(baseColor, modifier)

            // Should return original color unchanged
            assertEquals(baseColor, result)
        }

        @Test
        @DisplayName("Should resolve token references with modifiers")
        fun testTokenReferencesWithModifiers() {
            val colorTokens = mapOf(
                "primary" to ColorToken("#4682B4", "color"),
                "primaryLight" to ColorToken(
                    value = "{color.primary}",
                    type = "color",
                    extensions = TokenExtensions(
                        StudioTokens(ColorModifier("lighten", 0.2))
                    )
                )
            )

            val resolved = task.resolveTokenReferences(colorTokens)

            assertEquals("#4682B4", resolved["primary"])
            assertNotEquals("#4682B4", resolved["primaryLight"])
            assertTrue(resolved["primaryLight"]!!.startsWith("#"))
        }
    }

    @Nested
    @DisplayName("Color Validation")
    inner class ColorValidationTest {

        @Test
        @DisplayName("Should validate 6-character hex colors")
        fun testValidate6CharHex() {
            assertTrue(task.isValidHexColor("#FF5722"))
            assertTrue(task.isValidHexColor("#000000"))
            assertTrue(task.isValidHexColor("#FFFFFF"))
        }

        @Test
        @DisplayName("Should validate 3-character hex colors")
        fun testValidate3CharHex() {
            assertTrue(task.isValidHexColor("#F00"))
            assertTrue(task.isValidHexColor("#0F0"))
            assertTrue(task.isValidHexColor("#00F"))
        }

        @Test
        @DisplayName("Should validate 8-character ARGB hex colors")
        fun testValidate8CharHex() {
            assertTrue(task.isValidHexColor("#80FF5722"))
            assertTrue(task.isValidHexColor("#FF000000"))
            assertTrue(task.isValidHexColor("#00FFFFFF"))
        }

        @Test
        @DisplayName("Should reject invalid hex colors")
        fun testRejectInvalidHex() {
            assertFalse(task.isValidHexColor("FF5722")) // Missing #
            assertFalse(task.isValidHexColor("#GG5722")) // Invalid characters
            assertFalse(task.isValidHexColor("#FF572")) // Wrong length
            assertFalse(task.isValidHexColor("#FF57222")) // Wrong length
        }

        @Test
        @DisplayName("Should validate token references")
        fun testValidateTokenReferences() {
            assertTrue(task.isValidTokenReference("{color.primary}"))
            assertTrue(task.isValidTokenReference("{spacing.base}"))
            assertFalse(task.isValidTokenReference("color.primary")) // Missing braces
            assertFalse(task.isValidTokenReference("{color}")) // Missing dot
        }
    }

    @Nested
    @DisplayName("Legacy Format Compatibility")
    inner class LegacyFormatTest {

        @Test
        @DisplayName("Should parse legacy theme format")
        fun testLegacyFormatParsing() {
            val legacyContent = """
                {
                  "name": "Legacy Theme",
                  "enumName": "LEGACY_THEME",
                  "colors": {
                    "primary": "#4682B4",
                    "secondary": "#708090"
                  }
                }
            """.trimIndent()

            val legacyTheme = json.decodeFromString<LegacyTheme>(legacyContent)

            assertEquals("Legacy Theme", legacyTheme.name)
            assertEquals("LEGACY_THEME", legacyTheme.enumName)
            assertEquals("#4682B4", legacyTheme.colors["primary"])
            assertEquals("#708090", legacyTheme.colors["secondary"])
        }
    }

    @Nested
    @DisplayName("Theme Validation")
    inner class ThemeValidationTest {

        @Test
        @DisplayName("Should validate theme consistency")
        fun testThemeConsistency() {
            val themes = listOf(
                ParsedTheme("Theme1", "THEME1", mapOf("primary" to "#FF0000", "secondary" to "#00FF00")),
                ParsedTheme("Theme2", "THEME2", mapOf("primary" to "#0000FF", "secondary" to "#FFFF00"))
            )

            // Should not throw - both themes have same color keys
            assertDoesNotThrow {
                task.validateParsedThemes(themes)
            }
        }

        @Test
        @DisplayName("Should reject inconsistent theme colors")
        fun testThemeInconsistency() {
            val themes = listOf(
                ParsedTheme("Theme1", "THEME1", mapOf("primary" to "#FF0000", "secondary" to "#00FF00")),
                ParsedTheme("Theme2", "THEME2", mapOf("primary" to "#0000FF")) // Missing secondary
            )

            assertThrows<GradleException> {
                task.validateParsedThemes(themes)
            }
        }

        @Test
        @DisplayName("Should reject invalid color formats")
        fun testInvalidColorFormat() {
            val themes = listOf(
                ParsedTheme("Theme1", "THEME1", mapOf("primary" to "invalid-color"))
            )

            assertThrows<GradleException> {
                task.validateParsedThemes(themes)
            }
        }
    }

    @Nested
    @DisplayName("Color Normalization")
    inner class ColorNormalizationTest {

        @Test
        @DisplayName("Should normalize 3-character hex to 6-character")
        fun testNormalize3CharHex() {
            assertEquals("FF0000", task.normalizeHexColor("#F00"))
            assertEquals("00FF00", task.normalizeHexColor("#0F0"))
            assertEquals("0000FF", task.normalizeHexColor("#00F"))
        }

        @Test
        @DisplayName("Should keep 6-character hex unchanged")
        fun testNormalize6CharHex() {
            assertEquals("FF5722", task.normalizeHexColor("#FF5722"))
            assertEquals("000000", task.normalizeHexColor("#000000"))
        }

        @Test
        @DisplayName("Should keep 8-character ARGB hex unchanged")
        fun testNormalize8CharHex() {
            assertEquals("80FF5722", task.normalizeHexColor("#80FF5722"))
            assertEquals("FF000000", task.normalizeHexColor("#FF000000"))
        }
    }

    @Nested
    @DisplayName("Property Name Sanitization")
    inner class PropertyNameTest {

        @Test
        @DisplayName("Should sanitize property names correctly")
        fun testPropertyNameSanitization() {
            assertEquals("primary", task.sanitizePropertyName("primary"))
            assertEquals("primarydark", task.sanitizePropertyName("primary-dark"))
            assertEquals("onprimary", task.sanitizePropertyName("on-primary"))
            assertEquals("primarycontainer", task.sanitizePropertyName("primary_container"))
            assertEquals("test123", task.sanitizePropertyName("test-123"))
        }
    }
}

// Test helper class that exposes private methods for testing
class TestableGenerateThemesTask {

    fun parseTokenStudioFormat(fileName: String, tokenSet: TokenSet): List<ParsedTheme> {
        val themes = mutableListOf<ParsedTheme>()

        // Extract color tokens
        val colorTokens = tokenSet.color ?: emptyMap()
        val resolvedColors = resolveTokenReferences(colorTokens)

        // Check if themes are defined in metadata
        val themeMetadata = tokenSet.`$themes`
        if (themeMetadata != null && themeMetadata.isNotEmpty()) {
            // Use defined themes
            themeMetadata.forEach { (_, metadata) ->
                themes.add(ParsedTheme(
                    name = metadata.name,
                    enumName = metadata.enumName,
                    colors = resolvedColors
                ))
            }
        } else {
            // Default: create one theme from the file
            val themeName = fileName.split("-").joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
            val enumName = fileName.uppercase().replace("-", "_")

            themes.add(ParsedTheme(
                name = themeName,
                enumName = enumName,
                colors = resolvedColors
            ))
        }

        return themes
    }

    fun resolveTokenReferences(colorTokens: Map<String, ColorToken>): Map<String, String> {
        val resolved = mutableMapOf<String, String>()
        val unresolved = colorTokens.toMutableMap()

        // Enhanced resolution with color modifier support
        var maxIterations = 10
        while (unresolved.isNotEmpty() && maxIterations > 0) {
            val resolvedInIteration = mutableListOf<String>()

            unresolved.forEach { (tokenName, token) ->
                val value = token.getActualValue()
                if (value.startsWith("{") && value.endsWith("}")) {
                    // It's a reference
                    val referencePath = value.substring(1, value.length - 1)
                    val parts = referencePath.split(".")
                    if (parts.size == 2 && parts[0] == "color") {
                        val referencedToken = parts[1]
                        if (resolved.containsKey(referencedToken)) {
                            val baseColor = resolved[referencedToken]!!
                            val modifier = token.getModifier()

                            val finalColor = if (modifier != null) {
                                applyColorModifier(baseColor, modifier)
                            } else {
                                baseColor
                            }

                            resolved[tokenName] = finalColor
                            resolvedInIteration.add(tokenName)
                        }
                    }
                } else {
                    // It's a direct value, apply modifier if present
                    val modifier = token.getModifier()
                    val finalColor = if (modifier != null) {
                        applyColorModifier(value, modifier)
                    } else {
                        value
                    }
                    resolved[tokenName] = finalColor
                    resolvedInIteration.add(tokenName)
                }
            }

            resolvedInIteration.forEach { unresolved.remove(it) }
            maxIterations--
        }

        // Add any remaining unresolved tokens as-is (they might be invalid references)
        unresolved.forEach { (tokenName, token) ->
            resolved[tokenName] = token.getActualValue()
        }

        return resolved
    }

    fun applyColorModifier(baseColor: String, modifier: ColorModifier): String {
        try {
            val rgb = parseHexColor(baseColor)

            return when (modifier.type.lowercase()) {
                "lighten" -> lightenColor(rgb, modifier.value)
                "darken" -> darkenColor(rgb, modifier.value)
                "alpha" -> applyAlpha(rgb, modifier.value)
                "mix" -> mixColors(rgb, modifier.color ?: "#FFFFFF", modifier.value)
                else -> baseColor
            }
        } catch (e: Exception) {
            return baseColor
        }
    }

    private fun parseHexColor(hex: String): Triple<Int, Int, Int> {
        val cleaned = hex.removePrefix("#")
        val expandedHex = if (cleaned.length == 3) {
            cleaned.map { "$it$it" }.joinToString("")
        } else {
            cleaned
        }

        if (expandedHex.length != 6) {
            throw IllegalArgumentException("Invalid hex color format: $hex")
        }

        val r = expandedHex.substring(0, 2).toInt(16)
        val g = expandedHex.substring(2, 4).toInt(16)
        val b = expandedHex.substring(4, 6).toInt(16)

        return Triple(r, g, b)
    }

    private fun lightenColor(rgb: Triple<Int, Int, Int>, amount: Double): String {
        val (r, g, b) = rgb
        val factor = 1.0 + amount

        val newR = minOf(255, (r * factor).toInt())
        val newG = minOf(255, (g * factor).toInt())
        val newB = minOf(255, (b * factor).toInt())

        return "#%02X%02X%02X".format(newR, newG, newB)
    }

    private fun darkenColor(rgb: Triple<Int, Int, Int>, amount: Double): String {
        val (r, g, b) = rgb
        val factor = 1.0 - amount

        val newR = maxOf(0, (r * factor).toInt())
        val newG = maxOf(0, (g * factor).toInt())
        val newB = maxOf(0, (b * factor).toInt())

        return "#%02X%02X%02X".format(newR, newG, newB)
    }

    private fun applyAlpha(rgb: Triple<Int, Int, Int>, alpha: Double): String {
        val (r, g, b) = rgb
        val alphaValue = (alpha * 255).toInt().coerceIn(0, 255)

        return "#%02X%02X%02X%02X".format(alphaValue, r, g, b)
    }

    private fun mixColors(rgb1: Triple<Int, Int, Int>, color2: String, ratio: Double): String {
        val rgb2 = parseHexColor(color2)
        val (r1, g1, b1) = rgb1
        val (r2, g2, b2) = rgb2

        val mixRatio = ratio.coerceIn(0.0, 1.0)
        val invRatio = 1.0 - mixRatio

        val newR = (r1 * invRatio + r2 * mixRatio).toInt().coerceIn(0, 255)
        val newG = (g1 * invRatio + g2 * mixRatio).toInt().coerceIn(0, 255)
        val newB = (b1 * invRatio + b2 * mixRatio).toInt().coerceIn(0, 255)

        return "#%02X%02X%02X".format(newR, newG, newB)
    }

    fun validateParsedThemes(themes: List<ParsedTheme>) {
        if (themes.isEmpty()) return

        // Get all color keys from first theme as reference
        val referenceTheme = themes.first()
        val referenceColors = referenceTheme.colors.keys.sorted()

        // Validate all themes have the same color keys
        themes.forEach { theme ->
            val themeColors = theme.colors.keys.sorted()

            if (themeColors != referenceColors) {
                val missing = referenceColors - themeColors.toSet()
                val extra = themeColors - referenceColors.toSet()

                val errorMessage = buildString {
                    append("Theme '${theme.name}' has inconsistent colors compared to '${referenceTheme.name}':")
                    if (missing.isNotEmpty()) {
                        append("\n  Missing colors: ${missing.joinToString(", ")}")
                    }
                    if (extra.isNotEmpty()) {
                        append("\n  Extra colors: ${extra.joinToString(", ")}")
                    }
                    append("\n  Expected colors: ${referenceColors.joinToString(", ")}")
                    append("\n  Actual colors: ${themeColors.joinToString(", ")}")
                }

                throw GradleException(errorMessage)
            }

            // Validate hex color format
            theme.colors.forEach { (colorName, colorValue) ->
                if (!isValidHexColor(colorValue) && !isValidTokenReference(colorValue)) {
                    throw GradleException("Invalid color format in theme '${theme.name}' for color '$colorName': '$colorValue'. Expected format: #RRGGBB, #RGB, or {color.reference}")
                }
            }
        }
    }

    fun isValidTokenReference(value: String): Boolean {
        return value.startsWith("{") && value.endsWith("}") && value.contains(".")
    }

    fun isValidHexColor(color: String): Boolean {
        return color.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3}|[A-Fa-f0-9]{8})$"))
    }

    fun normalizeHexColor(color: String): String {
        val cleaned = color.removePrefix("#")
        return when (cleaned.length) {
            3 -> cleaned.map { "$it$it" }.joinToString("")
            6 -> cleaned
            8 -> cleaned // ARGB format, keep as-is
            else -> cleaned
        }
    }

    fun sanitizePropertyName(colorName: String): String {
        return colorName.replace(Regex("[^a-zA-Z0-9]"), "")
            .replaceFirstChar { it.lowercase() }
    }
}