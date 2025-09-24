package de.kodierer.droidcon25.gradle

import com.squareup.kotlinpoet.*
import kotlin.io.path.Path
import java.io.File

/**
 * Generates Kotlin code from parsed theme data using KotlinPoet.
 */
class ThemeCodeGenerator {

    /**
     * Generates both color and theme files for the given themes.
     */
    fun generateThemeFiles(outputDir: File, themes: List<ParsedTheme>, packageName: String) {
        generateColorFile(outputDir, themes, packageName)
        generateThemeFile(outputDir, themes, packageName)
    }

    /**
     * Generates the colors file containing the ThemeColorObject interface and color objects.
     */
    fun generateColorFile(outputDir: File, themes: List<ParsedTheme>, packageName: String) {
        val colorNames = themes.first().colors.keys.sorted()
        val colorClassName = ClassName("androidx.compose.ui.graphics", "Color")
        val immutableClassName = ClassName("androidx.compose.runtime", "Immutable")

        // Create ThemeColorObject interface
        val themeColorObjectInterface = TypeSpec.interfaceBuilder("ThemeColorObject")
            .apply {
                colorNames.forEach { colorName ->
                    val propertyName = ColorUtils.sanitizePropertyName(colorName)
                    addProperty(
                        PropertySpec.builder(propertyName, colorClassName)
                            .build()
                    )
                }
            }
            .build()

        // Create color objects for each theme
        val colorObjects = themes.map { theme ->
            val objectName = theme.enumName.lowercase()
                .split("_")
                .joinToString("_") { part -> part.replaceFirstChar { it.uppercaseChar() } }

            TypeSpec.objectBuilder(objectName)
                .addAnnotation(immutableClassName)
                .addSuperinterface(ClassName(packageName, "ThemeColorObject"))
                .apply {
                    colorNames.forEach { colorName ->
                        val propertyName = ColorUtils.sanitizePropertyName(colorName)
                        val colorValue = theme.colors[colorName]!!
                        val androidColorValue = convertToAndroidColor(colorValue)

                        addProperty(
                            PropertySpec.builder(propertyName, colorClassName)
                                .addModifiers(KModifier.OVERRIDE)
                                .initializer("Color($androidColorValue)")
                                .build()
                        )
                    }
                }
                .build()
        }

        // Build the complete file
        val fileSpec = FileSpec.builder(packageName, "GeneratedColors")
            .addFileComment("Auto-generated file - do not modify")
            .addType(themeColorObjectInterface)
            .apply {
                colorObjects.forEach { addType(it) }
            }
            .build()

        fileSpec.writeTo(Path(outputDir.path))
    }

    /**
     * Generates the theme file containing the AppTheme enum and AllThemes object.
     */
    fun generateThemeFile(outputDir: File, themes: List<ParsedTheme>, packageName: String) {
        val themeDataClassName = ClassName("de.kodierer.droidcon25.ui.theme", "ThemeData")
        val appColorsClassName = ClassName("de.kodierer.droidcon25.ui.theme", "AppColors")

        // Build AppTheme enum
        val appThemeEnum = TypeSpec.enumBuilder("AppTheme")
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("displayName", String::class)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("displayName", String::class)
                    .initializer("displayName")
                    .build()
            )
            .apply {
                themes.forEach { theme ->
                    addEnumConstant(
                        theme.enumName,
                        TypeSpec.anonymousClassBuilder()
                            .addSuperclassConstructorParameter("%S", theme.name)
                            .build()
                    )
                }
            }
            .build()

        // Build AllThemes object
        val allThemesObject = TypeSpec.objectBuilder("AllThemes")
            .addProperty(
                PropertySpec.builder("themes", ClassName("kotlin.collections", "List"))
                    .initializer(buildCodeBlock {
                        add("listOf(\n")
                        indent()
                        themes.forEachIndexed { index, theme ->
                            val objectName = theme.enumName.lowercase().replaceFirstChar { it.uppercase() }
                            add("%T(\n", themeDataClassName)
                            indent()
                            add("theme = %T.%L,\n", ClassName(packageName, "AppTheme"), theme.enumName)
                            add("name = %S,\n", theme.name)
                            add("colorObject = %L,\n", objectName)
                            add("appColors = %T(%L)\n", appColorsClassName, objectName)
                            unindent()
                            if (index < themes.size - 1) {
                                add("),\n")
                            } else {
                                add(")\n")
                            }
                        }
                        unindent()
                        add(")")
                    })
                    .build()
            )
            .addProperty(
                PropertySpec.builder("allAppThemes", ClassName("kotlin.collections", "List"))
                    .initializer("themes.map { it.theme }")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("themeMap", ClassName("kotlin.collections", "Map"))
                    .initializer("themes.associateBy { it.theme }")
                    .build()
            )
            .addFunction(
                FunSpec.builder("getThemeData")
                    .addParameter("theme", ClassName(packageName, "AppTheme"))
                    .returns(themeDataClassName)
                    .addStatement("return themeMap[theme] ?: error(%P)", "Theme data not found for \$theme")
                    .build()
            )
            .build()

        // Build the complete file
        val fileSpec = FileSpec.builder(packageName, "GeneratedThemes")
            .addFileComment("Auto-generated file - do not modify")
            .addType(appThemeEnum)
            .addType(allThemesObject)
            .build()

        fileSpec.writeTo(Path(outputDir.path))
    }

    /**
     * Converts a hex color string to Android Color format (0xAARRGGBB).
     */
    private fun convertToAndroidColor(hexColor: String): String {
        val cleanHex = hexColor.removePrefix("#")
        return when (cleanHex.length) {
            3 -> {
                // #RGB -> #AARRGGBB
                val r = cleanHex[0]
                val g = cleanHex[1]
                val b = cleanHex[2]
                "0xFF$r$r$g$g$b$b"
            }
            6 -> {
                // #RRGGBB -> #AARRGGBB
                "0xFF$cleanHex"
            }
            8 -> {
                // #AARRGGBB -> 0xAARRGGBB
                "0x$cleanHex"
            }
            else -> throw IllegalArgumentException("Invalid hex color format: $hexColor")
        }
    }
}