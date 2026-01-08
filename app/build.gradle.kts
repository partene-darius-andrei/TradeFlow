import java.util.Properties

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
        versionName = "1.5.2"

        // Inject Coinbase credentials at build time
        // Priority: Environment variables (CI) > local.properties (local dev)
        val localProperties = File(rootProject.projectDir, "local.properties")
        val props = Properties()
        if (localProperties.exists()) {
            props.load(localProperties.inputStream())
        }

        val coinbaseApiKey = System.getenv("COINBASE_API_KEY")
            ?: props.getProperty("coinbase.api.key", "")
        val coinbaseApiSecret = System.getenv("COINBASE_API_SECRET")
            ?: props.getProperty("coinbase.api.secret", "")

        // Escape the secret for Java string literal (preserve \n as \\n)
        val escapedSecret = coinbaseApiSecret
            .replace("\\", "\\\\")  // Escape backslashes first
            .replace("\"", "\\\"")  // Escape quotes
            .replace("\n", "\\n")   // Convert newlines to \n escape sequence

        buildConfigField("String", "COINBASE_API_KEY", "\"$coinbaseApiKey\"")
        buildConfigField("String", "COINBASE_API_SECRET", "\"$escapedSecret\"")
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
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation (app-level NavHost)
    implementation(libs.androidx.navigation.compose)

    // Hilt (app-level DI wiring)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Timber (app-level logging)
    implementation(libs.timber)

    // Firebase
    implementation(platform(libs.firebase.bom))
}

apply(plugin = "com.google.firebase.appdistribution")
configure<com.google.firebase.appdistribution.gradle.AppDistributionExtension> {
    releaseNotesFile = "release-notes.txt"
    testers = "partene.darius@gmail.com"
    serviceCredentialsFile = "app/tradeflow.json"
}
