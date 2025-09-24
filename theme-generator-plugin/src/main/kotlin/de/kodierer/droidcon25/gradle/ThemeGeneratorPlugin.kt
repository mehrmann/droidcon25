package de.kodierer.droidcon25.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.InputFiles
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SkipWhenEmpty
import java.io.File
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import com.android.build.gradle.BaseExtension
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.GradleException

/**
 * Theme Generator Gradle Plugin
 *
 * Generates theme-related Kotlin code from JSON theme definitions.
 * Provides a configurable DSL for input folder and package name with automatic source set configuration.
 */
class ThemeGeneratorPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Ensure Android plugin is applied first
        checkIsAndroidProject(target)

        // Create the extension for DSL configuration
        val extension = target.extensions.create(
            "themeGenerator",
            ThemeGeneratorExtension::class.java)

        // Register the theme generation task
        val generateThemesTask = target.tasks.register(
            "generateThemes",
            GenerateThemesTask::class.java)

        // Configure automatic source sets using AndroidComponentsExtension
        try {
            val androidComponentsExt = target.extensions.getByType(AndroidComponentsExtension::class.java)
            val androidBaseExt = target.extensions.getByType(BaseExtension::class.java)

            // Configure source sets when first variant is detected
            var configured = false
            androidComponentsExt.onVariants { _ ->
                if (!configured) {
                    target.afterEvaluate {
                        val outputDir = extension.outputDirectory.get().asFile

                        // Configure main source set which is shared across all variants
                        androidBaseExt.sourceSets.getByName("main") {
                            kotlin.srcDir(outputDir)
                        }
                    }
                    configured = true
                }
            }

        } catch (e: Exception) {
            target.logger.warn("Could not configure Android source sets automatically: ${e.message}")
            target.logger.info("Please manually add to your app/build.gradle.kts:")
            target.logger.info("   sourceSets { getByName(\"main\") { kotlin.srcDir(\"build/generated/themes/src\") } }")
        }

        target.afterEvaluate {
            target.logger.lifecycle("[THEME-GENERATOR] Entering afterEvaluate block")
            // Configure the task with extension values
            generateThemesTask.configure {
                inputDirectory.set(extension.inputDirectory)
                outputDirectory.set(extension.outputDirectory)
                packageName.set(extension.packageName)
                group = "theme-generator"
                description = "Generates theme classes from JSON definitions"
            }


            // Make sure theme generation runs before compilation
            target.tasks.matching { task ->
                task.name.contains("compile") && task.name.contains("Kotlin")
            }.configureEach {
                dependsOn(generateThemesTask)
            }
        }
    }

    private fun checkIsAndroidProject(project: Project) {
        val hasAndroidAppPlugin = project.plugins.findPlugin("com.android.application") != null
        val hasAndroidLibPlugin = project.plugins.findPlugin("com.android.library") != null

        if (!hasAndroidAppPlugin && !hasAndroidLibPlugin) {
            throw GradleException("Theme Generator Plugin requires 'com.android.application' or 'com.android.library' plugin to be applied first")
        }
    }
}

/**
 * DSL Extension for configuring the theme generator
 */
abstract class ThemeGeneratorExtension {
    /**
     * Directory containing JSON theme files
     */
    abstract val inputDirectory: DirectoryProperty

    /**
     * Directory where generated Kotlin files will be placed
     */
    abstract val outputDirectory: DirectoryProperty

    /**
     * Package name for generated classes
     */
    abstract val packageName: Property<String>

    init {
        // Set default values
        packageName.convention("generated.themes")
    }
}

/**
 * Task that generates theme classes from JSON definitions
 */
abstract class GenerateThemesTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    val inputFiles: FileCollection
        get() = project.fileTree(inputDirectory.get().asFile) {
            include("*.json")
        }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @TaskAction
    fun generate() {
        val inputDir = inputDirectory.get().asFile
        val outputDir = outputDirectory.get().asFile
        val pkg = packageName.get()

        logger.info("Generating themes from: ${inputDir.absolutePath}")
        logger.info("Output directory: ${outputDir.absolutePath}")
        logger.info("Package name: $pkg")

        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val json = Json { ignoreUnknownKeys = true }
        val parsedThemes = mutableListOf<ParsedTheme>()

        // Read all JSON token files
        if (inputDir.exists()) {
            inputDir.listFiles { file -> file.extension == "json" }?.forEach { tokenFile ->
                logger.info("Processing token file: ${tokenFile.name}")
                val tokenContent = tokenFile.readText()

                try {
                    // Try Token Studio format first
                    val tokenSet = json.decodeFromString<TokenSet>(tokenContent)
                    val themes = parseTokenStudioFormat(tokenFile.nameWithoutExtension, tokenSet)
                    parsedThemes.addAll(themes)
                } catch (e: Exception) {
                    try {
                        // Fallback to legacy format
                        val legacyTheme = json.decodeFromString<LegacyTheme>(tokenContent)
                        val theme = ParsedTheme(
                            name = legacyTheme.name,
                            enumName = legacyTheme.enumName,
                            colors = legacyTheme.colors
                        )
                        parsedThemes.add(theme)
                        logger.info("Loaded legacy theme: ${legacyTheme.name}")
                    } catch (legacyError: Exception) {
                        logger.error("Failed to parse ${tokenFile.name} as Token Studio or legacy format")
                        logger.error("Token Studio error: ${e.message}")
                        logger.error("Legacy error: ${legacyError.message}")
                        throw GradleException("Failed to parse theme file: ${tokenFile.name}")
                    }
                }
            }
        }

        if (parsedThemes.isEmpty()) {
            logger.warn("No theme files found in ${inputDir.absolutePath}")
            return
        }

        logger.info("Found ${parsedThemes.size} themes: ${parsedThemes.map { it.name }}")

        // Validate themes consistency and color format
        validateParsedThemes(parsedThemes)

        // Generate files
        generateColorFile(outputDir, parsedThemes, pkg)
        generateThemeFile(outputDir, parsedThemes, pkg)

        logger.info("Theme generation completed successfully")
    }

    private fun parseTokenStudioFormat(fileName: String, tokenSet: TokenSet): List<ParsedTheme> {
        val themes = mutableListOf<ParsedTheme>()

        // Extract color tokens
        val colorTokens = tokenSet.color ?: emptyMap()
        val resolvedColors = resolveTokenReferences(colorTokens)

        // Check if themes are defined in metadata
        val themeMetadata = tokenSet.`$themes`
        if (themeMetadata != null && themeMetadata.isNotEmpty()) {
            // Use defined themes
            themeMetadata.forEach { (themeKey, metadata) ->
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

    private fun resolveTokenReferences(colorTokens: Map<String, ColorToken>): Map<String, String> {
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

    private fun applyColorModifier(baseColor: String, modifier: ColorModifier): String {
        try {
            val rgb = parseHexColor(baseColor)

            return when (modifier.type.lowercase()) {
                "lighten" -> lightenColor(rgb, modifier.value)
                "darken" -> darkenColor(rgb, modifier.value)
                "alpha" -> applyAlpha(rgb, modifier.value)
                "mix" -> mixColors(rgb, modifier.color ?: "#FFFFFF", modifier.value)
                else -> {
                    logger.warn("Unknown color modifier type: ${modifier.type}")
                    baseColor
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to apply color modifier to $baseColor: ${e.message}")
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

    private fun validateParsedThemes(themes: List<ParsedTheme>) {
        if (themes.isEmpty()) return

        // Get all color keys from first theme as reference
        val referenceTheme = themes.first()
        val referenceColors = referenceTheme.colors.keys.sorted()

        logger.info("Reference color set from '${referenceTheme.name}': $referenceColors")

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

        logger.info("✓ All themes have consistent color sets with ${referenceColors.size} colors")
        logger.info("✓ All color values are valid format")
    }

    private fun isValidTokenReference(value: String): Boolean {
        return value.startsWith("{") && value.endsWith("}") && value.contains(".")
    }


    private fun isValidHexColor(color: String): Boolean {
        return color.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3}|[A-Fa-f0-9]{8})$"))
    }

    private fun generateColorFile(outputDir: File, themes: List<ParsedTheme>, packageName: String) {
        val packagePath = packageName.replace('.', '/')
        val colorFile = File(outputDir, "$packagePath/GeneratedColors.kt")
        colorFile.parentFile.mkdirs()

        // Get all color names from first theme (already validated to be consistent)
        val colorNames = themes.first().colors.keys.sorted()

        // Generate interface with all color properties
        val interfaceProperties = colorNames.joinToString("\n    ") { colorName ->
            val propertyName = sanitizePropertyName(colorName)
            "val $propertyName: Color"
        }

        // Generate theme objects
        val themeObjects = themes.joinToString("\n\n") { theme ->
            val objectName = theme.enumName.lowercase().replaceFirstChar { it.uppercase() }
            val colorProperties = colorNames.joinToString("\n    ") { colorName ->
                val propertyName = sanitizePropertyName(colorName)
                val colorValue = theme.colors[colorName]!!
                val hexValue = normalizeHexColor(colorValue)
                "override val $propertyName = Color(0xFF$hexValue)"
            }

            """@Immutable
object $objectName : ThemeColorObject {
    $colorProperties
}"""
        }

        colorFile.writeText("""package $packageName

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import de.kodierer.droidcon25.ui.theme.ThemeColorObject

// Auto-generated interface based on theme color keys
interface ThemeColorObject {
    $interfaceProperties
}

$themeObjects
""")
    }

    private fun sanitizePropertyName(colorName: String): String {
        // Convert color names to valid Kotlin property names
        return colorName.replace(Regex("[^a-zA-Z0-9]"), "")
            .replaceFirstChar { it.lowercase() }
    }

    private fun normalizeHexColor(color: String): String {
        val cleaned = color.removePrefix("#")
        // Convert 3-digit hex to 6-digit hex
        return when (cleaned.length) {
            3 -> cleaned.map { "$it$it" }.joinToString("")
            6 -> cleaned
            8 -> cleaned // ARGB format, keep as-is
            else -> cleaned
        }
    }

    private fun generateThemeFile(outputDir: File, themes: List<ParsedTheme>, packageName: String) {
        val packagePath = packageName.replace('.', '/')
        val themeFile = File(outputDir, "$packagePath/GeneratedThemes.kt")
        themeFile.parentFile.mkdirs()

        // Generate enum entries
        val enumEntries = themes.joinToString(",\n    ") { theme ->
            "${theme.enumName}(\"${theme.name}\")"
        }

        // Generate theme data for dynamic access
        val themeDataEntries = themes.joinToString(",\n        ") { theme ->
            val objectName = theme.enumName.lowercase().replaceFirstChar { it.uppercase() }
            """ThemeData(
            theme = AppTheme.${theme.enumName},
            name = "${theme.name}",
            colorObject = $objectName,
            appColors = AppColors($objectName)
        )"""
        }

        themeFile.writeText("""package $packageName

import de.kodierer.droidcon25.ui.theme.AppColors
import de.kodierer.droidcon25.ui.theme.ThemeColorObject
import de.kodierer.droidcon25.ui.theme.ThemeData

enum class AppTheme(val displayName: String) {
    $enumEntries
}

object AllThemes {
    val themes = listOf(
        $themeDataEntries
    )

    val allAppThemes = themes.map { it.theme }
    val themeMap = themes.associateBy { it.theme }

    fun getThemeData(theme: AppTheme): ThemeData = themeMap[theme]
        ?: error("Theme data not found for ${'$'}theme")
}
""")
    }
}

// Token Studio format support
@Serializable
data class TokenSet(
    val color: Map<String, ColorToken>? = null,
    val spacing: Map<String, DimensionToken>? = null,
    val typography: Map<String, TypographyToken>? = null,
    // Add metadata for theme identification (non-standard but useful)
    val `$themes`: Map<String, ThemeMetadata>? = null
)

@Serializable
data class ThemeMetadata(
    val name: String,
    val enumName: String
)

@Serializable
data class ColorToken(
    val value: String,                    // "#FF0000" or "{color.primary}"
    val type: String = "color",
    val description: String? = null,
    // W3C DTCG format support
    @SerialName("\$value") val dollarValue: String? = null,
    @SerialName("\$type") val dollarType: String? = null,
    @SerialName("\$description") val dollarDescription: String? = null,
    // Token Studio color modifier support
    @SerialName("\$extensions") val extensions: TokenExtensions? = null
) {
    // Helper to get actual value regardless of format
    fun getActualValue(): String = dollarValue ?: value
    fun getActualType(): String = dollarType ?: type
    fun getActualDescription(): String? = dollarDescription ?: description
    fun getModifier(): ColorModifier? = extensions?.studioTokens?.modify
}

@Serializable
data class DimensionToken(
    val value: String,                    // "16px" or "{spacing.base}"
    val type: String = "dimension",
    val description: String? = null,
    @SerialName("\$value") val dollarValue: String? = null,
    @SerialName("\$type") val dollarType: String? = null,
    @SerialName("\$description") val dollarDescription: String? = null
)

@Serializable
data class TypographyToken(
    val value: TypographyValue? = null,
    val type: String = "typography",
    val description: String? = null,
    @SerialName("\$value") val dollarValue: TypographyValue? = null,
    @SerialName("\$type") val dollarType: String? = null,
    @SerialName("\$description") val dollarDescription: String? = null
)

@Serializable
data class TypographyValue(
    val fontFamily: String? = null,
    val fontSize: String? = null,
    val fontWeight: String? = null,
    val lineHeight: String? = null,
    val letterSpacing: String? = null
)

// Legacy theme format for backward compatibility
@Serializable
data class LegacyTheme(
    val name: String,
    val enumName: String,
    val colors: Map<String, String>
)

@Serializable
data class TokenExtensions(
    @SerialName("studio.tokens") val studioTokens: StudioTokens? = null
)

@Serializable
data class StudioTokens(
    val modify: ColorModifier? = null
)

@Serializable
data class ColorModifier(
    val type: String,        // "lighten", "darken", "alpha", "mix"
    val value: Double,       // 0.0 to 1.0
    val space: String? = null, // "lch", "srgb", "p3", "hsl"
    val color: String? = null  // For mix modifier: the color to mix with
)

// Internal representation after parsing
data class ParsedTheme(
    val name: String,
    val enumName: String,
    val colors: Map<String, String>
)
