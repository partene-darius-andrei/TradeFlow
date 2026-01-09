plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.dpart.tradeflow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dpart.tradeflow"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.11.0"

        // Inject Coinbase credentials at build time
        // Priority: Environment variables (CI) > local.properties (local dev)
        val coinbaseApiKey = System.getenv("COINBASE_API_KEY") 
            ?: project.findProperty("coinbase.api.key") as String? ?: ""
        val coinbaseApiSecret = System.getenv("COINBASE_API_SECRET") 
            ?: project.findProperty("coinbase.api.secret") as String? ?: ""
            
        // Escape the secret for Java string literal (preserve \n as \\n)
        val escapedSecret = coinbaseApiSecret
            .replace("\\", "\\\\")  // Escape backslashes first
            .replace("\"", "\\\"")  // Escape quotes
            .replace("\n", "\\n")   // Convert newlines to \n escape sequence

        buildConfigField("String", "COINBASE_API_KEY", "\"$coinbaseApiKey\"")
        buildConfigField("String", "COINBASE_API_SECRET", "\"$escapedSecret\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Network
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    
    // Security & JWT
    implementation(libs.androidx.security.crypto)
    implementation(libs.nimbus.jose.jwt)
    implementation(libs.bcprov.jdk18on)
    implementation(libs.bcpkix.jdk18on)
    
    // Technical Analysis
    implementation(libs.ta4j.core)
    
    // Utilities
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.work.runtime.ktx)
    
    // Charts (future use)
    implementation(libs.vico.compose.m3)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    
    // Debug tools
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    
    // Module dependencies
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":exchange:coinbase"))
}

