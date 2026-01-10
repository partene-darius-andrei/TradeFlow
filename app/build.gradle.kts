import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dpart.tradeflow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dpart.tradeflow"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.10.1"

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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Module dependencies (business logic only)
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":exchange:coinbase"))

    // App-level dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose (minimal for "Hello" screen)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt (DI for business logic modules)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
