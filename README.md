# Droidcon 25 Demo App

This is the example repository for a Droidcon talk demonstrating themeable colors and multiple color palettes in Android applications.

## Features

- Custom theming system with multiple color palettes
- Royal Purple and Ocean Fire themes
- Theme switching with dropdown selector
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
- Custom theming system with Royal Purple and Ocean Fire themes
- Theme switching UI with dropdown selector
- Color swatch grid displaying theme variants
- WCAG accessibility compliance tests (A level - 3:1 contrast ratio)

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

### 🏷️ `codegen-fixed` - Fixed Code Generation Compilation
- Proper source set configuration for generated code
- Reliable build process with correct task dependencies

**Key Fixes:**
- Configured `kotlin.srcDir` for generated sources
- Moved source set configuration into main android block
- Added proper Kotlin compilation task dependencies
- Used `layout.buildDirectory.get()` for correct path resolution

**Generated Content:**
```kotlin
object GeneratedStrings {
    const val APP_TITLE = "Hello Berlin!!!"
}
```

### Current Version - Advanced Code Generation Framework
- Extracted code generation logic to `buildSrc`
- Proper task dependencies and incremental builds
- Reusable code generation framework for future expansion

**Key Improvements:**
- Code generation logic moved to `buildSrc/src/main/kotlin/CodegenTasks.kt`
- Input/output tracking for efficient incremental builds
- Foundation ready for generating color themes and other sources

## Code Generation Architecture

The build system uses a custom Gradle task to generate Kotlin source files at build time:

1. **buildSrc Module**: Contains reusable code generation functions
2. **Task Dependencies**: Proper ordering ensures buildSrc compiles first
3. **Incremental Builds**: Only regenerates when buildSrc source changes
4. **Future-Ready**: Designed to expand for color theme generation

```kotlin
// Usage in app/build.gradle.kts
val generateSources = createCodeGenerationTask()
```