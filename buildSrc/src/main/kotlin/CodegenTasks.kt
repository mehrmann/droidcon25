import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.io.File

fun Project.createCodeGenerationTask(): TaskProvider<*> {
    return tasks.register("generateSources") {
        val outputDir = file("${layout.buildDirectory.get()}/generated/codegen/src")
        val outputFile = file("$outputDir/de/kodierer/droidcon25/generated/GeneratedStrings.kt")

        // Mark buildSrc as input so task re-runs when buildSrc changes
        inputs.dir(rootProject.file("buildSrc/src"))
        outputs.dir(outputDir)

        // Alternative approach for demo: Force task to always run (slower builds)
        // outputs.upToDateWhen { false }

        doLast {
            generateStrings(outputDir, outputFile)
        }
    }
}

private fun generateStrings(outputDir: File, outputFile: File) {
    outputDir.mkdirs()
    outputFile.parentFile.mkdirs()

    outputFile.writeText("""
        package de.kodierer.droidcon25.generated

        object GeneratedStrings {
            const val APP_TITLE = "Hello Berlin!!!"
        }
    """.trimIndent())
}
