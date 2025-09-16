import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.util.Properties

fun Project.createCodeGenerationTask(): TaskProvider<*> {
    return tasks.register("generateSources") {
        val outputDir = file("${layout.buildDirectory.get()}/generated/codegen/src")
        val outputFile = file("$outputDir/de/kodierer/droidcon25/generated/GeneratedStrings.kt")
        val configFile = rootProject.file("config/constants.properties")

        // Mark buildSrc and config as input so task re-runs when they change
        inputs.dir(rootProject.file("buildSrc/src"))
        inputs.file(configFile)
        outputs.dir(outputDir)

        // Alternative approach for demo: Force task to always run (slower builds)
        // outputs.upToDateWhen { false }

        doLast {
            generateStrings(outputDir, outputFile, configFile)
        }
    }
}

private fun generateStrings(outputDir: File, outputFile: File, configFile: File) {
    outputDir.mkdirs()
    outputFile.parentFile.mkdirs()

    val properties = Properties()
    if (configFile.exists()) {
        configFile.inputStream().use { properties.load(it) }
    }

    val constants = properties.entries.joinToString("\n") { (key, value) ->
        "    const val $key = \"$value\""
    }

    outputFile.writeText("""package de.kodierer.droidcon25.generated

object GeneratedStrings {
$constants
}
""")
}
