package de.kodierer.droidcon25.gradle

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.gradle.api.GradleException
import java.io.File

/**
 * Handles parsing of theme files and processing of token data.
 */
class ThemeParser {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parses all theme files in the input directory.
     */
    fun parseThemeFiles(inputDir: File): List<ParsedTheme> {
        val themes = mutableListOf<ParsedTheme>()

        inputDir.listFiles { _, name -> name.endsWith(".json") }?.forEach { file ->
            try {
                val content = file.readText()
                val tokenSet = json.decodeFromString<TokenSet>(content)
                val tokenStudioThemes = parseTokenStudioFormat(file.name, tokenSet)
                themes.addAll(tokenStudioThemes)
            } catch (e: Exception) {
                throw GradleException("Failed to parse theme file ${file.name} as Token Studio format: ${e.message}", e)
            }
        }

        return themes
    }

    /**
     * Parses Token Studio format theme file.
     */
    private fun parseTokenStudioFormat(fileName: String, tokenSet: TokenSet): List<ParsedTheme> {
        val themes = mutableListOf<ParsedTheme>()

        // Extract color tokens
        val colorTokens = tokenSet.color ?: return emptyList()

        // Extract theme metadata
        val themeMetadata = tokenSet.themes

        if (themeMetadata != null) {
            // Multiple themes defined
            themeMetadata.forEach { (themeKey, metadata) ->
                val resolvedColors = resolveTokenReferences(colorTokens)
                themes.add(ParsedTheme(
                    name = metadata.name,
                    enumName = metadata.enumName ?: themeKey.uppercase().replace(Regex("[^A-Z0-9_]"), "_"),
                    colors = resolvedColors
                ))
            }
        } else {
            // Single theme - use filename as theme name
            val themeName = fileName.removeSuffix(".json").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
            val resolvedColors = resolveTokenReferences(colorTokens)
            themes.add(ParsedTheme(
                name = themeName,
                enumName = themeName.uppercase().replace(Regex("[^A-Z0-9_]"), "_"),
                colors = resolvedColors
            ))
        }

        return themes
    }

    /**
     * Resolves token references and applies color modifiers.
     */
    private fun resolveTokenReferences(colorTokens: Map<String, ColorToken>): Map<String, String> {
        val resolvedColors = mutableMapOf<String, String>()
        val processing = mutableSetOf<String>()

        fun resolveToken(tokenName: String): String {
            if (tokenName in processing) {
                throw GradleException("Circular reference detected for token: $tokenName")
            }

            if (resolvedColors.containsKey(tokenName)) {
                return resolvedColors[tokenName]!!
            }

            val token = colorTokens[tokenName]
                ?: throw GradleException("Token not found: $tokenName")

            processing.add(tokenName)

            try {
                val baseValue = token.getActualValue()
                val resolvedValue = if (ColorUtils.isValidTokenReference(baseValue)) {
                    // Reference to another token
                    val referencedTokenName = baseValue.removeSurrounding("{", "}")
                        .removePrefix("color.")
                    resolveToken(referencedTokenName)
                } else {
                    // Direct color value
                    if (!ColorUtils.isValidHexColor(baseValue)) {
                        throw GradleException("Invalid color value for token $tokenName: $baseValue")
                    }
                    ColorUtils.normalizeHexColor(baseValue)
                }

                // Apply modifier if present
                val finalValue = token.getModifier()?.let { modifier ->
                    ColorUtils.applyColorModifier(resolvedValue, modifier)
                } ?: resolvedValue

                resolvedColors[tokenName] = finalValue
                return finalValue

            } finally {
                processing.remove(tokenName)
            }
        }

        // Resolve all tokens
        colorTokens.keys.forEach { tokenName ->
            resolveToken(tokenName)
        }

        return resolvedColors
    }
}