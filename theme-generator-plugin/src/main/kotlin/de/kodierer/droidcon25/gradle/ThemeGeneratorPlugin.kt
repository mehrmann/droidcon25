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
        val themes = mutableListOf<Theme>()

        // Read all JSON theme files
        if (inputDir.exists()) {
            inputDir.listFiles { file -> file.extension == "json" }?.forEach { themeFile ->
                logger.info("Processing theme file: ${themeFile.name}")
                val themeContent = themeFile.readText()
                val theme = json.decodeFromString<Theme>(themeContent)
                themes.add(theme)
            }
        }

        if (themes.isEmpty()) {
            logger.warn("No theme files found in ${inputDir.absolutePath}")
            return
        }

        logger.info("Found ${themes.size} themes: ${themes.map { it.name }}")

        // Validate themes consistency and color format
        validateThemes(themes)

        // Generate files
        generateColorFile(outputDir, themes, pkg)
        generateThemeFile(outputDir, themes, pkg)

        logger.info("Theme generation completed successfully")
    }

    private fun validateThemes(themes: List<Theme>) {
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
                if (!isValidHexColor(colorValue)) {
                    throw GradleException("Invalid hex color format in theme '${theme.name}' for color '$colorName': '$colorValue'. Expected format: #RRGGBB or #RGB")
                }
            }
        }

        logger.info("✓ All themes have consistent color sets with ${referenceColors.size} colors")
        logger.info("✓ All color values are valid hex format")
    }

    private fun isValidHexColor(color: String): Boolean {
        return color.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$"))
    }

    private fun generateColorFile(outputDir: File, themes: List<Theme>, packageName: String) {
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
        return if (cleaned.length == 3) {
            cleaned.map { "$it$it" }.joinToString("")
        } else {
            cleaned
        }
    }

    private fun generateThemeFile(outputDir: File, themes: List<Theme>, packageName: String) {
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

@Serializable
data class Theme(
    val name: String,
    val enumName: String,
    val colors: Map<String, String>
)
