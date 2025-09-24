plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.0.21"
    `maven-publish`
}

group = "de.kodierer.droidcon25"
version = "1.0.0"

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.android.tools.build:gradle:8.7.2")
    implementation("com.squareup:kotlinpoet:1.18.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.8")
}

gradlePlugin {
    plugins {
        create("themeGenerator") {
            id = "de.kodierer.droidcon25.theme-generator"
            implementationClass = "de.kodierer.droidcon25.gradle.ThemeGeneratorPlugin"
            displayName = "Theme Generator Plugin"
            description = "Generates theme classes from JSON theme definitions with automatic source set configuration"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "local"
            url = uri("../local-repo")
        }
    }
}