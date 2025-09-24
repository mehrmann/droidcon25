package de.kodierer.droidcon25.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.Project

/**
 * Extension for configuring the theme generator plugin through Gradle DSL.
 *
 * Example usage in build.gradle.kts:
 * ```
 * themeGenerator {
 *     inputDirectory.set(file("themes"))
 *     outputDirectory.set(file("build/generated/themes/src"))
 *     packageName.set("com.example.generated")
 * }
 * ```
 */
abstract class ThemeGeneratorExtension(project: Project) {

    /**
     * The directory containing theme JSON files.
     * Defaults to "themes" in the project root.
     */
    abstract val inputDirectory: DirectoryProperty

    /**
     * The directory where generated Kotlin files will be written.
     * Defaults to "build/generated/themes/src" in the project root.
     */
    abstract val outputDirectory: DirectoryProperty

    /**
     * The package name for generated Kotlin files.
     * Defaults to the project's group + ".generated"
     */
    abstract val packageName: Property<String>

    init {
        // Set default values
        inputDirectory.convention(project.layout.projectDirectory.dir("themes"))
        outputDirectory.convention(project.layout.buildDirectory.dir("generated/themes/src"))
        packageName.convention("${project.group}.generated")
    }
}