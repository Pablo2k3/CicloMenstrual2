// app/build.gradle.kts

plugins {
    id("com.android.application")
}

android {
    compileSdk = 34 // Ajusta según tus necesidades

    namespace = "com.example.ciclomenstrual" // Agrega esta línea

    defaultConfig {
        applicationId = "com.example.ciclomenstrual" // Este puede ser igual al namespace
        minSdk = 21 // Ajusta según tus necesidades
        targetSdk = 34 // Ajusta según tus necesidades
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.applandeo:material-calendar-view:1.9.2")
    implementation(libs.navigation.fragment)
    implementation(libs.monitor)
    implementation(libs.ext.junit)
    testImplementation(libs.junit.junit) // Dependencia del calendario
    // Otras dependencias
}
