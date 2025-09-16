import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

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

fun Project.createCodeGenerationTask(): TaskProvider<*> {
    return tasks.register("generateSources") {
        val outputDir = file("${layout.buildDirectory.get()}/generated/codegen/src")
        val themesDir = rootProject.file("themes")

        // Mark buildSrc and themes as input so task re-runs when they change
        inputs.dir(rootProject.file("buildSrc/src"))
        inputs.dir(themesDir)
        outputs.dir(outputDir)

        doLast {
            generateThemes(outputDir, themesDir)
        }
    }
}

private fun generateThemes(outputDir: File, themesDir: File) {
    outputDir.mkdirs()

    val json = Json { ignoreUnknownKeys = true }
    val themes = mutableListOf<Theme>()

    // Read all JSON theme files
    if (themesDir.exists()) {
        themesDir.listFiles { file -> file.extension == "json" }?.forEach { themeFile ->
            val themeContent = themeFile.readText()
            val theme = json.decodeFromString<Theme>(themeContent)
            themes.add(theme)
        }
    }

    // Generate Color.kt
    generateColorFile(outputDir, themes)

    // Generate Theme.kt
    generateThemeFile(outputDir, themes)
}

private fun generateColorFile(outputDir: File, themes: List<Theme>) {
    val colorFile = File(outputDir, "de/kodierer/droidcon25/generated/GeneratedColors.kt")
    colorFile.parentFile.mkdirs()

    val themeObjects = themes.map { theme ->
        val objectName = theme.enumName.lowercase().replaceFirstChar { it.uppercase() }
        """object $objectName : ThemeColorObject {
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

    colorFile.writeText("""package de.kodierer.droidcon25.generated

import androidx.compose.ui.graphics.Color
import de.kodierer.droidcon25.ui.theme.ThemeColorObject

$themeObjects
""")
}

private fun generateThemeFile(outputDir: File, themes: List<Theme>) {
    val themeFile = File(outputDir, "de/kodierer/droidcon25/generated/GeneratedThemes.kt")
    themeFile.parentFile.mkdirs()

    // Generate enum entries
    val enumEntries = themes.joinToString(",\n    ") { theme ->
        "${theme.enumName}(\"${theme.name}\")"
    }

    // Generate theme data for dynamic access - no individual color instances needed
    val themeDataEntries = themes.joinToString(",\n        ") { theme ->
        val objectName = theme.enumName.lowercase().replaceFirstChar { it.uppercase() }
        """ThemeData(
            theme = AppTheme.${theme.enumName},
            name = "${theme.name}",
            colorObject = $objectName,
            appColors = AppColors($objectName)
        )"""
    }

    themeFile.writeText("""package de.kodierer.droidcon25.generated

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