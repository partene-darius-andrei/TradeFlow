plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
}

android {
    namespace = "com.dpart.tradeflow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dpart.tradeflow"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Module dependencies
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":exchange:coinbase"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:trading"))
    implementation(project(":feature:settings"))

    // App-level dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose (minimal for app entry point)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation (app-level NavHost)
    implementation(libs.androidx.navigation.compose)

    // Hilt (app-level DI wiring)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Timber (app-level logging)
    implementation(libs.timber)

    // Firebase (app-level analytics/crashlytics)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}

apply(plugin = "com.google.firebase.appdistribution")
configure<com.google.firebase.appdistribution.gradle.AppDistributionExtension> {
    releaseNotesFile = "release-notes.txt"
    testers = "partene.darius@gmail.com"
    serviceCredentialsFile = "app/tradeflow.json"
}
