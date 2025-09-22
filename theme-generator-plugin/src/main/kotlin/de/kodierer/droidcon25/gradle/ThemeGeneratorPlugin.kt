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
            androidComponentsExt.onVariants { variant ->
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

        // Generate files
        generateColorFile(outputDir, themes, pkg)
        generateThemeFile(outputDir, themes, pkg)

        logger.info("Theme generation completed successfully")
    }

    private fun generateColorFile(outputDir: File, themes: List<Theme>, packageName: String) {
        val packagePath = packageName.replace('.', '/')
        val colorFile = File(outputDir, "$packagePath/GeneratedColors.kt")
        colorFile.parentFile.mkdirs()

        val themeObjects = themes.map { theme ->
            val objectName = theme.enumName.lowercase().replaceFirstChar { it.uppercase() }
            """@Immutable
object $objectName : ThemeColorObject {
    override val primary = Color(0xFF${theme.colors.primary.removePrefix("#")})
    override val primaryLight = Color(0xFF${theme.colors.primaryLight.removePrefix("#")})
    override val primaryDark = Color(0xFF${theme.colors.primaryDark.removePrefix("#")})
    override val secondary = Color(0xFF${theme.colors.secondary.removePrefix("#")})
    override val secondaryLight = Color(0xFF${theme.colors.secondaryLight.removePrefix("#")})
    override val secondaryDark = Color(0xFF${theme.colors.secondaryDark.removePrefix("#")})
    override val tertiary = Color(0xFF${theme.colors.tertiary.removePrefix("#")})
    override val onPrimary = Color(0xFF${theme.colors.onPrimary.removePrefix("#")})
    override val onSecondary = Color(0xFF${theme.colors.onSecondary.removePrefix("#")})
    override val onTertiary = Color(0xFF${theme.colors.onTertiary.removePrefix("#")})
    override val background = Color(0xFF${theme.colors.background.removePrefix("#")})
    override val surface = Color(0xFF${theme.colors.surface.removePrefix("#")})
    override val onBackground = Color(0xFF${theme.colors.onBackground.removePrefix("#")})
    override val onSurface = Color(0xFF${theme.colors.onSurface.removePrefix("#")})
    override val primaryContainer = Color(0xFF${theme.colors.primaryContainer.removePrefix("#")})
    override val secondaryContainer = Color(0xFF${theme.colors.secondaryContainer.removePrefix("#")})
    override val onPrimaryContainer = Color(0xFF${theme.colors.onPrimaryContainer.removePrefix("#")})
    override val onSecondaryContainer = Color(0xFF${theme.colors.onSecondaryContainer.removePrefix("#")})
}"""
        }.joinToString("\n\n")

        colorFile.writeText("""package $packageName

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import de.kodierer.droidcon25.ui.theme.ThemeColorObject

$themeObjects
""")
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
data class ThemeColors(
    val primary: String,
    val primaryLight: String,
    val primaryDark: String,
    val secondary: String,
    val secondaryLight: String,
    val secondaryDark: String,
    val tertiary: String,
    val onPrimary: String,
    val onSecondary: String,
    val onTertiary: String,
    val background: String,
    val surface: String,
    val onBackground: String,
    val onSurface: String,
    val primaryContainer: String,
    val secondaryContainer: String,
    val onPrimaryContainer: String,
    val onSecondaryContainer: String
)

@Serializable
data class Theme(
    val name: String,
    val enumName: String,
    val colors: ThemeColors
)
