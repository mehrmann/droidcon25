package de.kodierer.droidcon25.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task that generates theme-related Kotlin files from JSON theme definitions.
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
        val packageNameValue = packageName.get()

        logger.lifecycle("Generating themes from: ${inputDir.absolutePath}")
        logger.lifecycle("Output directory: ${outputDir.absolutePath}")
        logger.lifecycle("Package name: $packageNameValue")

        // Ensure output directory exists
        outputDir.mkdirs()

        try {
            // Parse theme files
            val parser = ThemeParser()
            val themes = parser.parseThemeFiles(inputDir)

            logger.lifecycle("Found ${themes.size} theme(s): ${themes.map { it.name }}")

            // Validate themes
            ThemeValidator.validateParsedThemes(themes)

            // Generate code files
            val codeGenerator = ThemeCodeGenerator()
            codeGenerator.generateThemeFiles(outputDir, themes, packageNameValue)

            logger.lifecycle("Successfully generated theme files in ${outputDir.absolutePath}")

        } catch (e: Exception) {
            logger.error("Theme generation failed", e)
            throw e
        }
    }
}