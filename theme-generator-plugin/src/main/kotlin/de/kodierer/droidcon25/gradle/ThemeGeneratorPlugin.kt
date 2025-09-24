package de.kodierer.droidcon25.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
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
            ThemeGeneratorExtension::class.java,
            target)

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
