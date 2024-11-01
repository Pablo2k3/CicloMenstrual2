// build.gradle.kts a nivel de proyecto

plugins {
    alias(libs.plugins.android.application) apply false
}

allprojects {
    repositories {
        google() // Asegúrate de que esta línea esté presente solo aquí
        mavenCentral()
    }
}