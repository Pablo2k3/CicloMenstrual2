// settings.gradle.kts

pluginManagement {
    repositories {
        google() // Primero
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google() // Primero
        mavenCentral() // Luego
    }
}

rootProject.name = "CicloMenstrual"
include(":app") // Incluye otros módulos si los tienes
