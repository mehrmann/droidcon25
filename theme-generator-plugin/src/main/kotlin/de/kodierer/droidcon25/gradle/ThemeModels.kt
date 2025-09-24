package de.kodierer.droidcon25.gradle

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Data models for theme JSON parsing and processing.
 * These models represent the structure of JSON theme files and parsed theme data.
 */

@Serializable
data class TokenSet(
    val color: Map<String, ColorToken>? = null,
    val dimension: Map<String, DimensionToken>? = null,
    val typography: Map<String, TypographyToken>? = null,
    @SerialName("\$themes") val themes: Map<String, ThemeMetadata>? = null
)

@Serializable
data class ThemeMetadata(
    val name: String,
    val enumName: String? = null
)

@Serializable
data class ColorToken(
    val value: String = "",
    val type: String = "color",
    val description: String? = null,
    @SerialName("\$value") val dollarValue: String? = null,
    @SerialName("\$type") val dollarType: String? = null,
    @SerialName("\$description") val dollarDescription: String? = null,
    @SerialName("\$extensions") val extensions: TokenExtensions? = null
) {
    fun getActualValue(): String = dollarValue ?: value
    fun getActualType(): String = dollarType ?: type
    fun getActualDescription(): String? = dollarDescription ?: description
    fun getModifier(): ColorModifier? = extensions?.studioTokens?.modify
}

@Serializable
data class DimensionToken(
    val value: String,
    val type: String,
    val description: String? = null,
    @SerialName("\$value") val dollarValue: String? = null,
    @SerialName("\$type") val dollarType: String? = null,
    @SerialName("\$description") val dollarDescription: String? = null
)

@Serializable
data class TypographyToken(
    val value: TypographyValue,
    val type: String,
    val description: String? = null,
    @SerialName("\$value") val dollarValue: TypographyValue? = null,
    @SerialName("\$type") val dollarType: String? = null,
    @SerialName("\$description") val dollarDescription: String? = null
)

@Serializable
data class TypographyValue(
    val fontFamily: String? = null,
    val fontWeight: String? = null,
    val lineHeight: String? = null,
    val fontSize: String? = null,
    val letterSpacing: String? = null,
    val paragraphSpacing: String? = null,
    val textDecoration: String? = null,
    val textCase: String? = null
)


@Serializable
data class TokenExtensions(
    @SerialName("studio.tokens") val studioTokens: StudioTokens?
)

@Serializable
data class StudioTokens(
    val modify: ColorModifier?
)

@Serializable
data class ColorModifier(
    val type: String,
    val value: Double,
    val color: String? = null,
    val space: String? = null
)

/**
 * Parsed theme data ready for code generation.
 * This represents a fully processed theme with resolved colors and metadata.
 */
data class ParsedTheme(
    val name: String,
    val enumName: String,
    val colors: Map<String, String>
)