# Droidcon 25 Demo App

This is the example repository for a Droidcon talk demonstrating themeable colors and multiple color palettes in Android applications.

## Features

- Custom theming system with multiple color palettes
- Color swatch grid displaying all theme variants
- WCAG accessibility compliance testing
- Build-time code generation system

## Building

```bash
./gradlew build
```

## Running Tests

```bash
./gradlew test
```

## Development Journey

This repository demonstrates step-by-step improvements in implementing themeable colors and build-time code generation. Each major milestone is tagged for easy reference:

### 🏷️ `baseline` - Initial Implementation
- Basic Android app with Jetpack Compose
- Custom theming system with (hard-coded) Royal Purple and Ocean Fire themes
- Theme switching UI with dropdown selector
- Color swatch grid displaying theme variants

**Key Features:**
- `AppColors` data class for unified color access
- `CompositionLocalProvider` for theme management
- Comprehensive unit tests for color contrast validation

### 🏷️ `codegen-v1` - Ultra Basic Code Generation
- Simple Gradle build script for code generation
- Generated `GeneratedStrings.kt` with app title
- Build-time string generation integrated into compilation

**Key Features:**
- Custom Gradle task `generateSources`
- Generated files in `app/build/generated/codegen/src/`
- Automatic execution before Kotlin compilation

**What's Generated:**
```kotlin
object GeneratedStrings {
    const val APP_TITLE = "Hello World"
}
```

### 🏷️ `codegen-v2` - Cleanup and File Management
- Move code generator into buildSrc

### 🏷️ `codegen-v3` - Externalized Configuration System
- Configuration-driven code generation with properties file

**Key Features:**
- `config/constants.properties` file for managing string constants
- Proper build input tracking for config file changes

### 🏷️ `codegen-v4` - JSON-Based Theme Configuration
- JSON-based theme system
- Dynamic theme enumeration with zero-code theme additions

**Key Features:**
- JSON theme files in `themes/` directory define colors and metadata
- Dynamic theme discovery - add/remove themes without code changes
- Core classes (`AppColors`, `ThemeData`, `LocalAppColors`) moved to main source
- Dynamic test system that automatically covers all themes

**What's Generated:**
- Dynamic theme enumeration from JSON files
- Type-safe theme accessors

### 🏷️ `codegen-v5` - Plugin Architecture Migration
- Migrated from `buildSrc` to dedicated Gradle plugin
- Composite build integration with `theme-generator-plugin`

**Key Features:**
- `theme-generator-plugin` as included build with `includeBuild()`
- Removed `buildSrc` directory in favor of proper plugin architecture
- Refined theme JSON files with improved color values
- Clean separation of plugin logic from main build

